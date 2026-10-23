package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MicrosoftLoginRequest(
        @NotBlank(message = "O ID token ou e-mail da Microsoft é obrigatório")
        String idToken,
        String email,
        String nome
) {
}
