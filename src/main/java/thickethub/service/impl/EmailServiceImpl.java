package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import thickethub.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    /** Endereço remetente configurado via variável de ambiente MAIL_FROM */
    @Value("${spring.mail.from:no-reply@thickethub.dev.br}")
    private String remetente;

    @Async("taskExecutor")
    @Override
    public void enviarEmail(String destinatario, String assunto, String corpo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(corpo);
            mailSender.send(message);
            log.info("E-mail enviado com sucesso de '{}' para '{}'", remetente, destinatario);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
        }
    }
}
