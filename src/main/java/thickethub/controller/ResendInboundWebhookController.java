package thickethub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thickethub.domain.enums.Papel;
import thickethub.domain.model.Servico;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.NovoChamadoRequest;
import thickethub.repository.ServicoRepository;
import thickethub.repository.UsuarioRepository;
import thickethub.service.ChamadoService;
import thickethub.service.R2StorageService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Endpoint de Webhook para receber e-mails encaminhados pelo Resend Inbound.
 * Converte automaticamente e-mails recebidos em chamados (Mail-to-Ticket),
 * buscando a descrição completa (HTML/texto) e baixando as imagens e anexos.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/resend")
@RequiredArgsConstructor
public class ResendInboundWebhookController {

    private final ObjectMapper objectMapper;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final ChamadoService chamadoService;
    private final PasswordEncoder passwordEncoder;
    private final R2StorageService r2StorageService;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @PostMapping("/inbound")
    public ResponseEntity<Map<String, Object>> receberEmailInbound(@RequestBody String payload) {
        log.info("[RESEND WEBHOOK] Evento de webhook recebido");

        try {
            JsonNode root = objectMapper.readTree(payload);

            JsonNode dataNode = root.has("data") ? root.path("data") : root;
            String emailId = dataNode.path("email_id").asText("");
            if (emailId.isBlank()) {
                emailId = dataNode.path("id").asText("");
            }

            String from = dataNode.path("from").asText("");
            String subject = dataNode.path("subject").asText("");
            String text = dataNode.path("text").asText("");
            String html = dataNode.path("html").asText("");

            List<Map<String, Object>> anexosList = new ArrayList<>();
            StringBuilder imagensMarkdown = new StringBuilder();
            StringBuilder documentosMarkdown = new StringBuilder();

            // O webhook inicial do Resend envia apenas metadados (sem o corpo e anexos).
            // Consultamos a API Resend Receiving para obter o texto completo e os anexos.
            if (!emailId.isBlank() && mailPassword != null && mailPassword.trim().startsWith("re_")) {
                try {
                    // 1. Obter corpo completo do e-mail
                    HttpRequest emailReq = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.resend.com/emails/receiving/" + emailId))
                            .header("Authorization", "Bearer " + mailPassword.trim())
                            .header("User-Agent", "ThicketHub/1.0")
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build();

                    HttpResponse<String> emailResp = httpClient.send(emailReq, HttpResponse.BodyHandlers.ofString());
                    if (emailResp.statusCode() >= 200 && emailResp.statusCode() < 300) {
                        JsonNode emailJson = objectMapper.readTree(emailResp.body());
                        if (from.isBlank()) from = emailJson.path("from").asText(from);
                        if (subject.isBlank()) subject = emailJson.path("subject").asText(subject);
                        if (text.isBlank()) text = emailJson.path("text").asText("");
                        if (html.isBlank()) html = emailJson.path("html").asText("");
                    } else {
                        log.warn("[RESEND WEBHOOK] Resposta ao buscar e-mail {}: HTTP {}", emailId, emailResp.statusCode());
                    }

                    // 2. Obter lista de anexos e imagens
                    HttpRequest attReq = HttpRequest.newBuilder()
                            .uri(URI.create("https://api.resend.com/emails/receiving/" + emailId + "/attachments"))
                            .header("Authorization", "Bearer " + mailPassword.trim())
                            .header("User-Agent", "ThicketHub/1.0")
                            .timeout(Duration.ofSeconds(15))
                            .GET()
                            .build();

                    HttpResponse<String> attResp = httpClient.send(attReq, HttpResponse.BodyHandlers.ofString());
                    if (attResp.statusCode() >= 200 && attResp.statusCode() < 300) {
                        JsonNode attJson = objectMapper.readTree(attResp.body());
                        JsonNode dataArray = attJson.has("data") ? attJson.path("data") : attJson;

                        if (dataArray.isArray()) {
                            for (JsonNode attNode : dataArray) {
                                String filename = attNode.path("filename").asText("anexo");
                                String contentType = attNode.path("content_type").asText("application/octet-stream");
                                String downloadUrl = attNode.path("download_url").asText("");
                                long size = attNode.path("size").asLong(0);

                                if (!downloadUrl.isBlank()) {
                                    String finalUrl = downloadUrl;
                                    // Tenta baixar e persistir de forma permanente no Cloudflare R2
                                    try {
                                        HttpRequest dlReq = HttpRequest.newBuilder()
                                                .uri(URI.create(downloadUrl))
                                                .timeout(Duration.ofSeconds(30))
                                                .GET()
                                                .build();
                                        HttpResponse<byte[]> dlResp = httpClient.send(dlReq, HttpResponse.BodyHandlers.ofByteArray());
                                        if (dlResp.statusCode() == 200 && dlResp.body() != null && dlResp.body().length > 0) {
                                            String r2Url = r2StorageService.uploadBytes(dlResp.body(), filename, contentType, "anexos");
                                            if (r2Url != null && !r2Url.isBlank()) {
                                                finalUrl = r2Url;
                                            }
                                        }
                                    } catch (Exception ex) {
                                        log.warn("[RESEND WEBHOOK] Falha ao persistir anexo '{}' no R2: {}", filename, ex.getMessage());
                                    }

                                    Map<String, Object> anexoObj = new LinkedHashMap<>();
                                    anexoObj.put("id", UUID.randomUUID().toString());
                                    anexoObj.put("nome", filename);
                                    anexoObj.put("url", finalUrl);
                                    anexoObj.put("tipo", contentType);
                                    if (size > 0) anexoObj.put("tamanho", size);
                                    anexosList.add(anexoObj);

                                    if (contentType.startsWith("image/")) {
                                        imagensMarkdown.append("\n\n![").append(filename).append("](").append(finalUrl).append(")");
                                    } else {
                                        documentosMarkdown.append("\n• [").append(filename).append("](").append(finalUrl).append(")");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[RESEND WEBHOOK] Erro ao consultar detalhes do e-mail no Resend: {}", e.getMessage());
                }
            }

            // Normalização do texto da descrição
            String descricaoLimpa = text.trim();
            if (descricaoLimpa.isBlank() && !html.isBlank()) {
                descricaoLimpa = converterHtmlParaTexto(html);
            }
            if (descricaoLimpa.isBlank()) {
                descricaoLimpa = "Chamado aberto automaticamente a partir de e-mail recebido.";
            }

            // Montar descrição final com imagens, links e tag de anexos estruturada
            StringBuilder descricaoFinal = new StringBuilder(descricaoLimpa);
            if (!imagensMarkdown.isEmpty()) {
                descricaoFinal.append("\n\n---\n### 📷 Imagens do Chamado:").append(imagensMarkdown);
            }
            if (!documentosMarkdown.isEmpty()) {
                descricaoFinal.append("\n\n---\n### 📎 Arquivos e Evidências Vinculadas:").append(documentosMarkdown);
            }
            if (!anexosList.isEmpty()) {
                descricaoFinal.append("\n\n<!-- THICKET_ANEXOS:").append(objectMapper.writeValueAsString(anexosList)).append(" -->");
            }

            // Extrair remetente limpo
            String cleanEmail = from;
            if (from.contains("<") && from.contains(">")) {
                cleanEmail = from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
            }
            if (cleanEmail.isBlank()) {
                cleanEmail = "usuario.externo@thickethub.dev.br";
            }
            String finalCleanEmail = cleanEmail.toLowerCase().trim();

            // Localizar ou cadastrar o usuário solicitante
            String nomeExtraido = (from.contains("<") && from.contains(">")) 
                    ? from.substring(0, from.indexOf("<")).trim() 
                    : (finalCleanEmail.contains("@") ? "Solicitante " + finalCleanEmail.split("@")[0] : finalCleanEmail);
            if (nomeExtraido.isBlank()) {
                nomeExtraido = "Solicitante";
            }
            final String nomeFinal = nomeExtraido;
            final String emailFinal = finalCleanEmail;

            Usuario solicitante = usuarioRepository.findByEmail(emailFinal)
                    .orElseGet(() -> {
                        Usuario novo = Usuario.builder()
                                .nome(nomeFinal)
                                .email(emailFinal)
                                .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .papel(Papel.USUARIO)
                                .setor("Atendimento Geral")
                                .ativo(true)
                                .build();
                        return usuarioRepository.save(novo);
                    });

            // Localizar serviço padrão ativo do catálogo
            Long servicoId = servicoRepository.findAll().stream()
                    .filter(Servico::isAtivo)
                    .map(Servico::getId)
                    .findFirst()
                    .orElse(1L);

            String tituloChamado = subject.isBlank() ? "Chamado via E-mail" : subject;
            if (tituloChamado.length() > 190) {
                tituloChamado = tituloChamado.substring(0, 190);
            }

            NovoChamadoRequest request = new NovoChamadoRequest(tituloChamado, descricaoFinal.toString(), servicoId);
            var chamadoCriado = chamadoService.criarChamado(solicitante, request);

            log.info("[RESEND WEBHOOK] SUCESSO: Chamado '{}' criado com {} anexos a partir de e-mail de '{}'",
                    chamadoCriado.protocolo(), anexosList.size(), finalCleanEmail);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "protocolo", chamadoCriado.protocolo(),
                    "email", finalCleanEmail,
                    "anexosProcessados", anexosList.size()
            ));

        } catch (Exception e) {
            log.error("[RESEND WEBHOOK] Falha ao processar webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String converterHtmlParaTexto(String html) {
        if (html == null || html.isBlank()) return "";
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                   .replaceAll("(?i)</p>", "\n\n")
                   .replaceAll("<[^>]+>", " ")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .replaceAll("\\n{3,}", "\n\n")
                   .trim();
    }
}
