package thickethub.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import thickethub.dto.request.*;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.AuthResponse;
import thickethub.security.SecurityUtils;
import thickethub.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = SecurityUtils.extrairIp(httpRequest);
        AuthResponse response = authService.login(request, ip);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Login realizado com sucesso", 200));
    }

    @PostMapping("/microsoft")
    public ResponseEntity<ApiResponse<AuthResponse>> loginMicrosoft(@Valid @RequestBody MicrosoftLoginRequest request, HttpServletRequest httpRequest) {
        String ip = SecurityUtils.extrairIp(httpRequest);
        AuthResponse response = authService.loginMicrosoft(request, ip);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Login institucional Microsoft realizado com sucesso", 200));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Token renovado com sucesso", 200));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Logout realizado com sucesso", 200));
    }

    @PostMapping("/recuperacao-sms/solicitar")
    public ResponseEntity<ApiResponse<thickethub.dto.response.SolicitarSmsResponse>> solicitarSms(@Valid @RequestBody SolicitarSmsRequest request) {
        thickethub.dto.response.SolicitarSmsResponse response = authService.solicitarRecuperacaoSms(request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Se o usuário estiver cadastrado, o código será enviado.", 200));
    }

    @PostMapping("/recuperacao-sms/validar")
    public ResponseEntity<ApiResponse<String>> validarSms(@Valid @RequestBody ValidarSmsRequest request) {
        String ticket = authService.validarSms(request);
        return ResponseEntity.ok(ApiResponse.sucesso(ticket, "Código validado com sucesso.", 200));
    }

    @PostMapping("/recuperacao-sms/redefinir-senha")
    public ResponseEntity<ApiResponse<Void>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        authService.redefinirSenha(request);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Senha redefinida com sucesso.", 200));
    }

    @PostMapping("/primeiro-acesso")
    public ResponseEntity<ApiResponse<AuthResponse>> primeiroAcesso(@Valid @RequestBody CompletarCadastroRequest request) {
        String email = SecurityUtils.getEmailUsuarioLogado();
        AuthResponse response = authService.completarPrimeiroAcesso(email, request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Primeiro acesso concluído com sucesso.", 200));
    }

    @PostMapping("/termos")
    public ResponseEntity<ApiResponse<thickethub.dto.response.UsuarioResponse>> aceitarTermos(@RequestBody(required = false) AceitarTermosRequest request) {
        thickethub.domain.model.Usuario logado = SecurityUtils.getUsuarioLogado();
        thickethub.dto.response.UsuarioResponse response = authService.aceitarTermos(logado.getId(), request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Termos aceitos com sucesso.", 200));
    }
}
