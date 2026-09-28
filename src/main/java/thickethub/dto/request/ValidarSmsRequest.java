package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarSmsRequest(
        @NotBlank String telefone,
        @NotBlank @Size(min = 4, max = 10) String codigo
) {}
