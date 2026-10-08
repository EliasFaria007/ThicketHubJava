package thickethub.dto.response;

import thickethub.domain.model.Auditoria;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        String acao,
        String detalhe,
        Long usuarioId,
        String usuarioNome,
        String ip,
        String ipOrigem,
        LocalDateTime criadoEm
) {
    public static AuditoriaResponse de(Auditoria a) {
        if (a == null) return null;
        String ipReal = a.getIpOrigem() != null && !a.getIpOrigem().isBlank() ? a.getIpOrigem() : "127.0.0.1";
        if ("0:0:0:0:0:0:0:1".equals(ipReal) || "::1".equals(ipReal)) {
            ipReal = "127.0.0.1";
        }
        return new AuditoriaResponse(
                a.getId(),
                a.getAcao(),
                a.getDetalhe(),
                a.getUsuarioId(),
                a.getUsuarioNome(),
                ipReal,
                ipReal,
                a.getCriadoEm()
        );
    }
}
