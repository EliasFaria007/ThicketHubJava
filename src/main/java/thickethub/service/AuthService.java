package thickethub.service;

import thickethub.dto.request.*;
import thickethub.dto.response.AuthResponse;
import thickethub.dto.response.SolicitarSmsResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipOrigem);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String token);
    SolicitarSmsResponse solicitarRecuperacaoSms(SolicitarSmsRequest request);
    String validarSms(ValidarSmsRequest request);
    void redefinirSenha(RedefinirSenhaRequest request);
    AuthResponse completarPrimeiroAcesso(String email, CompletarCadastroRequest request);
    AuthResponse loginMicrosoft(MicrosoftLoginRequest request, String ipOrigem);
    thickethub.dto.response.UsuarioResponse aceitarTermos(Long usuarioId, AceitarTermosRequest request);
}
