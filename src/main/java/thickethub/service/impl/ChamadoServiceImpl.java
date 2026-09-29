package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.Papel;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.enums.TipoHistorico;
import thickethub.domain.model.*;
import thickethub.dto.request.*;
import thickethub.dto.response.*;
import thickethub.exception.BusinessException;
import thickethub.exception.ConflictException;
import thickethub.exception.ForbiddenException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.*;
import thickethub.repository.specification.ChamadoSpecification;
import thickethub.security.SecurityUtils;
import thickethub.service.ChamadoService;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoServiceImpl implements ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ServicoRepository servicoRepository;
    private final SlaConfigRepository slaConfigRepository;
    private final HistoricoRepository historicoRepository;
    private final ComentarioRepository comentarioRepository;
    private final ChamadoVinculoRepository chamadoVinculoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter PROTOCOLO_DATA = DateTimeFormatter.ofPattern("MMdd");

    // ============================================================
    // criarChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse criarChamado(Usuario solicitante, NovoChamadoRequest request) {
        Servico servico = servicoRepository.findById(request.servicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + request.servicoId()));

        PrioridadeChamado prioridade = servico.getPrioridadePadrao();

        SlaConfig sla = servico.getSlaConfig() != null
                ? servico.getSlaConfig()
                : slaConfigRepository.findByPrioridade(prioridade)
                .orElse(null);

        int slaAtendimento = servico.getSlaAtendimentoHoras() != null ? servico.getSlaAtendimentoHoras()
                : (sla != null ? sla.getSlaAtendimentoHoras() : 4);
        int slaResolucao = servico.getSlaResolucaoHoras() != null ? servico.getSlaResolucaoHoras()
                : (sla != null ? sla.getSlaResolucaoHoras() : 24);

        LocalDateTime agora = LocalDateTime.now();

        Chamado chamado = Chamado.builder()
                .protocolo(gerarProtocolo())
                .titulo(request.titulo())
                .descricao(request.descricao())
                .status(StatusChamado.ABERTO)
                .prioridade(prioridade)
                .solicitante(solicitante)
                .setor(servico.getSetor())
                .filaSetor(servico.getSetor())
                .servico(servico)
                .dataAbertura(agora)
                .slaRodandoDesde(agora)
                .slaConsumidoMs(0L)
                .slaAtendimentoHoras(slaAtendimento)
                .slaResolucaoHoras(slaResolucao)
                .prazoLimite(agora.plusHours(slaResolucao))
                .versao(0L)
                .build();

        chamadoRepository.save(chamado);

        registrarHistorico(chamado, solicitante, TipoHistorico.ABERTURA, "Chamado criado pelo solicitante");
        auditar(solicitante, "chamado.criar", "protocolo=" + chamado.getProtocolo());
        notificarSetor(chamado, "Novo chamado aberto: " + chamado.getProtocolo() + " - " + chamado.getTitulo());

        log.info("Chamado criado: protocolo={}, solicitanteId={}", chamado.getProtocolo(), solicitante.getId());
        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // buscar() & buscarPorProtocolo()
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        Chamado chamado = buscarOuFalhar(id);
        return ChamadoDetalheResponse.de(chamado);
    }

    @Override
    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscarPorProtocolo(String protocolo) {
        Chamado chamado = chamadoRepository.findByProtocolo(protocolo)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado com protocolo: " + protocolo));
        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // listar() com Specification e paginação
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public PaginacaoResponse<ChamadoResumoResponse> listar(
            Usuario usuarioLogado,
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long servicoId,
            String setor,
            String fila,
            String busca,
            Pageable pageable) {

        Specification<Chamado> spec = Specification
                .where(ChamadoSpecification.porVisibilidade(usuarioLogado))
                .and(ChamadoSpecification.porStatus(status))
                .and(ChamadoSpecification.porPrioridade(prioridade))
                .and(ChamadoSpecification.porServico(servicoId))
                .and(ChamadoSpecification.porSetor(setor))
                .and(ChamadoSpecification.porFila(fila, usuarioLogado))
                .and(ChamadoSpecification.buscaTextual(busca));

        Page<Chamado> page = chamadoRepository.findAll(spec, pageable);
        Page<ChamadoResumoResponse> dtoPage = page.map(ChamadoResumoResponse::de);

        return PaginacaoResponse.de(dtoPage);
    }

    // ============================================================
    // atualizar()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse atualizar(Long id, AtualizarChamadoRequest request) {
        Chamado chamado = buscarOuFalhar(id);
        Usuario logado = SecurityUtils.getUsuarioLogado();
        validarTecnicoDoSetor(logado, chamado);

        if (request.status() != null) {
            chamado.setStatus(request.status());
        }
        if (request.prioridade() != null) {
            chamado.setPrioridade(request.prioridade());
        }
        if (request.oQueFoiFeito() != null && !request.oQueFoiFeito().isBlank()) {
            chamado.setOQueFoiFeito(request.oQueFoiFeito());
        }

        chamadoRepository.save(chamado);
        registrarHistorico(chamado, logado, TipoHistorico.STATUS, "Chamado atualizado: status=" + request.status());
        auditar(logado, "chamado.atualizar", "protocolo=" + chamado.getProtocolo());

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // assumirChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse assumirChamado(Usuario tecnico, Long chamadoId) {
        log.info("[CHAMADO ASSUMIR] Técnico '{}' (id={}) tentando assumir chamado id={}",
                tecnico.getNome(), tecnico.getId(), chamadoId);

        Chamado chamado = buscarOuFalhar(chamadoId);
        validarTecnicoDoSetor(tecnico, chamado);

        if (chamado.getTecnico() != null) {
            log.warn("[CHAMADO ASSUMIR] Chamado '{}' já está atribuído ao técnico '{}'",
                    chamado.getProtocolo(), chamado.getTecnico().getNome());
            throw new ConflictException("Chamado já atribuído a outro técnico.");
        }
        if (chamado.getStatus() != StatusChamado.ABERTO && chamado.getStatus() != StatusChamado.AGUARDANDO) {
            log.warn("[CHAMADO ASSUMIR] Status inválido — chamado '{}' está como {}", chamado.getProtocolo(), chamado.getStatus());
            throw new BusinessException("Só é possível assumir chamados com status ABERTO ou AGUARDANDO.");
        }

        chamado.setTecnico(tecnico);
        if (chamado.getPrimeiraRespostaEm() == null) {
            chamado.setPrimeiraRespostaEm(LocalDateTime.now());
        }
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);

        congelarSla(chamado);
        chamado.setSlaRodandoDesde(LocalDateTime.now());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, tecnico, TipoHistorico.ASSUNCAO, "Chamado assumido pelo técnico " + tecnico.getNome());
        auditar(tecnico, "chamado.assumir", "protocolo=" + chamado.getProtocolo());
        notificarUsuario(chamado.getSolicitante(), chamado, "Seu chamado " + chamado.getProtocolo() + " foi assumido por " + tecnico.getNome());

        log.info("[CHAMADO ASSUMIR] SUCESSO — chamado '{}' assumido pelo técnico '{}' (id={})",
                chamado.getProtocolo(), tecnico.getNome(), tecnico.getId());
        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // pausarChamado() & retomarChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse pausarChamado(Usuario user, Long chamadoId) {
        Chamado chamado = buscarOuFalhar(chamadoId);
        validarTecnicoDoSetor(user, chamado);

        if (chamado.getStatus() == StatusChamado.PAUSADO) {
            throw new BusinessException("Chamado já está pausado.");
        }

        congelarSla(chamado);
        chamado.setStatus(StatusChamado.PAUSADO);
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, user, TipoHistorico.PAUSA, "Chamado pausado — SLA congelado");
        auditar(user, "chamado.pausar", "protocolo=" + chamado.getProtocolo());

        return ChamadoDetalheResponse.de(chamado);
    }

    @Override
    @Transactional
    public ChamadoDetalheResponse retomarChamado(Usuario user, Long chamadoId) {
        Chamado chamado = buscarOuFalhar(chamadoId);
        validarTecnicoDoSetor(user, chamado);

        if (chamado.getStatus() != StatusChamado.PAUSADO) {
            throw new BusinessException("Só é possível retomar chamados pausados.");
        }

        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setSlaRodandoDesde(LocalDateTime.now());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, user, TipoHistorico.RETOMADA, "Chamado retomado — SLA reativado");
        auditar(user, "chamado.retomar", "protocolo=" + chamado.getProtocolo());

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // resolverChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse resolverChamado(Usuario user, Long chamadoId, ResolverChamadoRequest request) {
        log.info("[CHAMADO RESOLVER] Técnico '{}' resolvendo chamado id={}", user.getNome(), chamadoId);

        Chamado chamado = buscarOuFalhar(chamadoId);
        validarTecnicoDoSetor(user, chamado);

        if (request == null || request.oQueFoiFeito() == null || request.oQueFoiFeito().isBlank()) {
            log.warn("[CHAMADO RESOLVER] Tentativa de resolver chamado '{}' sem informar o que foi feito", chamado.getProtocolo());
            throw new BusinessException("É obrigatório registrar o que foi feito para resolver o chamado.");
        }

        congelarSla(chamado);
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setResolvidoEm(LocalDateTime.now());
        chamado.setOQueFoiFeito(request.oQueFoiFeito());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, user, TipoHistorico.RESOLUCAO, "Chamado resolvido: " + request.oQueFoiFeito());
        auditar(user, "chamado.resolver", "protocolo=" + chamado.getProtocolo());
        notificarUsuario(chamado.getSolicitante(), chamado, "Seu chamado " + chamado.getProtocolo() + " foi resolvido. Avalie o atendimento.");

        log.info("[CHAMADO RESOLVER] SUCESSO — chamado '{}' marcado como RESOLVIDO por '{}'",
                chamado.getProtocolo(), user.getNome());
        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // devolverChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse devolverChamado(Usuario user, Long chamadoId, DevolverChamadoRequest request) {
        Chamado chamado = buscarOuFalhar(chamadoId);
        validarTecnicoDoSetor(user, chamado);

        if (chamado.getStatus() == StatusChamado.RESOLVIDO || chamado.getStatus() == StatusChamado.FECHADO) {
            throw new BusinessException("Não é possível devolver um chamado resolvido ou fechado.");
        }

        chamado.setTecnico(null);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setFilaSetor(chamado.getSetor());
        chamado.setSlaRodandoDesde(LocalDateTime.now());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, user, TipoHistorico.DEVOLUCAO, "Chamado devolvido para a fila: " + request.motivo());
        auditar(user, "chamado.devolver", "protocolo=" + chamado.getProtocolo() + "; motivo=" + request.motivo());
        notificarSetor(chamado, "Chamado " + chamado.getProtocolo() + " devolvido para a fila do setor.");

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // encaminhar()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse encaminhar(Long id, EncaminharChamadoRequest request) {
        Chamado chamado = buscarOuFalhar(id);
        Usuario logado = SecurityUtils.getUsuarioLogado();

        if (request.filaSetor() != null && !request.filaSetor().isBlank()) {
            chamado.setSetor(request.filaSetor());
            chamado.setFilaSetor(request.filaSetor());
        }

        if (request.tecnicoId() != null) {
            Usuario novoTecnico = usuarioRepository.findById(request.tecnicoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado com ID: " + request.tecnicoId()));
            chamado.setTecnico(novoTecnico);
            chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
            notificarUsuario(novoTecnico, chamado, "Chamado " + chamado.getProtocolo() + " foi encaminhado para você.");
        } else {
            chamado.setTecnico(null);
            chamado.setStatus(StatusChamado.ABERTO);
        }

        chamadoRepository.save(chamado);
        registrarHistorico(chamado, logado, TipoHistorico.ENCAMINHAMENTO, "Chamado encaminhado: " + request.motivo());
        auditar(logado, "chamado.encaminhar", "protocolo=" + chamado.getProtocolo() + "; motivo=" + request.motivo());
        notificarSetor(chamado, "Chamado " + chamado.getProtocolo() + " encaminhado para o setor " + chamado.getSetor());

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // reabrirChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse reabrirChamado(Usuario user, Long chamadoId, String justificativa) {
        boolean isAdm = user.getPapel() == Papel.ADMIN || user.getPapel() == Papel.SUPERUSUARIO || user.getPapel() == Papel.SUPER;
        if (!isAdm) {
            throw new ForbiddenException("Apenas administradores podem reabrir chamados.");
        }

        Chamado chamado = buscarOuFalhar(chamadoId);

        if (chamado.getStatus() != StatusChamado.RESOLVIDO && chamado.getStatus() != StatusChamado.FECHADO) {
            throw new BusinessException("Só é possível reabrir chamados resolvidos ou fechados.");
        }

        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setResolvidoEm(null);
        chamado.setMotivoReabertura(justificativa);
        chamado.setSlaRodandoDesde(LocalDateTime.now());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, user, TipoHistorico.REABERTURA, "Chamado reaberto: " + justificativa);
        auditar(user, "chamado.reabrir", "protocolo=" + chamado.getProtocolo());
        if (chamado.getTecnico() != null) {
            notificarUsuario(chamado.getTecnico(), chamado, "O chamado " + chamado.getProtocolo() + " foi reaberto.");
        }

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // avaliarChamado()
    // ============================================================
    @Override
    @Transactional
    public ChamadoDetalheResponse avaliarChamado(Usuario solicitante, Long chamadoId, AvaliarChamadoRequest request) {
        Chamado chamado = buscarOuFalhar(chamadoId);

        if (!chamado.getSolicitante().getId().equals(solicitante.getId())) {
            throw new ForbiddenException("Apenas o solicitante pode avaliar o chamado.");
        }
        if (chamado.getStatus() != StatusChamado.RESOLVIDO && chamado.getStatus() != StatusChamado.FECHADO) {
            throw new BusinessException("Apenas chamados resolvidos podem ser avaliados.");
        }

        chamado.setAvaliacao(request.nota());
        chamado.setStatus(StatusChamado.FECHADO);
        chamado.setDataFechamento(LocalDateTime.now());
        chamadoRepository.save(chamado);

        registrarHistorico(chamado, solicitante, TipoHistorico.STATUS, "Atendimento avaliado com nota " + request.nota());
        auditar(solicitante, "chamado.avaliar", "protocolo=" + chamado.getProtocolo() + "; nota=" + request.nota());

        return ChamadoDetalheResponse.de(chamado);
    }

    // ============================================================
    // Comentários
    // ============================================================
    @Override
    @Transactional
    public ComentarioResponse adicionarComentario(Usuario autor, Long chamadoId, NovoComentarioRequest request) {
        Chamado chamado = buscarOuFalhar(chamadoId);

        boolean flagInterno = Boolean.TRUE.equals(request.flagInterno());
        if (flagInterno && autor.getPapel() == Papel.USUARIO) {
            throw new ForbiddenException("Usuários solicitantes não podem adicionar notas internas.");
        }

        Comentario comentario = Comentario.builder()
                .chamado(chamado)
                .autor(autor)
                .mensagem(request.mensagem())
                .flagInterno(flagInterno)
                .dataCriacao(LocalDateTime.now())
                .build();

        comentarioRepository.save(comentario);
        registrarHistorico(chamado, autor, TipoHistorico.COMENTARIO,
                (flagInterno ? "[Nota Interna] " : "") + "Comentário adicionado");

        // Notificar solicitante ou técnico dependendo de quem comentou
        if (!flagInterno) {
            if (autor.getId().equals(chamado.getSolicitante().getId()) && chamado.getTecnico() != null) {
                notificarUsuario(chamado.getTecnico(), chamado, "Novo comentário no chamado " + chamado.getProtocolo());
            } else if (!autor.getId().equals(chamado.getSolicitante().getId())) {
                notificarUsuario(chamado.getSolicitante(), chamado, "Nova resposta no chamado " + chamado.getProtocolo());
            }
        }

        return ComentarioResponse.de(comentario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComentarioResponse> listarComentarios(Usuario usuario, Long chamadoId) {
        Chamado chamado = buscarOuFalhar(chamadoId);
        boolean isTecnicoOuAdm = usuario.getPapel() != Papel.USUARIO;

        List<Comentario> lista = isTecnicoOuAdm
                ? comentarioRepository.findByChamado_IdOrderByDataCriacaoAsc(chamadoId)
                : comentarioRepository.findByChamado_IdAndFlagInternoFalseOrderByDataCriacaoAsc(chamadoId);

        return lista.stream().map(ComentarioResponse::de).toList();
    }

    // ============================================================
    // Vincular Chamados
    // ============================================================
    @Override
    @Transactional
    public void vincularChamado(Usuario usuario, Long chamadoOrigemId, VincularChamadoRequest request) {
        Chamado origem = buscarOuFalhar(chamadoOrigemId);
        Chamado destino = buscarOuFalhar(request.chamadoDestinoId());

        if (origem.getId().equals(destino.getId())) {
            throw new BusinessException("Não é possível vincular um chamado a ele mesmo.");
        }

        if (chamadoVinculoRepository.existsByChamadoOrigem_IdAndChamadoDestino_Id(origem.getId(), destino.getId())) {
            throw new ConflictException("Estes chamados já estão vinculados.");
        }

        ChamadoVinculo vinculo = ChamadoVinculo.builder()
                .chamadoOrigem(origem)
                .chamadoDestino(destino)
                .tipo(request.tipo())
                .build();

        chamadoVinculoRepository.save(vinculo);
        registrarHistorico(origem, usuario, TipoHistorico.VINCULO, "Vinculado ao chamado " + destino.getProtocolo() + " como " + request.tipo());
        auditar(usuario, "chamado.vincular", "origem=" + origem.getProtocolo() + "; destino=" + destino.getProtocolo());
    }

    // ============================================================
    // Métricas
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public MetricasResponse obterMetricas(Usuario usuarioLogado) {
        long totalChamados = chamadoRepository.count();
        long abertos = chamadoRepository.countByStatus(StatusChamado.ABERTO);
        long emAndamento = chamadoRepository.countByStatus(StatusChamado.EM_ATENDIMENTO) + chamadoRepository.countByStatus(StatusChamado.EM_ANDAMENTO);
        long resolvidos = chamadoRepository.countByStatus(StatusChamado.RESOLVIDO);
        long fechados = chamadoRepository.countByStatus(StatusChamado.FECHADO) + chamadoRepository.countByStatus(StatusChamado.CONCLUIDO);
        long cancelados = chamadoRepository.countByStatus(StatusChamado.CANCELADO);
        long pausados = chamadoRepository.countByStatus(StatusChamado.PAUSADO);

        long totalUsuarios = usuarioRepository.count();
        long totalTecnicos = usuarioRepository.countByPapel(Papel.TECNICO);
        long totalAdmins = usuarioRepository.countByPapel(Papel.ADMIN) + usuarioRepository.countByPapel(Papel.SUPERUSUARIO);

        double percSla = totalChamados > 0 ? ((double) (resolvidos + fechados) / totalChamados) * 100.0 : 100.0;

        return new MetricasResponse(
                totalChamados, abertos, emAndamento, resolvidos, fechados,
                cancelados, pausados, totalUsuarios, totalTecnicos, totalAdmins, percSla
        );
    }

    // ============================================================
    // Auxiliares internos
    // ============================================================

    private Chamado buscarOuFalhar(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado com ID: " + id));
    }

    private void validarTecnicoDoSetor(Usuario user, Chamado chamado) {
        if (user.getPapel() == Papel.ADMIN || user.getPapel() == Papel.SUPERUSUARIO || user.getPapel() == Papel.SUPER) {
            return;
        }
        if (user.getPapel() != Papel.TECNICO || user.getSetoresLiberados() == null || !user.getSetoresLiberados().contains(chamado.getSetor())) {
            log.warn("[CHAMADO PERMISSÃO] NEGADO — '{}' (papel={}) sem acesso ao setor '{}' do chamado '{}'",
                    user.getEmail(), user.getPapel(), chamado.getSetor(), chamado.getProtocolo());
            throw new ForbiddenException("Você não tem permissão para atuar no setor deste chamado.");
        }
    }

    private String gerarProtocolo() {
        String ano = String.valueOf(LocalDate.now().getYear());
        String data = LocalDate.now().format(PROTOCOLO_DATA);
        String sufixo;
        do {
            sufixo = String.format("%04d", new SecureRandom().nextInt(10_000));
        } while (chamadoRepository.existsByProtocolo("CH-" + ano + "-" + data + "-" + sufixo));
        return "CH-" + ano + "-" + data + "-" + sufixo;
    }

    private void congelarSla(Chamado chamado) {
        if (chamado.getSlaRodandoDesde() != null) {
            long decorrido = Duration.between(chamado.getSlaRodandoDesde(), LocalDateTime.now()).toMillis();
            chamado.setSlaConsumidoMs(
                    (chamado.getSlaConsumidoMs() != null ? chamado.getSlaConsumidoMs() : 0L) + decorrido);
            chamado.setSlaRodandoDesde(null);
        }
    }

    private void registrarHistorico(Chamado chamado, Usuario autor, TipoHistorico tipo, String descricao) {
        HistoricoChamado h = HistoricoChamado.builder()
                .chamado(chamado)
                .autor(autor)
                .autorNome(autor != null ? autor.getNome() : "Sistema")
                .tipo(tipo)
                .descricao(descricao)
                .dataEvento(LocalDateTime.now())
                .build();
        historicoRepository.save(h);
    }

    private void auditar(Usuario usuario, String acao, String detalhe) {
        try {
            Auditoria a = Auditoria.builder()
                    .acao(acao)
                    .detalhe(detalhe)
                    .usuario(usuario)
                    .criadoEm(LocalDateTime.now())
                    .build();
            auditoriaRepository.save(a);
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de chamado: {}", e.getMessage());
        }
    }

    private void notificarUsuario(Usuario destino, Chamado chamado, String mensagem) {
        if (destino == null) return;
        try {
            Notificacao n = Notificacao.builder()
                    .usuario(destino)
                    .chamado(chamado)
                    .tipo("CHAMADO")
                    .mensagem(mensagem)
                    .lida(false)
                    .criadoEm(LocalDateTime.now())
                    .build();
            notificacaoRepository.save(n);
            messagingTemplate.convertAndSendToUser(destino.getEmail(), "/queue/notificacoes", mensagem);
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação de chamado: {}", e.getMessage());
        }
    }

    private void notificarSetor(Chamado chamado, String mensagem) {
        try {
            messagingTemplate.convertAndSend("/topic/setor/" + chamado.getSetor(), mensagem);
        } catch (Exception e) {
            log.warn("Falha ao enviar broadcast de setor: {}", e.getMessage());
        }
    }
}