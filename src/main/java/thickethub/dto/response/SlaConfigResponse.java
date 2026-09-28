package thickethub.dto.response;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.model.SlaConfig;

public record SlaConfigResponse(
        Long id,
        PrioridadeChamado prioridade,
        Integer slaAtendimentoHoras,
        Integer slaResolucaoHoras
) {
    public static SlaConfigResponse de(SlaConfig s) {
        if (s == null) return null;
        return new SlaConfigResponse(
                s.getId(),
                s.getPrioridade(),
                s.getSlaAtendimentoHoras(),
                s.getSlaResolucaoHoras()
        );
    }
}
