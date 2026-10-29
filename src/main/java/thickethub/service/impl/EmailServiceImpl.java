package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import thickethub.service.EmailService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    /** Endereço remetente configurado via variável de ambiente MAIL_FROM */
    @Value("${spring.mail.from:no-reply@thickethub.dev.br}")
    private String remetente;

    /** Senha ou API Key configurada via MAIL_PASSWORD */
    @Value("${spring.mail.password:}")
    private String mailPassword;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Async("taskExecutor")
    @Override
    public void enviarEmail(String destinatario, String assunto, String corpo) {
        executarEnvio(destinatario, assunto, corpo);
    }

    @Override
    public boolean enviarEmailSincrono(String destinatario, String assunto, String corpo) {
        return executarEnvio(destinatario, assunto, corpo);
    }

    private boolean executarEnvio(String destinatario, String assunto, String corpo) {
        if (destinatario == null || destinatario.isBlank()) return false;

        // Se a chave do Resend (iniciada por "re_") estiver configurada, prioriza a API REST oficial do Resend
        // (Opera sobre HTTPS porta 443, sendo imune a restrições/bloqueios de portas SMTP 587/465 em clouds)
        if (mailPassword != null && mailPassword.trim().startsWith("re_")) {
            boolean enviadoApi = enviarViaResendApi(destinatario, assunto, corpo);
            if (enviadoApi) return true;
        }

        // Fallback para envio padrão via JavaMailSender (SMTP)
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(remetente);
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(corpo);
            mailSender.send(message);
            log.info("[E-MAIL SMTP] Enviado com sucesso de '{}' para '{}' (Assunto: {})", remetente, destinatario, assunto);
            return true;
        } catch (Exception e) {
            log.warn("[E-MAIL SMTP] Falha ao enviar para {}: {}", destinatario, e.getMessage());
            return false;
        }
    }

    private boolean enviarViaResendApi(String destinatario, String assunto, String corpo) {
        try {
            String fromFormatado = remetente != null && remetente.contains("<") 
                    ? remetente 
                    : "ThicketHub <" + remetente + ">";

            String json = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"text\":\"%s\"}",
                    escapeJson(fromFormatado),
                    escapeJson(destinatario),
                    escapeJson(assunto),
                    escapeJson(corpo)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + mailPassword.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[E-MAIL RESEND API] Enviado com sucesso para '{}' (Status: {}, Resposta: {})",
                        destinatario, response.statusCode(), response.body());
                return true;
            } else {
                log.warn("[E-MAIL RESEND API] Erro ao enviar para {}: HTTP {} - {}",
                        destinatario, response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("[E-MAIL RESEND API] Exceção ao enviar via Resend API: {}", e.getMessage());
            return false;
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
