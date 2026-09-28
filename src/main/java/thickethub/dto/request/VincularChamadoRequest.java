package thickethub.dto.request;

import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.TipoVinculo;

public record VincularChamadoRequest(
        @NotNull Long chamadoDestinoId,
        @NotNull TipoVinculo tipo
) {}
