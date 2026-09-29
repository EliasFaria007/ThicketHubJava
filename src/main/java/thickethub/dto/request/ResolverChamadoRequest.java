package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverChamadoRequest(
        @NotBlank(message = "O descritivo do que foi feito é obrigatório")
        @Size(min = 10, max = 2000, message = "O descritivo do que foi feito deve ter entre 10 e 2000 caracteres")
        String oQueFoiFeito
) {}
