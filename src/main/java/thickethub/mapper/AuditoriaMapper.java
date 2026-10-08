package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Auditoria;
import thickethub.dto.response.AuditoriaResponse;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    @Mapping(target = "ip", source = "ipOrigem")
    AuditoriaResponse toResponse(Auditoria auditoria);
}
