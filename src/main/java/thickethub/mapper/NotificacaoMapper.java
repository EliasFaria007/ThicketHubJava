package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Notificacao;
import thickethub.dto.response.NotificacaoResponse;

@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "chamadoId", source = "chamado.id")
    NotificacaoResponse toResponse(Notificacao notificacao);
}
