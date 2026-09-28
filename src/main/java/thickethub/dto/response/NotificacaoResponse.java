package thickethub.dto.response;

import thickethub.domain.model.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
        Long id,
        Long usuarioId,
        Long chamadoId,
        String tipo,
        Boolean lida,
        String mensagem,
        LocalDateTime criadoEm
) {
    public static NotificacaoResponse de(Notificacao n) {
        if (n == null) return null;
        return new NotificacaoResponse(
                n.getId(),
                n.getUsuarioId(),
                n.getChamadoId(),
                n.getTipo(),
                n.getLida(),
                n.getMensagem(),
                n.getCriadoEm()
        );
    }
}
