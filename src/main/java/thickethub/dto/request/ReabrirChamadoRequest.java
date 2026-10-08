package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReabrirChamadoRequest(
        @Size(max = 500) String justificativa
) {}
