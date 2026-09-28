package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.PrioridadeChamado;

public record ServicoRequest(
        @NotBlank String nome,
        @NotBlank String setor,
        @NotNull PrioridadeChamado prioridade,
        Integer slaAtendimentoHoras,
        Integer slaResolucaoHoras,
        Boolean ativo
) {}
