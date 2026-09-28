package thickethub.dto.response;

import thickethub.domain.enums.TipoHistorico;
import thickethub.domain.model.HistoricoChamado;

import java.time.LocalDateTime;

public record HistoricoResponse(
        Long id,
        Long chamadoId,
        TipoHistorico tipo,
        String descricao,
        String autorNome,
        LocalDateTime dataEvento
) {
    public static HistoricoResponse de(HistoricoChamado h) {
        if (h == null) return null;
        return new HistoricoResponse(
                h.getId(),
                h.getChamado() != null ? h.getChamado().getId() : null,
                h.getTipo(),
                h.getDescricao(),
                h.getAutorNome(),
                h.getDataEvento()
        );
    }
}
