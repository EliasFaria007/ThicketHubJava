package thickethub.dto.response;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Chamado;

import java.time.LocalDateTime;

public record ChamadoResumoResponse(
        Long id,
        String protocolo,
        String titulo,
        StatusChamado status,
        PrioridadeChamado prioridade,
        String setor,
        String solicitanteNome,
        String tecnicoNome,
        LocalDateTime dataAbertura,
        LocalDateTime prazoLimite
) {
    public static ChamadoResumoResponse de(Chamado chamado) {
        if (chamado == null) return null;
        return new ChamadoResumoResponse(
                chamado.getId(),
                chamado.getProtocolo(),
                chamado.getTitulo(),
                chamado.getStatus(),
                chamado.getPrioridade(),
                chamado.getSetor(),
                chamado.getSolicitante() != null ? chamado.getSolicitante().getNome() : null,
                chamado.getTecnico() != null ? chamado.getTecnico().getNome() : null,
                chamado.getDataAbertura(),
                chamado.getPrazoLimite()
        );
    }
}
