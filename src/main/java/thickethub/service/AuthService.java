package thickethub.service;

import thickethub.dto.request.*;
import thickethub.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipOrigem);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String token);
    void solicitarRecuperacaoSms(SolicitarSmsRequest request);
    String validarSms(ValidarSmsRequest request);
    void redefinirSenha(RedefinirSenhaRequest request);
    AuthResponse completarPrimeiroAcesso(String email, CompletarCadastroRequest request);
}
