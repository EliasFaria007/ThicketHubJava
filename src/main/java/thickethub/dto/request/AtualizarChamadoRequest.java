package thickethub.dto.request;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;

public record AtualizarChamadoRequest(
        StatusChamado status,
        PrioridadeChamado prioridade,
        String oQueFoiFeito,
        Long versao
) {}