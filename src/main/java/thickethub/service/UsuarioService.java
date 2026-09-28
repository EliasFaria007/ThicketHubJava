package thickethub.service;

import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.ImportarLoteResponse;
import thickethub.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioService {
    UsuarioResponse criarUsuario(Usuario adminLogado, CriarUsuarioRequest request);
    ImportarLoteResponse importarEmLote(Usuario adminLogado, ImportarUsuariosLoteRequest request);
    UsuarioResponse buscarPorId(Long id);
    List<UsuarioResponse> listarTodos();
    UsuarioResponse atualizar(Usuario logado, Long id, AtualizarUsuarioRequest request);
    void desativar(Usuario adminLogado, Long id);
    void ativar(Usuario adminLogado, Long id);
    UsuarioResponse resetarSenhaManual(Usuario solicitante, Long usuarioId);
    void alterarSenha(Usuario logado, AlterarSenhaRequest request);
}
