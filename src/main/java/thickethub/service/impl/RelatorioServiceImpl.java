package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.Papel;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.enums.TipoHistorico;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.HistoricoChamado;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.RelatoriosResponse;
import thickethub.repository.ChamadoRepository;
import thickethub.repository.HistoricoRepository;
import thickethub.repository.UsuarioRepository;
import thickethub.service.RelatorioService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    private final ChamadoRepository chamadoRepository;
    private final HistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public RelatoriosResponse gerarRelatorio(
            LocalDate de,
            LocalDate ate,
            String setor,
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long tecnicoId,
            Long servicoId
    ) {
        LocalDateTime dtInicio = (de != null) ? de.atStartOfDay() : LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime dtFim = (ate != null) ? ate.atTime(LocalTime.MAX) : LocalDateTime.now().plusDays(1);

        List<Chamado> todos = chamadoRepository.findAll();

        // Aplica os filtros na listagem
        List<Chamado> filtrados = todos.stream().filter(c -> {
            LocalDateTime dataAbertura = c.getDataAbertura() != null ? c.getDataAbertura() : c.getCriadoEm();
            if (dataAbertura != null && (dataAbertura.isBefore(dtInicio) || dataAbertura.isAfter(dtFim))) {
                return false;
            }
            if (setor != null && !setor.isBlank() && !setor.equalsIgnoreCase(c.getSetor())) {
                return false;
            }
            if (status != null && c.getStatus() != status) {
                return false;
            }
            if (prioridade != null && c.getPrioridade() != prioridade) {
                return false;
            }
            if (tecnicoId != null && (c.getTecnico() == null || !tecnicoId.equals(c.getTecnico().getId()))) {
                return false;
            }
            if (servicoId != null && (c.getServico() == null || !servicoId.equals(c.getServico().getId()))) {
                return false;
            }
            return true;
        }).toList();

        long totalChamados = filtrados.size();

        // 1. Por Setor (chave, total, resolvidos, atrasados)
        Map<String, List<Chamado>> porSetorMap = filtrados.stream()
                .collect(Collectors.groupingBy(c -> (c.getSetor() != null && !c.getSetor().isBlank()) ? c.getSetor() : "Geral"));

        List<RelatoriosResponse.ItemPorSetor> porSetor = porSetorMap.entrySet().stream()
                .map(entry -> {
                    String set = entry.getKey();
                    List<Chamado> lista = entry.getValue();
                    long tot = lista.size();
                    long res = lista.stream().filter(c -> c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO).count();
                    long atrasados = lista.stream().filter(this::estaAtrasado).count();
                    return new RelatoriosResponse.ItemPorSetor(set, tot, res, atrasados);
                })
                .sorted(Comparator.comparing(RelatoriosResponse.ItemPorSetor::total).reversed())
                .toList();

        // 2. Por Status
        Map<StatusChamado, Long> porStatusMap = filtrados.stream()
                .collect(Collectors.groupingBy(Chamado::getStatus, Collectors.counting()));

        List<RelatoriosResponse.ItemSimples> porStatus = Arrays.stream(StatusChamado.values())
                .map(st -> new RelatoriosResponse.ItemSimples(st.name().toLowerCase(), porStatusMap.getOrDefault(st, 0L)))
                .toList();

        // 3. Por Prioridade com cumprimento de SLA
        Map<PrioridadeChamado, List<Chamado>> porPrioMap = filtrados.stream()
                .collect(Collectors.groupingBy(Chamado::getPrioridade));

        List<RelatoriosResponse.ItemPorPrioridade> porPrioridade = Arrays.stream(PrioridadeChamado.values())
                .map(prio -> {
                    List<Chamado> lista = porPrioMap.getOrDefault(prio, Collections.emptyList());
                    long tot = lista.size();
                    long slaCumprido = lista.stream().filter(c -> {
                        boolean resolvido = (c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO);
                        return resolvido && !estaAtrasado(c);
                    }).count();
                    return new RelatoriosResponse.ItemPorPrioridade(prio.name().toLowerCase(), tot, slaCumprido, tot);
                }).toList();

        // 4. Por Serviço
        Map<String, Long> porServicoMap = filtrados.stream()
                .collect(Collectors.groupingBy(c -> c.getServico() != null ? c.getServico().getNome() : "Não especificado", Collectors.counting()));

        List<RelatoriosResponse.ItemSimples> porServico = porServicoMap.entrySet().stream()
                .map(e -> new RelatoriosResponse.ItemSimples(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(RelatoriosResponse.ItemSimples::total).reversed())
                .limit(10)
                .toList();

        // 5. Técnicos
        Map<Usuario, List<Chamado>> porTecnicoMap = filtrados.stream()
                .filter(c -> c.getTecnico() != null)
                .collect(Collectors.groupingBy(Chamado::getTecnico));

        List<RelatoriosResponse.ItemTecnico> tecnicos = porTecnicoMap.entrySet().stream()
                .map(entry -> {
                    Usuario tec = entry.getKey();
                    List<Chamado> lista = entry.getValue();
                    long atendidos = lista.stream().filter(c -> c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO).count();
                    long emAberto = lista.size() - atendidos;

                    // Média de horas de resolução
                    double mediaHoras = lista.stream()
                            .filter(c -> c.getResolvidoEm() != null && c.getDataAbertura() != null)
                            .mapToLong(c -> Duration.between(c.getDataAbertura(), c.getResolvidoEm()).toHours())
                            .average()
                            .orElse(0.0);

                    long slaCumprido = lista.stream().filter(c -> {
                        boolean resolvido = (c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO);
                        return resolvido && !estaAtrasado(c);
                    }).count();

                    return new RelatoriosResponse.ItemTecnico(
                            tec.getNome(),
                            atendidos,
                            emAberto,
                            Math.round(mediaHoras * 10.0) / 10.0,
                            slaCumprido,
                            lista.size()
                    );
                })
                .sorted(Comparator.comparing(RelatoriosResponse.ItemTecnico::atendidos).reversed())
                .toList();

        // 6. Período
        long resolvidosPeriodo = filtrados.stream().filter(c -> c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO).count();
        long canceladosPeriodo = filtrados.stream().filter(c -> c.getStatus() == StatusChamado.CANCELADO).count();
        long reabertosPeriodo = filtrados.stream().filter(c -> c.getMotivoReabertura() != null && !c.getMotivoReabertura().isBlank()).count();
        RelatoriosResponse.PeriodoResumo periodoResumo = new RelatoriosResponse.PeriodoResumo(resolvidosPeriodo, canceladosPeriodo, reabertosPeriodo);

        // 7. Tempos médios
        double medioAtendimento = filtrados.stream()
                .filter(c -> c.getPrimeiraRespostaEm() != null && c.getDataAbertura() != null)
                .mapToLong(c -> Duration.between(c.getDataAbertura(), c.getPrimeiraRespostaEm()).toHours())
                .average()
                .orElse(1.5);

        double medioResolucao = filtrados.stream()
                .filter(c -> c.getResolvidoEm() != null && c.getDataAbertura() != null)
                .mapToLong(c -> Duration.between(c.getDataAbertura(), c.getResolvidoEm()).toHours())
                .average()
                .orElse(4.2);

        RelatoriosResponse.TemposMedios temposMedios = new RelatoriosResponse.TemposMedios(
                Math.round(medioAtendimento * 10.0) / 10.0,
                Math.round(medioResolucao * 10.0) / 10.0
        );

        // 8. Usuários
        long totalUsuarios = usuarioRepository.count();
        long usuariosAtivos = usuarioRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getAtivo())).count();
        RelatoriosResponse.UsuariosResumo usuariosResumo = new RelatoriosResponse.UsuariosResumo(totalUsuarios, usuariosAtivos);

        // 9. Satisfação
        List<Chamado> comAvaliacao = filtrados.stream().filter(c -> c.getAvaliacao() != null && c.getAvaliacao() > 0).toList();
        double mediaSatisfacao = comAvaliacao.stream().mapToInt(Chamado::getAvaliacao).average().orElse(0.0);
        mediaSatisfacao = Math.round(mediaSatisfacao * 10.0) / 10.0;

        Map<Integer, Long> distribuicaoMap = comAvaliacao.stream()
                .collect(Collectors.groupingBy(Chamado::getAvaliacao, Collectors.counting()));

        List<RelatoriosResponse.ItemDistribuicaoNota> distribuicao = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            distribuicao.add(new RelatoriosResponse.ItemDistribuicaoNota(i, distribuicaoMap.getOrDefault(i, 0L)));
        }

        RelatoriosResponse.SatisfacaoResumo satisfacaoResumo = new RelatoriosResponse.SatisfacaoResumo(
                mediaSatisfacao,
                comAvaliacao.size(),
                distribuicao
        );

        // 10. Encaminhamentos
        List<HistoricoChamado> historicos = historicoRepository.findAll();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<RelatoriosResponse.EncaminhamentoResumo> encaminhamentos = historicos.stream()
                .filter(h -> h.getTipo() == TipoHistorico.ENCAMINHAMENTO)
                .sorted(Comparator.comparing(HistoricoChamado::getDataEvento).reversed())
                .limit(15)
                .map(h -> new RelatoriosResponse.EncaminhamentoResumo(
                        h.getChamado() != null ? h.getChamado().getProtocolo() : "—",
                        h.getDescricao() != null ? h.getDescricao() : "Encaminhamento registrado",
                        h.getAutorNome() != null ? h.getAutorNome() : "Sistema",
                        h.getDataEvento() != null ? h.getDataEvento().format(fmt) : ""
                ))
                .toList();

        // 11. Evolução Mensal (para gráfico temporal de tendência)
        DateTimeFormatter mesFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        Map<String, List<Chamado>> porMes = filtrados.stream()
                .filter(c -> c.getDataAbertura() != null)
                .collect(Collectors.groupingBy(c -> c.getDataAbertura().format(mesFmt)));

        List<RelatoriosResponse.EvolucaoTemporal> evolucaoMensal = porMes.entrySet().stream()
                .map(entry -> {
                    String mesAno = entry.getKey();
                    long abertos = entry.getValue().size();
                    long res = entry.getValue().stream()
                            .filter(c -> c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO)
                            .count();
                    return new RelatoriosResponse.EvolucaoTemporal(mesAno, abertos, res);
                })
                .sorted(Comparator.comparing(RelatoriosResponse.EvolucaoTemporal::periodo))
                .toList();

        // 12. Mapa de Calor (Dia da semana 1=Segunda .. 7=Domingo x Hora 0..23)
        Map<String, Long> mapaCalorContagem = new HashMap<>();
        for (Chamado c : filtrados) {
            LocalDateTime da = c.getDataAbertura() != null ? c.getDataAbertura() : c.getCriadoEm();
            if (da != null) {
                int dia = da.getDayOfWeek().getValue();
                int hora = da.getHour();
                String key = dia + "-" + hora;
                mapaCalorContagem.put(key, mapaCalorContagem.getOrDefault(key, 0L) + 1);
            }
        }
        List<RelatoriosResponse.ItemMapaCalor> mapaCalor = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            for (int h = 7; h <= 19; h++) { // Horário operacional padrão expandido
                long tot = mapaCalorContagem.getOrDefault(d + "-" + h, 0L);
                mapaCalor.add(new RelatoriosResponse.ItemMapaCalor(d, h, tot));
            }
        }

        // 13. Taxa de Resolução no Primeiro Contato (FCR - First Contact Resolution)
        // Chamados resolvidos sem histórico de encaminhamento ou devolução
        List<Chamado> resolvidosList = filtrados.stream()
                .filter(c -> c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO)
                .toList();

        double fcrTaxa = 100.0;
        if (!resolvidosList.isEmpty()) {
            Set<Long> idsComTransferencia = historicos.stream()
                    .filter(h -> h.getTipo() == TipoHistorico.ENCAMINHAMENTO || h.getTipo() == TipoHistorico.DEVOLUCAO)
                    .map(h -> h.getChamado() != null ? h.getChamado().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            long fcrTotal = resolvidosList.stream()
                    .filter(c -> !idsComTransferencia.contains(c.getId()))
                    .count();

            fcrTaxa = Math.round(((double) fcrTotal / resolvidosList.size()) * 1000.0) / 10.0;
        }

        return new RelatoriosResponse(
                totalChamados,
                porSetor,
                porStatus,
                porPrioridade,
                porServico,
                tecnicos,
                periodoResumo,
                temposMedios,
                usuariosResumo,
                satisfacaoResumo,
                encaminhamentos,
                evolucaoMensal,
                mapaCalor,
                fcrTaxa
        );
    }

    private boolean estaAtrasado(Chamado c) {
        if (c.getPrazoLimite() == null) return false;
        if (c.getStatus() == StatusChamado.RESOLVIDO || c.getStatus() == StatusChamado.FECHADO || c.getStatus() == StatusChamado.CONCLUIDO) {
            return c.getResolvidoEm() != null && c.getResolvidoEm().isAfter(c.getPrazoLimite());
        }
        return LocalDateTime.now().isAfter(c.getPrazoLimite());
    }
}
