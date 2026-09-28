package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReabrirChamadoRequest(
        @NotBlank @Size(min = 10, max = 500) String justificativa
) {}
