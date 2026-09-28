package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.Notificacao;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.NotificacaoResponse;
import thickethub.dto.response.NotificacoesListResponse;
import thickethub.dto.response.PaginacaoResponse;
import thickethub.exception.ForbiddenException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.NotificacaoRepository;
import thickethub.service.NotificacaoService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoServiceImpl implements NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void enviar(Usuario destinatario, Chamado chamado, String tipo, String mensagem) {
        if (destinatario == null) return;
        try {
            Notificacao n = Notificacao.builder()
                    .usuario(destinatario)
                    .chamado(chamado)
                    .tipo(tipo != null ? tipo : "SISTEMA")
                    .mensagem(mensagem)
                    .lida(false)
                    .criadoEm(LocalDateTime.now())
                    .build();

            notificacaoRepository.save(n);
            messagingTemplate.convertAndSendToUser(destinatario.getEmail(), "/queue/notificacoes", NotificacaoResponse.de(n));
        } catch (Exception e) {
            log.warn("Falha ao salvar/enviar notificação: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacoesListResponse listarNaoLidas(Long usuarioId) {
        List<Notificacao> lista = notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(usuarioId);
        long totalNaoLidas = lista.size();
        List<NotificacaoResponse> dtos = lista.stream().map(NotificacaoResponse::de).toList();
        return new NotificacoesListResponse(dtos, totalNaoLidas);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacaoResponse<NotificacaoResponse> listarTodas(Long usuarioId, Pageable pageable) {
        Page<Notificacao> page = notificacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId, pageable);
        return PaginacaoResponse.de(page.map(NotificacaoResponse::de));
    }

    @Override
    @Transactional
    public void marcarComoLida(Long usuarioId, Long notificacaoId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));

        if (!n.getUsuarioId().equals(usuarioId)) {
            throw new ForbiddenException("Esta notificação não pertence a você.");
        }

        n.setLida(true);
        notificacaoRepository.save(n);
    }

    @Override
    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        List<Notificacao> naoLidas = notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(usuarioId);
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
    }
}
