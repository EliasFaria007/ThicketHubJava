package thickethub.service;

import thickethub.dto.request.ServicoRequest;
import thickethub.dto.response.ServicoResponse;

import java.util.List;

public interface ServicoService {
    ServicoResponse criar(ServicoRequest request);
    ServicoResponse buscarPorId(Long id);
    List<ServicoResponse> listarTodos(Boolean apenasAtivos);
    List<ServicoResponse> listarPorSetor(String setor);
    ServicoResponse atualizar(Long id, ServicoRequest request);
    void desativar(Long id);
    void ativar(Long id);
}
