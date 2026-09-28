package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Chamado;
import thickethub.dto.response.ChamadoDetalheResponse;
import thickethub.dto.response.ChamadoResumoResponse;

@Mapper(componentModel = "spring")
public interface ChamadoMapper {

    @Mapping(target = "servicoId", source = "servico.id")
    @Mapping(target = "servicoNome", source = "servico.nome")
    @Mapping(target = "solicitanteId", source = "solicitante.id")
    @Mapping(target = "solicitanteNome", source = "solicitante.nome")
    @Mapping(target = "solicitanteEmail", source = "solicitante.email")
    @Mapping(target = "tecnicoId", source = "tecnico.id")
    @Mapping(target = "tecnicoNome", source = "tecnico.nome")
    ChamadoDetalheResponse toDetalheResponse(Chamado chamado);

    @Mapping(target = "solicitanteNome", source = "solicitante.nome")
    @Mapping(target = "tecnicoNome", source = "tecnico.nome")
    ChamadoResumoResponse toResumoResponse(Chamado chamado);
}
