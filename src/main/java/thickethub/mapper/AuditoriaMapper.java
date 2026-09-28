package thickethub.mapper;

import org.mapstruct.Mapper;
import thickethub.domain.model.Auditoria;
import thickethub.dto.response.AuditoriaResponse;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    AuditoriaResponse toResponse(Auditoria auditoria);
}
