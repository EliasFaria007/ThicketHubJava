package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoComentarioRequest(
        @NotBlank String mensagem,
        Boolean flagInterno
) {}
