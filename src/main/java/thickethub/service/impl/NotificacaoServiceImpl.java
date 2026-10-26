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
            NotificacaoResponse responseDto = NotificacaoResponse.de(n);
            messagingTemplate.convertAndSendToUser(destinatario.getEmail(), "/queue/notificacoes", responseDto);
            messagingTemplate.convertAndSend("/topic/chamados", responseDto);
        } catch (Exception e) {
            log.warn("Falha ao salvar/enviar notificação: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacoesListResponse listarNaoLidas(Long usuarioId) {
        long totalNaoLidas = notificacaoRepository.countByUsuario_IdAndLidaFalse(usuarioId);
        List<Notificacao> recentes = notificacaoRepository.findTop20ByUsuario_IdOrderByCriadoEmDesc(usuarioId);
        List<NotificacaoResponse> dtos = recentes.stream().map(NotificacaoResponse::de).toList();
        return new NotificacoesListResponse(dtos, totalNaoLidas);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacaoResponse<NotificacaoResponse> listarTodas(Long usuarioId, Pageable pageable) {
        Page<Notificacao> page = notificacaoRepository.findByUsuario_IdOrderByCriadoEmDesc(usuarioId, pageable);
        return PaginacaoResponse.de(page.map(NotificacaoResponse::de));
    }

    @Override
    @Transactional
    public void marcarComoLida(Long usuarioId, Long notificacaoId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada."));

        if (!java.util.Objects.equals(n.getUsuarioId(), usuarioId)) {
            throw new ForbiddenException("Esta notificação não pertence a você.");
        }

        n.setLida(true);
        notificacaoRepository.save(n);
    }

    @Override
    @Transactional
    public void marcarPorChamado(Long usuarioId, Long chamadoId) {
        if (chamadoId == null || usuarioId == null) return;
        List<Notificacao> naoLidas = notificacaoRepository.findByUsuario_IdAndChamado_IdAndLidaFalse(usuarioId, chamadoId);
        if (!naoLidas.isEmpty()) {
            naoLidas.forEach(n -> n.setLida(true));
            notificacaoRepository.saveAll(naoLidas);
        }
    }

    @Override
    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        List<Notificacao> naoLidas = notificacaoRepository.findByUsuario_IdAndLidaFalseOrderByCriadoEmDesc(usuarioId);
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
    }
}
