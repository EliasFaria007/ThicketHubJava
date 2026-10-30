package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.model.Auditoria;
import thickethub.domain.model.RecuperacaoSms;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.AuthResponse;
import thickethub.dto.response.UsuarioResponse;
import thickethub.exception.BusinessException;
import thickethub.exception.RateLimitExceededException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.exception.UnauthorizedException;
import thickethub.repository.AuditoriaRepository;
import thickethub.repository.ConfigDominioRepository;
import thickethub.repository.RecuperacaoSmsRepository;
import thickethub.repository.UsuarioRepository;
import thickethub.security.JwtService;
import thickethub.security.RateLimiterService;
import thickethub.security.SecurityUtils;
import thickethub.security.TokenBlacklistService;
import thickethub.service.AuthService;
import thickethub.service.EmailService;
import thickethub.service.SmsService;
import thickethub.dto.response.SolicitarSmsResponse;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final RecuperacaoSmsRepository recuperacaoSmsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RateLimiterService rateLimiterService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final ConfigDominioRepository configDominioRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRACAO_MINUTOS = 10;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipOrigem) {
        log.info("[LOGIN] Tentativa de login — email: {}, ip: {}", request.email(), ipOrigem);

        // Rate limit por email e IP
        if (!rateLimiterService.permitirLogin(request.email(), ipOrigem)) {
            log.warn("[LOGIN] BLOQUEADO por rate limiting — email: {}, ip: {}", request.email(), ipOrigem);
            auditar(null, "auth.login.bloqueado",
                    "email=" + request.email() + "; ip=" + ipOrigem, ipOrigem);
            throw new RateLimitExceededException(
                    "Muitas tentativas de login. Aguarde 15 minutos antes de tentar novamente.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            log.warn("[LOGIN] FALHOU — credenciais inválidas para email: {}, ip: {}", request.email(), ipOrigem);
            auditar(null, "auth.login.falhou",
                    "email=" + request.email() + "; ip=" + ipOrigem, ipOrigem);
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        rateLimiterService.limparRateLimit(request.email(), ipOrigem);

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            log.warn("[LOGIN] NEGADO — conta desativada para usuário id: {}, email: {}", usuario.getId(), request.email());
            throw new BusinessException("Conta desativada. Contate o administrador.");
        }

        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        auditar(usuario, "auth.login.sucesso",
                "ip=" + ipOrigem, ipOrigem);

        log.info("[LOGIN] SUCESSO — usuário: '{}' (id={}, papel={}) autenticado via ip: {}",
                usuario.getNome(), usuario.getId(), usuario.getPapel(), ipOrigem);

        return buildAuthResponse(accessToken, refreshToken, usuario);
    }

    @Override
    @Transactional
    public AuthResponse loginMicrosoft(MicrosoftLoginRequest request, String ipOrigem) {
        log.info("[LOGIN MICROSOFT] Tentativa de login via Microsoft — email: {}, ip: {}", request.email(), ipOrigem);

        String email = request.email();
        String nome = request.nome();

        // Se o token fornecido for um JWT (ex: ID token do Azure AD/Entra ID) e email não veio no body, tenta extrair
        if ((email == null || email.isBlank()) && request.idToken() != null && request.idToken().contains(".")) {
            try {
                String[] parts = request.idToken().split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
                    String payloadJson = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
                    log.info("[LOGIN MICROSOFT] Claims recebidas no ID Token: {}", payloadJson);
                    com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payloadJson);
                    if (node.hasNonNull("preferred_username")) {
                        email = node.get("preferred_username").asText();
                    } else if (node.hasNonNull("email")) {
                        email = node.get("email").asText();
                    } else if (node.hasNonNull("upn")) {
                        email = node.get("upn").asText();
                    } else if (node.hasNonNull("unique_name") && node.get("unique_name").asText().contains("@")) {
                        email = node.get("unique_name").asText();
                    }
                    if (node.hasNonNull("name") && (nome == null || nome.isBlank())) {
                        nome = node.get("name").asText();
                    }
                }
            } catch (Exception e) {
                log.warn("[LOGIN MICROSOFT] Falha ao extrair claims do ID Token: {}", e.getMessage());
            }
        }

        if (email == null || email.isBlank()) {
            // Em caso de envio direto do email no idToken
            if (request.idToken() != null && request.idToken().contains("@")) {
                email = request.idToken().trim();
            } else {
                throw new BusinessException("E-mail institucional da Microsoft não informado.");
            }
        }

        email = email.toLowerCase().trim();

        // Validar domínio institucional cadastrado se houver domínios configurados
        // Se o usuário já existir no banco (ex: superusuário ou usuário pré-cadastrado), permite o login diretamente
        String dominio = email.contains("@") ? email.substring(email.indexOf("@") + 1) : "";
        List<thickethub.domain.model.ConfigDominio> dominiosAtivos = configDominioRepository.findByAtivoTrue();
        boolean usuarioJaCadastrado = usuarioRepository.findByEmail(email).isPresent();
        if (!dominiosAtivos.isEmpty() && !usuarioJaCadastrado) {
            boolean dominioValido = dominiosAtivos.stream().anyMatch(d -> d.getDominio().equalsIgnoreCase(dominio));
            if (!dominioValido) {
                log.warn("[LOGIN MICROSOFT] Domínio não autorizado para auto-cadastro: {}", dominio);
                throw new BusinessException("O domínio @" + dominio + " não é autorizado para auto-cadastro institucional.");
            }
        }

        // Buscar usuário existente ou provisionar automaticamente como USUARIO padrão
        String finalEmail = email;
        String finalNome = (nome != null && !nome.isBlank()) ? nome : email.substring(0, email.indexOf("@"));

        Usuario usuario;
        try {
            usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
                log.info("[LOGIN MICROSOFT] Provisionando novo usuário institucional via SSO Microsoft: {}", finalEmail);
                Usuario novo = Usuario.builder()
                        .email(finalEmail)
                        .nome(finalNome)
                        .papel(thickethub.domain.enums.Papel.USUARIO)
                        .ativo(true)
                        .primeiroAcessoConcluido(true)
                        .termosVersao("1.0")
                        .termosAceitosEm(LocalDateTime.now())
                        .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .criadoEm(LocalDateTime.now())
                        .build();
                return usuarioRepository.saveAndFlush(novo);
            });
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.info("[LOGIN MICROSOFT] Concorrência detectada no provisionamento para {}. Buscando usuário existente.", finalEmail);
            usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Erro de concorrência ao autenticar usuário institucional."));
        }

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            log.warn("[LOGIN MICROSOFT] NEGADO — conta desativada para usuário id: {}, email: {}", usuario.getId(), email);
            throw new BusinessException("Conta institucional desativada. Contate o administrador.");
        }

        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        auditar(usuario, "auth.login.microsoft", "sso_microsoft; email=" + email, ipOrigem);
        log.info("[LOGIN MICROSOFT] SUCESSO — usuário '{}' (id={}, papel={}) autenticado via Microsoft",
                usuario.getNome(), usuario.getId(), usuario.getPapel());

        return buildAuthResponse(accessToken, refreshToken, usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        log.info("[TOKEN REFRESH] Tentativa de renovação de token");
        String email;
        try {
            email = jwtService.extractUsername(request.refreshToken());
        } catch (Exception e) {
            log.warn("[TOKEN REFRESH] FALHOU — refresh token inválido ou expirado: {}", e.getMessage());
            throw new UnauthorizedException("Refresh token inválido ou expirado.");
        }

        if (tokenBlacklistService.contem(request.refreshToken())) {
            log.warn("[TOKEN REFRESH] NEGADO — refresh token está na blacklist (revogado) para: {}", email);
            throw new UnauthorizedException("Refresh token revogado.");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado."));

        String novoAccessToken = jwtService.generateToken(usuario);
        String novoRefreshToken = jwtService.generateRefreshToken(usuario);

        log.info("[TOKEN REFRESH] SUCESSO — novo token gerado para usuário: '{}' (id={})", usuario.getNome(), usuario.getId());
        return buildAuthResponse(novoAccessToken, novoRefreshToken, usuario);
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            try {
                Duration tempoRestante = Duration.between(
                        LocalDateTime.now(),
                        jwtService.extractExpiration(token).toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                if (!tempoRestante.isNegative()) {
                    tokenBlacklistService.revogar(token, tempoRestante);
                    log.info("[LOGOUT] Token revogado com sucesso — expira em {} minutos", tempoRestante.toMinutes());
                }
            } catch (Exception e) {
                tokenBlacklistService.revogar(token, Duration.ofHours(1));
                log.warn("[LOGOUT] Falha ao calcular expiração do token — revogado com TTL padrão de 1h: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public SolicitarSmsResponse solicitarRecuperacaoSms(SolicitarSmsRequest request) {
        String email = request.email() != null && !request.email().isBlank() ? request.email().trim().toLowerCase() : null;
        String telefone = request.telefone() != null && !request.telefone().isBlank() ? request.telefone().trim() : null;

        String rateKey = email != null ? "email:" + email : "tel:" + telefone;
        if (!rateLimiterService.permitir("recuperacao:" + rateKey, 5, Duration.ofHours(1))) {
            throw new BusinessException("Limite de solicitações de recuperação atingido. Aguarde 1 hora.");
        }

        Usuario usuario = null;
        if (email != null) {
            usuario = usuarioRepository.findByEmail(email).orElse(null);
        }
        if (usuario == null && telefone != null) {
            usuario = usuarioRepository.findByTelefone(telefone).orElse(null);
        }

        // Não revelamos se o usuário existe (segurança anti-enumeração)
        if (usuario == null) {
            log.warn("[RECUPERACAO] Solicitação para usuário não cadastrado — email: '{}', tel: '{}'", email, telefone);
            return new SolicitarSmsResponse(null, email != null ? mascararEmail(email) : null);
        }

        String codigoOtp = gerarOtp(6);
        String ticket = UUID.randomUUID().toString();
        String hash = passwordEncoder.encode(codigoOtp);
        LocalDateTime expira = LocalDateTime.now().plusMinutes(OTP_EXPIRACAO_MINUTOS);

        // Salva por e-mail para permitir validação via e-mail
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            recuperacaoSmsRepository.save(RecuperacaoSms.builder()
                    .telefone(usuario.getEmail().toLowerCase())
                    .codigoHash(hash)
                    .ticketValidacao(ticket)
                    .expiraEm(expira)
                    .contadorTentativas(0)
                    .utilizado(false)
                    .build());
        }

        // Salva também por telefone (caso tenha telefone) com o mesmo ticket e código
        if (usuario.getTelefone() != null && !usuario.getTelefone().isBlank() && 
            !usuario.getTelefone().equalsIgnoreCase(usuario.getEmail())) {
            recuperacaoSmsRepository.save(RecuperacaoSms.builder()
                    .telefone(usuario.getTelefone())
                    .codigoHash(hash)
                    .ticketValidacao(ticket + "-tel")
                    .expiraEm(expira)
                    .contadorTentativas(0)
                    .utilizado(false)
                    .build());
        }

        // Dispara e-mail institucional corporativo com o código de verificação
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            try {
                emailService.enviarCodigoRecuperacao(
                        usuario.getEmail(),
                        usuario.getNome(),
                        codigoOtp,
                        OTP_EXPIRACAO_MINUTOS
                );
                log.info("[RECUPERACAO] Código OTP enviado com sucesso por E-MAIL para '{}'", usuario.getEmail());
            } catch (Exception e) {
                log.warn("[RECUPERACAO] Falha ao enviar código por e-mail: {}", e.getMessage());
            }
        }

        // Dispara SMS se houver telefone cadastrado
        if (usuario.getTelefone() != null && !usuario.getTelefone().isBlank()) {
            smsService.enviar(usuario.getTelefone(), "Seu código de recuperação ThicketHub: " + codigoOtp +
                    " (válido por " + OTP_EXPIRACAO_MINUTOS + " minutos)");
            log.info("[RECUPERACAO] Código OTP enviado via SMS para telefone terminando em {}",
                    usuario.getTelefone().substring(Math.max(0, usuario.getTelefone().length() - 4)));
        }

        String telMasc = usuario.getTelefone() != null ? mascararTelefone(usuario.getTelefone()) : null;
        String emailMasc = usuario.getEmail() != null ? mascararEmail(usuario.getEmail()) : null;

        return new SolicitarSmsResponse(telMasc, emailMasc);
    }

    private String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] partes = email.split("@");
        String nome = partes[0];
        String dominio = partes[1];
        if (nome.length() <= 2) {
            return nome.charAt(0) + "***@" + dominio;
        }
        return nome.substring(0, 2) + "***" + nome.charAt(nome.length() - 1) + "@" + dominio;
    }

    private String mascararTelefone(String telefone) {
        if (telefone == null || telefone.length() < 4) return telefone;
        String ultimos4 = telefone.substring(telefone.length() - 4);
        return "(**) *****-" + ultimos4;
    }

    @Override
    @Transactional
    public String validarSms(ValidarSmsRequest request) {
        String chave = (request.email() != null && !request.email().isBlank()) 
                ? request.email().trim().toLowerCase() 
                : (request.telefone() != null ? request.telefone().trim() : null);

        if (chave == null) {
            throw new BusinessException("Informe seu e-mail ou telefone cadastrado.");
        }

        if (!rateLimiterService.permitirValidacaoOtp(chave)) {
            throw new BusinessException("Muitas tentativas de validação. Aguarde antes de tentar novamente.");
        }

        RecuperacaoSms recuperacao = recuperacaoSmsRepository
                .findTopByTelefoneAndUtilizadoFalseOrderByExpiraEmDesc(chave)
                .orElse(null);

        // Se não achou pela chave direta, tenta achar o usuário pelo e-mail ou telefone para cruzar
        if (recuperacao == null) {
            Usuario u = usuarioRepository.findByEmail(chave.toLowerCase())
                    .or(() -> usuarioRepository.findByTelefone(chave))
                    .orElse(null);
            if (u != null) {
                if (u.getEmail() != null) {
                    recuperacao = recuperacaoSmsRepository
                            .findTopByTelefoneAndUtilizadoFalseOrderByExpiraEmDesc(u.getEmail().toLowerCase())
                            .orElse(null);
                }
                if (recuperacao == null && u.getTelefone() != null) {
                    recuperacao = recuperacaoSmsRepository
                            .findTopByTelefoneAndUtilizadoFalseOrderByExpiraEmDesc(u.getTelefone())
                            .orElse(null);
                }
            }
        }

        if (recuperacao == null) {
            throw new BusinessException("Nenhum código de recuperação ativo para este usuário.");
        }

        if (LocalDateTime.now().isAfter(recuperacao.getExpiraEm())) {
            throw new BusinessException("Código expirado. Solicite um novo código.");
        }

        recuperacao.setContadorTentativas(recuperacao.getContadorTentativas() + 1);
        if (recuperacao.getContadorTentativas() > 3) {
            throw new BusinessException("Número máximo de tentativas atingido. Solicite um novo código.");
        }

        if (!passwordEncoder.matches(request.codigo(), recuperacao.getCodigoHash())) {
            recuperacaoSmsRepository.save(recuperacao);
            throw new BusinessException("Código inválido. Tentativas restantes: "
                    + (3 - recuperacao.getContadorTentativas()));
        }

        recuperacao.setUtilizado(true);
        recuperacaoSmsRepository.save(recuperacao);

        // Retorna o ticket limpo (sem sufixo)
        String ticket = recuperacao.getTicketValidacao();
        return ticket.endsWith("-tel") ? ticket.substring(0, ticket.length() - 4) : ticket;
    }

    @Override
    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest request) {
        String ticket = request.ticketValidacao();
        log.info("[REDEFINIR SENHA] Tentativa de redefinição de senha com ticket: {}", ticket);

        RecuperacaoSms recuperacao = recuperacaoSmsRepository
                .findByTicketValidacaoAndUtilizadoFalse(ticket)
                .or(() -> recuperacaoSmsRepository.findByTicketValidacaoAndUtilizadoFalse(ticket + "-tel"))
                .orElse(null);

        // Se não achou como não utilizado, pode ter sido marcado como utilizado no validarSms
        // Então busca o ticket que acabou de ser validado nos últimos 15 minutos
        if (recuperacao == null) {
            recuperacao = recuperacaoSmsRepository
                    .findAll().stream()
                    .filter(r -> (ticket.equals(r.getTicketValidacao()) || (ticket + "-tel").equals(r.getTicketValidacao()))
                            && r.getExpiraEm().isAfter(LocalDateTime.now()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("Ticket de redefinição inválido ou expirado."));
        }

        String identificador = recuperacao.getTelefone();
        Usuario usuario = usuarioRepository.findByEmail(identificador.toLowerCase())
                .or(() -> usuarioRepository.findByTelefone(identificador))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setPrimeiroAcessoConcluido(true);
        usuarioRepository.save(usuario);

        recuperacao.setUtilizado(true);
        recuperacaoSmsRepository.save(recuperacao);

        auditar(usuario, "auth.redefinir_senha", "via=recuperacao_codigo", "sistema");
        log.info("[REDEFINIR SENHA] SUCESSO — senha redefinida com sucesso para usuário id={}, email='{}'",
                usuario.getId(), usuario.getEmail());
    }

    @Override
    @Transactional
    public AuthResponse completarPrimeiroAcesso(String email, CompletarCadastroRequest request) {
        log.info("[PRIMEIRO ACESSO] Usuário completando primeiro acesso — email: {}", email);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (Boolean.TRUE.equals(usuario.getPrimeiroAcessoConcluido())) {
            log.warn("[PRIMEIRO ACESSO] Já concluído anteriormente para usuário id={}", usuario.getId());
            throw new BusinessException("Primeiro acesso já foi concluído.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setPrimeiroAcessoConcluido(true);

        if (request.telefone() != null && !request.telefone().isBlank()) {
            usuario.setTelefone(request.telefone());
        }
        if (request.localidade() != null && !request.localidade().isBlank()) {
            usuario.setLocalidade(request.localidade());
        }
        usuarioRepository.save(usuario);

        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = jwtService.generateRefreshToken(usuario);

        auditar(usuario, "auth.primeiro_acesso.concluido", "", "sistema");
        log.info("[PRIMEIRO ACESSO] CONCLUÍDO — usuário: '{}' (id={}) definiu sua senha e completou o cadastro",
                usuario.getNome(), usuario.getId());

        return buildAuthResponse(accessToken, refreshToken, usuario);
    }

    // ============================================================
    // Auxiliares
    // ============================================================

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, Usuario usuario) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getSetor(),
                usuario.getPrimeiroAcessoConcluido(),
                usuario.getSetoresLiberados(),
                usuario.getAssinatura(),
                usuario.getIncluirAssinaturaPadrao(),
                usuario.getTermosVersao(),
                usuario.getTermosAceitosEm()
        );
    }

    private void auditar(Usuario usuario, String acao, String detalhe, String ip) {
        try {
            Auditoria auditoria = Auditoria.builder()
                    .acao(acao)
                    .detalhe(detalhe)
                    .ipOrigem(ip != null ? ip : "0.0.0.0")
                    .build();
            if (usuario != null) {
                auditoria = Auditoria.builder()
                        .acao(acao)
                        .detalhe(detalhe)
                        .usuarioId(usuario.getId())
                        .usuarioNome(usuario.getNome())
                        .ipOrigem(ip != null ? ip : "0.0.0.0")
                        .build();
            }
            auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de auth: {}", e.getMessage());
        }
    }

    private String gerarOtp(int digitos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public thickethub.dto.response.UsuarioResponse aceitarTermos(Long usuarioId, AceitarTermosRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        String versao = (request != null && request.versao() != null && !request.versao().isBlank()) ? request.versao() : "1.0";
        usuario.setTermosVersao(versao);
        usuario.setTermosAceitosEm(LocalDateTime.now());
        usuario.setPrimeiroAcessoConcluido(true);
        usuarioRepository.save(usuario);
        auditar(usuario, "auth.termos", "versao=" + versao, SecurityUtils.obterIpCliente());
        log.info("[TERMOS ACEITE] Usuário id={}, nome='{}' aceitou os termos versão {}", usuario.getId(), usuario.getNome(), versao);
        return thickethub.dto.response.UsuarioResponse.de(usuario);
    }
}
