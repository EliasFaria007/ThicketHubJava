package thickethub.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import thickethub.service.SmsService;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Async("smsExecutor")
    @Override
    public void enviar(String telefone, String mensagem) {
        // Integração com gateway de SMS institucional
        log.info("[SMS GATEWAY] Enviando SMS para {}: {}", telefone, mensagem);
    }
}
