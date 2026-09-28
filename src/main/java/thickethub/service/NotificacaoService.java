package thickethub.service;

import org.springframework.data.domain.Pageable;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.NotificacaoResponse;
import thickethub.dto.response.NotificacoesListResponse;
import thickethub.dto.response.PaginacaoResponse;

public interface NotificacaoService {
    void enviar(Usuario destinatario, Chamado chamado, String tipo, String mensagem);
    NotificacoesListResponse listarNaoLidas(Long usuarioId);
    PaginacaoResponse<NotificacaoResponse> listarTodas(Long usuarioId, Pageable pageable);
    void marcarComoLida(Long usuarioId, Long notificacaoId);
    void marcarTodasComoLidas(Long usuarioId);
}
