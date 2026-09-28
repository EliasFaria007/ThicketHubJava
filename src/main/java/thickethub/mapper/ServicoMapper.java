package thickethub.mapper;

import org.mapstruct.Mapper;
import thickethub.domain.model.Servico;
import thickethub.dto.response.ServicoResponse;

@Mapper(componentModel = "spring")
public interface ServicoMapper {
    ServicoResponse toResponse(Servico servico);
}
