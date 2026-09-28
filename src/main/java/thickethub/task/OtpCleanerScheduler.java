package thickethub.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import thickethub.repository.RecuperacaoSmsRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanerScheduler {

    private final RecuperacaoSmsRepository recuperacaoSmsRepository;

    /**
     * Roda a cada 30 minutos limpando OTPs de SMS expirados ou já utilizados.
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void limparOtpsExpirados() {
        try {
            LocalDateTime limite = LocalDateTime.now();
            recuperacaoSmsRepository.deleteExpiradosOuUtilizados(limite);
            log.debug("Limpeza periódica de OTPs de recuperação SMS executada com sucesso.");
        } catch (Exception e) {
            log.warn("Erro ao limpar OTPs expirados: {}", e.getMessage());
        }
    }
}
