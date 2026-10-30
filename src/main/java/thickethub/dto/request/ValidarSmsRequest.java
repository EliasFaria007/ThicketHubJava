package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarSmsRequest(
        String telefone,
        String email,
        @NotBlank @Size(min = 4, max = 10) String codigo
) {}
