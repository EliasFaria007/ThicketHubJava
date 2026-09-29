package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.Papel;
import thickethub.domain.model.Auditoria;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.ImportarLoteResponse;
import thickethub.dto.response.UsuarioResponse;
import thickethub.exception.BusinessException;
import thickethub.exception.ConflictException;
import thickethub.exception.ForbiddenException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.AuditoriaRepository;
import thickethub.repository.ConfigDominioRepository;
import thickethub.repository.UsuarioRepository;
import thickethub.service.UsuarioService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfigDominioRepository configDominioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    // ============================================================
    // criarUsuario()
    // ============================================================
    @Override
    @Transactional
    public UsuarioResponse criarUsuario(Usuario adminLogado, CriarUsuarioRequest request) {
        String dominio = extrairDominio(request.email());
        if (!configDominioRepository.existsByDominioAndAtivoTrue(dominio)) {
            throw new BusinessException("Domínio de e-mail não autorizado: " + dominio);
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Já existe um usuário com este e-mail.");
        }

        Papel papel = request.papel();
        if (papel == Papel.ADMIN && (adminLogado == null || (adminLogado.getPapel() != Papel.ADMIN
                && adminLogado.getPapel() != Papel.SUPERUSUARIO && adminLogado.getPapel() != Papel.SUPER))) {
            throw new ForbiddenException("Apenas administradores criam outros administradores.");
        }
        if ((papel == Papel.SUPERUSUARIO || papel == Papel.SUPER) && (adminLogado == null || (adminLogado.getPapel() != Papel.SUPERUSUARIO && adminLogado.getPapel() != Papel.SUPER))) {
            throw new ForbiddenException("Apenas superusuários criam outros superusuários.");
        }

        String senhaTemporaria = gerarSenhaAleatoria(10);
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senhaHash(passwordEncoder.encode(senhaTemporaria))
                .papel(papel)
                .setor(request.setor())
                .telefone(request.telefone())
                .localidade(request.localidade())
                .podeCadastrarUsuarios(request.podeCadastrarUsuarios() != null ? request.podeCadastrarUsuarios() : false)
                .setoresLiberados(request.setores() != null ? request.setores() : new HashSet<>())
                .ativo(true)
                .primeiroAcessoConcluido(false)
                .criadoEm(LocalDateTime.now())
                .build();
        usuarioRepository.save(usuario);

        auditar(adminLogado, "usuario.criar", "email=" + usuario.getEmail() + "; papel=" + papel);
        log.info("Usuário criado id={} por admin id={}", usuario.getId(),
                adminLogado != null ? adminLogado.getId() : "sistema");

        return UsuarioResponse.de(usuario, senhaTemporaria);
    }

    // ============================================================
    // importarEmLote()
    // ============================================================
    @Override
    @Transactional
    public ImportarLoteResponse importarEmLote(Usuario adminLogado, ImportarUsuariosLoteRequest request) {
        List<ImportarLoteResponse.ItemSucesso> sucessos = new ArrayList<>();
        List<ImportarLoteResponse.ItemFalha> falhas = new ArrayList<>();
        Set<String> emailsDaLote = new HashSet<>();
        Map<String, Boolean> dominiosValidos = new HashMap<>();

        for (int i = 0; i < request.usuarios().size(); i++) {
            var item = request.usuarios().get(i);
            int linha = i + 1;
            try {
                String dominio = extrairDominio(item.email());
                boolean dominioOk = dominiosValidos.computeIfAbsent(dominio,
                        d -> configDominioRepository.existsByDominioAndAtivoTrue(d));
                if (!dominioOk) {
                    falhas.add(new ImportarLoteResponse.ItemFalha(linha, item.email(),
                            "Domínio não autorizado: " + dominio));
                    continue;
                }

                if (!emailsDaLote.add(item.email().toLowerCase())) {
                    falhas.add(new ImportarLoteResponse.ItemFalha(linha, item.email(),
                            "E-mail duplicado no lote"));
                    continue;
                }

                if (usuarioRepository.existsByEmail(item.email())) {
                    falhas.add(new ImportarLoteResponse.ItemFalha(linha, item.email(),
                            "E-mail já cadastrado no sistema"));
                    continue;
                }

                Papel papel = item.papel() != null ? item.papel() : Papel.USUARIO;
                if (papel == Papel.ADMIN || papel == Papel.SUPERUSUARIO || papel == Papel.SUPER) {
                    falhas.add(new ImportarLoteResponse.ItemFalha(linha, item.email(),
                            "Papel " + papel + " não pode ser atribuído via importação"));
                    continue;
                }

                String setor = item.setor();
                Usuario usuario = Usuario.builder()
                        .nome(item.nome())
                        .email(item.email())
                        .senhaHash(null)
                        .papel(papel)
                        .setor(setor)
                        .setoresLiberados(setor != null ? Set.of(setor) : new HashSet<>())
                        .ativo(true)
                        .primeiroAcessoConcluido(false)
                        .criadoEm(LocalDateTime.now())
                        .build();
                usuarioRepository.save(usuario);

                sucessos.add(new ImportarLoteResponse.ItemSucesso(linha, usuario.getEmail(), usuario.getId()));

            } catch (Exception e) {
                log.warn("Falha ao importar linha {}: {}", linha, e.getMessage());
                falhas.add(new ImportarLoteResponse.ItemFalha(linha, item.email(),
                        "Erro inesperado: " + e.getMessage()));
            }
        }

        auditar(adminLogado, "usuario.importar_lote",
                "total=" + request.usuarios().size() + "; sucessos=" + sucessos.size() + "; falhas=" + falhas.size());
        log.info("Importação em lote: {} sucessos, {} falhas", sucessos.size(), falhas.size());

        return new ImportarLoteResponse(request.usuarios().size(), sucessos.size(), falhas.size(), sucessos, falhas);
    }

    // ============================================================
    // buscarPorId() / listarTodos()
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        return UsuarioResponse.de(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    // ============================================================
    // atualizar()
    // ============================================================
    @Override
    @Transactional
    public UsuarioResponse atualizar(Usuario logado, Long id, AtualizarUsuarioRequest request) {
        log.info("[USUARIO ATUALIZAR] Usuário '{}' (id={}) tentando atualizar usuário id={}",
                logado.getNome(), logado.getId(), id);

        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        boolean isAdm = logado.getPapel() == Papel.ADMIN || logado.getPapel() == Papel.SUPERUSUARIO || logado.getPapel() == Papel.SUPER;

        if (!isAdm && !alvo.getId().equals(logado.getId())) {
            log.warn("[USUARIO ATUALIZAR] ACESSO NEGADO — '{}' tentou alterar dados de '{}'",
                    logado.getEmail(), alvo.getEmail());
            throw new ForbiddenException("Você não tem permissão para alterar dados de outro usuário.");
        }

        // Apenas ADMIN/SUPERUSUARIO/SUPER podem alterar papel
        if (request.papel() != null && request.papel() != alvo.getPapel()) {
            if (!isAdm) {
                log.warn("[USUARIO ATUALIZAR] TENTATIVA ILEGAL — '{}' tentou alterar papel de '{}' para {}",
                        logado.getEmail(), alvo.getEmail(), request.papel());
                throw new ForbiddenException("Apenas administradores podem alterar o papel do usuário.");
            }
            log.info("[USUARIO ATUALIZAR] Papel alterado de {} para {} para usuário '{}'",
                    alvo.getPapel(), request.papel(), alvo.getEmail());
            alvo.setPapel(request.papel());
        }

        if (!alvo.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Já existe um usuário com o e-mail: " + request.email());
        }

        alvo.setNome(request.nome());
        alvo.setEmail(request.email());
        if (request.setor() != null) alvo.setSetor(request.setor());
        if (request.telefone() != null) alvo.setTelefone(request.telefone());
        if (request.localidade() != null) alvo.setLocalidade(request.localidade());

        if (isAdm) {
            if (request.ativo() != null) alvo.setAtivo(request.ativo());
            if (request.podeCadastrarUsuarios() != null) alvo.setPodeCadastrarUsuarios(request.podeCadastrarUsuarios());
            if (request.setoresLiberados() != null) alvo.setSetoresLiberados(request.setoresLiberados());
        }

        usuarioRepository.save(alvo);
        auditar(logado, "usuario.atualizar", "alvo=" + alvo.getEmail());
        log.info("[USUARIO ATUALIZAR] SUCESSO — usuário '{}' (id={}) atualizado por '{}'",
                alvo.getNome(), alvo.getId(), logado.getNome());

        return UsuarioResponse.de(alvo);
    }

    // ============================================================
    // desativar() / ativar()
    // ============================================================
    @Override
    @Transactional
    public void desativar(Usuario adminLogado, Long id) {
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (Objects.equals(alvo.getId(), adminLogado.getId())) {
            log.warn("[USUARIO DESATIVAR] '{}' tentou desativar a própria conta", adminLogado.getEmail());
            throw new BusinessException("Você não pode desativar a própria conta.");
        }
        alvo.setAtivo(false);
        usuarioRepository.save(alvo);
        auditar(adminLogado, "usuario.desativar", "alvo=" + alvo.getEmail());
        log.info("[USUARIO DESATIVAR] Conta de '{}' (id={}) desativada por '{}'",
                alvo.getNome(), alvo.getId(), adminLogado.getNome());
    }

    @Override
    @Transactional
    public void ativar(Usuario adminLogado, Long id) {
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        alvo.setAtivo(true);
        usuarioRepository.save(alvo);
        auditar(adminLogado, "usuario.ativar", "alvo=" + alvo.getEmail());
        log.info("[USUARIO ATIVAR] Conta de '{}' (id={}) reativada por '{}'",
                alvo.getNome(), alvo.getId(), adminLogado.getNome());
    }

    // ============================================================
    // resetarSenhaManual()
    // ============================================================
    @Override
    @Transactional
    public UsuarioResponse resetarSenhaManual(Usuario solicitante, Long usuarioId) {
        log.info("[RESET SENHA] '{}' (papel={}) solicitando reset de senha para usuário id={}",
                solicitante.getNome(), solicitante.getPapel(), usuarioId);

        boolean podeResetar = solicitante.getPapel() == Papel.ADMIN
                || solicitante.getPapel() == Papel.SUPERUSUARIO
                || solicitante.getPapel() == Papel.SUPER
                || solicitante.getPapel() == Papel.TECNICO;

        if (!podeResetar) {
            log.warn("[RESET SENHA] NEGADO — papel '{}' não tem permissão para resetar senhas", solicitante.getPapel());
            throw new ForbiddenException("Seu perfil não permite resetar senhas.");
        }

        Usuario alvo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (solicitante.getPapel() == Papel.TECNICO && !compartilhaSetor(solicitante, alvo)) {
            log.warn("[RESET SENHA] NEGADO — técnico '{}' tentou resetar senha de usuário de setor diferente: '{}'",
                    solicitante.getEmail(), alvo.getEmail());
            throw new ForbiddenException("Você só pode resetar senhas de usuários do seu setor.");
        }

        String novaSenha = gerarSenhaAleatoria(10);
        alvo.setSenhaHash(passwordEncoder.encode(novaSenha));
        alvo.setPrimeiroAcessoConcluido(false);
        usuarioRepository.save(alvo);

        auditar(solicitante, "usuario.resetar_senha", "alvo=" + alvo.getEmail());
        log.info("[RESET SENHA] SUCESSO — senha de '{}' (id={}) resetada por '{}' (id={})",
                alvo.getNome(), alvo.getId(), solicitante.getNome(), solicitante.getId());

        return UsuarioResponse.de(alvo, novaSenha);
    }

    // ============================================================
    // alterarSenha()
    // ============================================================
    @Override
    @Transactional
    public void alterarSenha(Usuario logado, AlterarSenhaRequest request) {
        log.info("[ALTERAR SENHA] Usuário '{}' (id={}) solicitando alteração de senha", logado.getNome(), logado.getId());

        if (!passwordEncoder.matches(request.senhaAtual(), logado.getSenhaHash())) {
            log.warn("[ALTERAR SENHA] FALHOU — senha atual incorreta para usuário '{}'", logado.getEmail());
            throw new BusinessException("Senha atual incorreta.");
        }
        logado.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(logado);
        auditar(logado, "usuario.alterar_senha", "");
        log.info("[ALTERAR SENHA] SUCESSO — senha alterada para usuário '{}' (id={})", logado.getNome(), logado.getId());
    }

    // ============================================================
    // Auxiliares
    // ============================================================

    private String extrairDominio(String email) {
        int arroba = email != null ? email.lastIndexOf('@') : -1;
        if (arroba < 0 || arroba == (email == null ? 0 : email.length()) - 1) {
            throw new BusinessException("E-mail inválido: " + email);
        }
        return email.substring(arroba + 1).toLowerCase();
    }

    private String gerarSenhaAleatoria(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private boolean compartilhaSetor(Usuario tecnico, Usuario alvo) {
        return tecnico.getSetoresLiberados() != null
                && alvo.getSetoresLiberados() != null
                && tecnico.getSetoresLiberados().stream()
                .anyMatch(alvo.getSetoresLiberados()::contains);
    }

    private void auditar(Usuario usuario, String acao, String detalhe) {
        try {
            Auditoria.AuditoriaBuilder builder = Auditoria.builder()
                    .acao(acao)
                    .detalhe(detalhe);
            if (usuario != null) {
                builder.usuarioId(usuario.getId()).usuarioNome(usuario.getNome());
            }
            auditoriaRepository.save(builder.build());
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria: {}", e.getMessage());
        }
    }
}