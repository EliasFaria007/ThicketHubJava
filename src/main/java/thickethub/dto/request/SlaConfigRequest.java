package thickethub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.PrioridadeChamado;

public record SlaConfigRequest(
        @NotNull PrioridadeChamado prioridade,
        @NotNull @Min(1) Integer slaAtendimentoHoras,
        @NotNull @Min(1) Integer slaResolucaoHoras
) {}
