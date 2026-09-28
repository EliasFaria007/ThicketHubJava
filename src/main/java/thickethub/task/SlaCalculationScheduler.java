package thickethub.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Chamado;
import thickethub.repository.ChamadoRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaCalculationScheduler {

    private final ChamadoRepository chamadoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Roda a cada 5 minutos monitorando chamados ativos para alerta de estouro de SLA.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void verificarSlaChamados() {
        try {
            List<StatusChamado> statusesAtivos = List.of(StatusChamado.ABERTO, StatusChamado.EM_ATENDIMENTO, StatusChamado.EM_ANDAMENTO);
            List<Chamado> chamadosAtivos = chamadoRepository.findChamadosComSlaAtivo(statusesAtivos);

            LocalDateTime agora = LocalDateTime.now();

            for (Chamado c : chamadosAtivos) {
                if (c.getPrazoLimite() != null && agora.isAfter(c.getPrazoLimite())) {
                    log.warn("SLA estourado para o chamado: protocolo={}, setor={}", c.getProtocolo(), c.getSetor());
                    messagingTemplate.convertAndSend("/topic/setor/" + c.getSetor(),
                            "ALERTA: Chamado " + c.getProtocolo() + " ultrapassou o prazo limite de SLA!");
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao processar verificação periódica de SLA: {}", e.getMessage());
        }
    }
}
