package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoComentarioRequest(
        @NotBlank @Size(min = 1, max = 2000) String mensagem,
        Boolean flagInterno
) {}
