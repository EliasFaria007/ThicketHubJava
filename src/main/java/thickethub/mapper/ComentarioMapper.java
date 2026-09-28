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
    ComentarioResponse toResponse(Comentario comentario);
}
