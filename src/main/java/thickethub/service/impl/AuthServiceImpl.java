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
import thickethub.exception.ResourceNotFoundException;
import thickethub.exception.UnauthorizedException;
import thickethub.repository.AuditoriaRepository;
import thickethub.repository.RecuperacaoSmsRepository;
import thickethub.repository.UsuarioRepository;
import thickethub.security.JwtService;
import thickethub.security.RateLimiterService;
import thickethub.security.TokenBlacklistService;
import thickethub.service.AuthService;
import thickethub.service.SmsService;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
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
            throw new BusinessException(
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
    public void solicitarRecuperacaoSms(SolicitarSmsRequest request) {
        String telefone = request.telefone();

        if (!rateLimiterService.permitir("sms.recuperacao:" + telefone, 3, Duration.ofHours(1))) {
            throw new BusinessException("Limite de solicitações de SMS atingido. Aguarde 1 hora.");
        }

        Usuario usuario = usuarioRepository.findByTelefone(telefone)
                .orElse(null);

        // Não revelamos se o telefone existe (segurança anti-enumeração)
        if (usuario == null) {
            log.warn("Recuperação de SMS solicitada para telefone não cadastrado: {}", telefone);
            return;
        }

        String codigoOtp = gerarOtp(6);
        String ticket = UUID.randomUUID().toString();

        RecuperacaoSms recuperacao = RecuperacaoSms.builder()
                .telefone(telefone)
                .codigoHash(passwordEncoder.encode(codigoOtp))
                .ticketValidacao(ticket)
                .expiraEm(LocalDateTime.now().plusMinutes(OTP_EXPIRACAO_MINUTOS))
                .contadorTentativas(0)
                .utilizado(false)
                .build();

        recuperacaoSmsRepository.save(recuperacao);
        smsService.enviar(telefone, "Seu código de recuperação ThicketHub: " + codigoOtp +
                " (válido por " + OTP_EXPIRACAO_MINUTOS + " minutos)");

        log.info("OTP de recuperação enviado para telefone terminando em {}",
                telefone.substring(Math.max(0, telefone.length() - 4)));
    }

    @Override
    @Transactional
    public String validarSms(ValidarSmsRequest request) {
        if (!rateLimiterService.permitirValidacaoOtp(request.telefone())) {
            throw new BusinessException("Muitas tentativas de validação. Aguarde antes de tentar novamente.");
        }

        RecuperacaoSms recuperacao = recuperacaoSmsRepository
                .findTopByTelefoneAndUtilizadoFalseOrderByExpiraEmDesc(request.telefone())
                .orElseThrow(() -> new BusinessException("Nenhum código de recuperação ativo para este telefone."));

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

        return recuperacao.getTicketValidacao();
    }

    @Override
    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest request) {
        log.info("[REDEFINIR SENHA] Tentativa de redefinição de senha via ticket");
        RecuperacaoSms recuperacao = recuperacaoSmsRepository
                .findByTicketValidacaoAndUtilizadoFalse(request.ticketValidacao())
                .orElseThrow(() -> new BusinessException("Ticket de redefinição inválido ou já utilizado."));

        if (LocalDateTime.now().isAfter(recuperacao.getExpiraEm())) {
            log.warn("[REDEFINIR SENHA] Ticket expirado para telefone terminando em {}",
                    recuperacao.getTelefone().substring(Math.max(0, recuperacao.getTelefone().length() - 4)));
            throw new BusinessException("Ticket expirado. Solicite um novo código.");
        }

        // Encontrar usuário pelo telefone associado ao ticket
        Usuario usuario = usuarioRepository.findByTelefone(recuperacao.getTelefone())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuario.setPrimeiroAcessoConcluido(true);
        usuarioRepository.save(usuario);

        recuperacao.setUtilizado(true);
        recuperacaoSmsRepository.save(recuperacao);

        auditar(usuario, "auth.redefinir_senha", "via=sms", "sistema");
        log.info("[REDEFINIR SENHA] SUCESSO — senha redefinida via SMS para usuário id={}, nome='{}'",
                usuario.getId(), usuario.getNome());
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
                usuario.getSetoresLiberados()
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
}
