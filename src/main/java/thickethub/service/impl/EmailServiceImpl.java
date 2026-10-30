package thickethub.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import thickethub.service.EmailService;
import thickethub.service.EmailTemplateBuilder;

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
        String html = EmailTemplateBuilder.gerarHtmlGenerico(assunto, corpo, "Acessar Plataforma", "https://www.thickethub.dev.br");
        executarEnvio(destinatario, assunto, html, corpo);
    }

    @Async("taskExecutor")
    @Override
    public void enviarEmailHtml(String destinatario, String assunto, String html, String textoFallback) {
        executarEnvio(destinatario, assunto, html, textoFallback != null ? textoFallback : assunto);
    }

    @Async("taskExecutor")
    @Override
    public void enviarNotificacaoChamado(
            String destinatario,
            String nomeDestinatario,
            String protocolo,
            String titulo,
            String status,
            String prioridade,
            String setor,
            String tecnicoNome,
            Long chamadoId,
            String mensagemDestaque
    ) {
        if (destinatario == null || destinatario.isBlank()) return;

        String assunto = "[ThicketHub] #" + protocolo + " - " + mensagemDestaque;
        String html = EmailTemplateBuilder.gerarHtmlNotificacaoChamado(
                nomeDestinatario,
                protocolo,
                titulo,
                status,
                prioridade,
                setor,
                tecnicoNome,
                chamadoId,
                mensagemDestaque
        );

        String textoFallback = String.format(
                "Olá, %s!\n\n%s\n\nProtocolo: %s\nTítulo: %s\nStatus: %s\nPrioridade: %s\nSetor: %s\n\nAcompanhe em: https://www.thickethub.dev.br/chamados/%s\n\nThicketHub",
                nomeDestinatario != null ? nomeDestinatario : "Usuário",
                mensagemDestaque,
                protocolo,
                titulo,
                status,
                prioridade,
                setor,
                chamadoId != null ? chamadoId : ""
        );

        executarEnvio(destinatario, assunto, html, textoFallback);
    }

    @Override
    public boolean enviarEmailSincrono(String destinatario, String assunto, String corpo) {
        String html = EmailTemplateBuilder.gerarHtmlGenerico(assunto, corpo, "Acessar Plataforma", "https://www.thickethub.dev.br");
        return executarEnvio(destinatario, assunto, html, corpo);
    }

    private boolean executarEnvio(String destinatario, String assunto, String html, String texto) {
        if (destinatario == null || destinatario.isBlank()) return false;

        // Se a chave do Resend (iniciada por "re_") estiver configurada, prioriza a API REST oficial do Resend
        if (mailPassword != null && mailPassword.trim().startsWith("re_")) {
            boolean enviadoApi = enviarViaResendApi(destinatario, assunto, html, texto);
            if (enviadoApi) return true;
        }

        // Fallback para envio padrão via JavaMailSender (SMTP) com suporte completo a HTML corporativo
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(texto, html); // texto puro + versão HTML rica
            mailSender.send(mimeMessage);
            log.info("[E-MAIL SMTP] Enviado com sucesso de '{}' para '{}' (Assunto: {})", remetente, destinatario, assunto);
            return true;
        } catch (Exception e) {
            log.warn("[E-MAIL SMTP] Falha ao enviar para {}: {}", destinatario, e.getMessage());
            return false;
        }
    }

    private boolean enviarViaResendApi(String destinatario, String assunto, String html, String texto) {
        try {
            String fromFormatado = remetente != null && remetente.contains("<") 
                    ? remetente 
                    : "ThicketHub <" + remetente + ">";

            String json = String.format(
                    "{\"from\":\"%s\",\"to\":[\"%s\"],\"subject\":\"%s\",\"html\":\"%s\",\"text\":\"%s\"}",
                    escapeJson(fromFormatado),
                    escapeJson(destinatario),
                    escapeJson(assunto),
                    escapeJson(html),
                    escapeJson(texto)
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

    @Async("emailExecutor")
    @Override
    public void enviarCodigoRecuperacao(
            String destinatario,
            String nomeDestinatario,
            String codigoOtp,
            int minutosValidade
    ) {
        String assunto = "Código de Recuperação: " + codigoOtp + " — ThicketHub";
        String html = EmailTemplateBuilder.gerarHtmlCodigoRecuperacao(nomeDestinatario, codigoOtp, minutosValidade);
        String texto = "Olá " + (nomeDestinatario != null ? nomeDestinatario : "") + ",\n\n"
                + "Seu código de recuperação ThicketHub é: " + codigoOtp + "\n"
                + "Ele expira em " + minutosValidade + " minutos.\n\n"
                + "Acesse: https://www.thickethub.dev.br/recuperar-senha";
        enviarEmailHtml(destinatario, assunto, html, texto);
    }

}
