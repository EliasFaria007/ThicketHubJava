package thickethub.dto.response;

import thickethub.domain.model.Auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        String acao,
        String detalhe,
        Long usuarioId,
        String usuarioNome,
        String ipOrigem,
        LocalDateTime criadoEm
) {
    public static AuditoriaResponse de(Auditoria a) {
        if (a == null) return null;
        return new AuditoriaResponse(
                a.getId(),
                a.getAcao(),
                a.getDetalhe(),
                a.getUsuarioId(),
                a.getUsuarioNome(),
                a.getIpOrigem(),
                a.getCriadoEm()
        );
    }
}
