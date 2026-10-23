package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Comentario;
import thickethub.dto.response.ComentarioResponse;

@Mapper(componentModel = "spring")
public interface ComentarioMapper {

    @Mapping(target = "chamadoId", source = "chamado.id")
    @Mapping(target = "autorId", source = "autor.id")
    @Mapping(target = "autorNome", source = "autor.nome")
    @Mapping(target = "autorPapel", expression = "java(comentario.getAutor() != null && comentario.getAutor().getPapel() != null ? comentario.getAutor().getPapel().name() : null)")
    @Mapping(target = "autorEmail", source = "autor.email")
    ComentarioResponse toResponse(Comentario comentario);
}
