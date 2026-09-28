package thickethub.service;

import org.springframework.data.domain.Pageable;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.AuditoriaResponse;
import thickethub.dto.response.PaginacaoResponse;

import java.time.LocalDateTime;

public interface AuditoriaService {
    void registrar(Usuario usuario, String acao, String detalhe, String ipOrigem);
    PaginacaoResponse<AuditoriaResponse> listar(Usuario usuarioLogado, Long usuarioId, String acao, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
}
