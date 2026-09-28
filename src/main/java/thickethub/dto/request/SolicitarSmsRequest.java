package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SolicitarSmsRequest(
        @NotBlank String telefone
) {}
