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
    public ResponseEntity<ApiResponse<Void>> solicitarSms(@Valid @RequestBody SolicitarSmsRequest request) {
        authService.solicitarRecuperacaoSms(request);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Se o telefone estiver cadastrado, o código será enviado.", 200));
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
}
