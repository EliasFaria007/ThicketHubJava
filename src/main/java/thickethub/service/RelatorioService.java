package thickethub.service;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.dto.response.RelatoriosResponse;

import java.time.LocalDate;

public interface RelatorioService {
    RelatoriosResponse gerarRelatorio(
            LocalDate de,
            LocalDate ate,
            String setor,
            StatusChamado status,
            PrioridadeChamado prioridade,
            Long tecnicoId,
            Long servicoId
    );
}
