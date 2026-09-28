package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;

public record AtualizarChamadoRequest(
        @NotNull StatusChamado status,
        @NotNull PrioridadeChamado prioridade,
        @NotBlank @Size(min = 10, max = 200) String oQueFoiFeito,
        @NotNull Long versao) {}