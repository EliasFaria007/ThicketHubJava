package thickethub.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.enums.TipoHistorico;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.HistoricoChamado;
import thickethub.repository.ChamadoRepository;
import thickethub.repository.HistoricoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FechamentoInatividadeScheduler {

    private final ChamadoRepository chamadoRepository;
    private final HistoricoRepository historicoRepository;

    /**
     * Roda a cada 1 hora verificando chamados resolvidos que ultrapassaram o tempo limite de inatividade.
     * IMPORTANTE: Conforme regra institucional estrita, NÃO altera chamados no status AGUARDANDO.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void fecharChamadosResolvidosPorInatividade() {
        try {
            boolean ativo = Boolean.TRUE.equals(thickethub.controller.ConfiguracaoController.GERAL_CONFIG.get("fechamentoInatividadeAtivo"));
            if (!ativo) return;

            Object diasObj = thickethub.controller.ConfiguracaoController.GERAL_CONFIG.getOrDefault("diasInatividadeFechamento", 5);
            int dias = diasObj instanceof Number ? ((Number) diasObj).intValue() : 5;
            if (dias <= 0) dias = 5;

            LocalDateTime limite = LocalDateTime.now().minusDays(dias);

            // Busca apenas chamados com status RESOLVIDO (nunca AGUARDANDO)
            List<Chamado> resolvidos = chamadoRepository.findByStatusIn(List.of(StatusChamado.RESOLVIDO));

            for (Chamado c : resolvidos) {
                // Checa a data em que foi resolvido
                LocalDateTime dtResolvido = c.getResolvidoEm() != null ? c.getResolvidoEm() : c.getDataAbertura();
                if (dtResolvido != null && dtResolvido.isBefore(limite)) {
                    log.info("[FECHAMENTO AUTOMÁTICO] Fechando chamado {} por inatividade após {} dias de resolução", c.getProtocolo(), dias);
                    c.setStatus(StatusChamado.FECHADO);
                    c.setDataFechamento(LocalDateTime.now());
                    chamadoRepository.save(c);

                    HistoricoChamado h = HistoricoChamado.builder()
                            .chamado(c)
                            .autor(null)
                            .autorNome("Sistema Institucional")
                            .tipo(TipoHistorico.STATUS)
                            .descricao("Chamado homologado e fechado automaticamente após " + dias + " dias de resolução sem contestação.")
                            .dataEvento(LocalDateTime.now())
                            .build();
                    historicoRepository.save(h);
                }
            }
        } catch (Exception e) {
            log.warn("[FECHAMENTO AUTOMÁTICO] Erro ao processar fechamento por inatividade: {}", e.getMessage());
        }
    }
}
