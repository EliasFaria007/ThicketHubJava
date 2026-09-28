package thickethub.dto.response;

import thickethub.domain.model.Comentario;

import java.time.LocalDateTime;

public record ComentarioResponse(
        Long id,
        Long chamadoId,
        Long autorId,
        String autorNome,
        String mensagem,
        Boolean flagInterno,
        LocalDateTime dataCriacao
) {
    public static ComentarioResponse de(Comentario c) {
        if (c == null) return null;
        return new ComentarioResponse(
                c.getId(),
                c.getChamado() != null ? c.getChamado().getId() : null,
                c.getAutor() != null ? c.getAutor().getId() : null,
                c.getAutor() != null ? c.getAutor().getNome() : null,
                c.getMensagem(),
                c.getFlagInterno(),
                c.getDataCriacao()
        );
    }
}
