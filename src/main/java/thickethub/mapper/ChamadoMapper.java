package thickethub.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.Comentario;
import thickethub.domain.model.HistoricoChamado;
import thickethub.dto.response.ChamadoDetalheResponse;
import thickethub.dto.response.ChamadoResumoResponse;
import thickethub.dto.response.ComentarioResponse;
import thickethub.dto.response.HistoricoResponse;

@Mapper(componentModel = "spring")
public interface ChamadoMapper {

    @Mapping(target = "servicoId", source = "servico.id")
    @Mapping(target = "servicoNome", source = "servico.nome")
    @Mapping(target = "solicitanteId", source = "solicitante.id")
    @Mapping(target = "solicitanteNome", source = "solicitante.nome")
    @Mapping(target = "solicitanteEmail", source = "solicitante.email")
    @Mapping(target = "tecnicoId", source = "tecnico.id")
    @Mapping(target = "tecnicoNome", source = "tecnico.nome")
    @Mapping(target = "oQueFoiFeito", source = "OQueFoiFeito")
    ChamadoDetalheResponse toDetalheResponse(Chamado chamado);

    @Mapping(target = "solicitanteNome", source = "solicitante.nome")
    @Mapping(target = "tecnicoNome", source = "tecnico.nome")
    ChamadoResumoResponse toResumoResponse(Chamado chamado);

    @Mapping(target = "chamadoId", source = "chamado.id")
    @Mapping(target = "autorId", source = "autor.id")
    @Mapping(target = "autorNome", source = "autor.nome")
    ComentarioResponse toComentarioResponse(Comentario comentario);

    @Mapping(target = "chamadoId", source = "chamado.id")
    HistoricoResponse toHistoricoResponse(HistoricoChamado historico);
}
