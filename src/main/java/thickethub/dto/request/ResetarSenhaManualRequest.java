package thickethub.dto.request;

import jakarta.validation.constraints.NotNull;

public record ResetarSenhaManualRequest(
        @NotNull Long usuarioId
) {}
