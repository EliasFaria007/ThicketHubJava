package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank String ticketValidacao,
        @NotBlank @Size(min = 6, max = 100) String novaSenha
) {}
