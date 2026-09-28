package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.UsuarioResponse;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "senhaTemporaria", ignore = true)
    UsuarioResponse toResponse(Usuario usuario);
}
