package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverChamadoRequest(
        @NotBlank(message = "O descritivo do que foi feito é obrigatório")
        @Size(min = 10, message = "O descritivo do que foi feito deve ter no mínimo 10 caracteres")
        String oQueFoiFeito
) {}
