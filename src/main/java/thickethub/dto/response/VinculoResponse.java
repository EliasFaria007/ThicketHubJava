package thickethub.dto.response;

import thickethub.domain.model.ChamadoVinculo;

public record VinculoResponse(
        Long id,
        Long chamadoOrigemId,
        String chamadoOrigemProtocolo,
        Long chamadoDestinoId,
        String chamadoDestinoProtocolo,
        String tipo
) {
    public static VinculoResponse de(ChamadoVinculo v) {
        if (v == null) return null;
        return new VinculoResponse(
                v.getId(),
                v.getChamadoOrigem() != null ? v.getChamadoOrigem().getId() : null,
                v.getChamadoOrigem() != null ? v.getChamadoOrigem().getProtocolo() : null,
                v.getChamadoDestino() != null ? v.getChamadoDestino().getId() : null,
                v.getChamadoDestino() != null ? v.getChamadoDestino().getProtocolo() : null,
                v.getTipo() != null ? v.getTipo().name() : null
        );
    }
}
