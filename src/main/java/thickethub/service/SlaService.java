package thickethub.service;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.dto.request.SlaConfigRequest;
import thickethub.dto.response.SlaConfigResponse;

import java.util.List;

public interface SlaService {
    List<SlaConfigResponse> listarTodos();
    SlaConfigResponse buscarPorPrioridade(PrioridadeChamado prioridade);
    SlaConfigResponse salvarOuAtualizar(SlaConfigRequest request);
}
