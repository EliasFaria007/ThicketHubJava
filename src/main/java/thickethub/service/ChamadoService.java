package thickethub.service;

import org.springframework.data.domain.Pageable;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.*;

import java.util.List;

public interface ChamadoService {
    ChamadoDetalheResponse criarChamado(Usuario solicitante, NovoChamadoRequest request);
    ChamadoDetalheResponse buscar(Long id);
    ChamadoDetalheResponse buscarPorProtocolo(String protocolo);
    PaginacaoResponse<ChamadoResumoResponse> listar(Usuario usuarioLogado, StatusChamado status, PrioridadeChamado prioridade, Long servicoId, String setor, String fila, String busca, Pageable pageable);
    ChamadoDetalheResponse atualizar(Long id, AtualizarChamadoRequest request);
    ChamadoDetalheResponse assumirChamado(Usuario tecnico, Long chamadoId);
    ChamadoDetalheResponse pausarChamado(Usuario user, Long chamadoId);
    ChamadoDetalheResponse retomarChamado(Usuario user, Long chamadoId);
    ChamadoDetalheResponse resolverChamado(Usuario user, Long chamadoId, ResolverChamadoRequest request);
    ChamadoDetalheResponse devolverChamado(Usuario user, Long chamadoId, DevolverChamadoRequest request);
    ChamadoDetalheResponse encaminhar(Long id, EncaminharChamadoRequest request);
    ChamadoDetalheResponse reabrirChamado(Usuario user, Long chamadoId, String justificativa);
    ChamadoDetalheResponse avaliarChamado(Usuario solicitante, Long chamadoId, AvaliarChamadoRequest request);
    ComentarioResponse adicionarComentario(Usuario autor, Long chamadoId, NovoComentarioRequest request);
    List<ComentarioResponse> listarComentarios(Usuario usuario, Long chamadoId);
    void vincularChamado(Usuario usuario, Long chamadoOrigemId, VincularChamadoRequest request);
    MetricasResponse obterMetricas(Usuario usuarioLogado);
}
