package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfigDominiosRequest(
        @NotBlank String dominio,
        Boolean ehPadrao,
        Boolean ativo
) {}
