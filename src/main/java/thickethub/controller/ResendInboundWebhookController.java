package thickethub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;
import java.util.UUID;

/**
 * Endpoint de Webhook para receber e-mails encaminhados pelo Resend Inbound.
 * Converte automaticamente e-mails recebidos em chamados (Mail-to-Ticket).
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

    @PostMapping("/inbound")
    public ResponseEntity<Map<String, Object>> receberEmailInbound(@RequestBody String payload) {
        log.info("[RESEND WEBHOOK] Evento de webhook recebido");

        try {
            JsonNode root = objectMapper.readTree(payload);
            String tipo = root.path("type").asText("");

            // O Resend Inbound pode disparar "email.received"
            JsonNode dataNode = root.path("data");
            String from = dataNode.path("from").asText("");
            String subject = dataNode.path("subject").asText("Chamado via E-mail");
            String text = dataNode.path("text").asText("");
            if (text.isBlank()) {
                text = dataNode.path("html").asText("Chamado aberto automaticamente por e-mail.");
            }

            // Extrair e-mail limpo de formatos como "Nome Sobrenome <email@exemplo.com>"
            String cleanEmail = from;
            if (from.contains("<") && from.contains(">")) {
                cleanEmail = from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
            }

            if (cleanEmail.isBlank()) {
                log.warn("[RESEND WEBHOOK] E-mail de remetente ausente no payload");
                return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing_from"));
            }

            // Localizar ou registrar o usuário solicitante
            String finalCleanEmail = cleanEmail.toLowerCase().trim();
            Usuario solicitante = usuarioRepository.findByEmail(finalCleanEmail)
                    .orElseGet(() -> {
                        String nome = from.contains("<") ? from.substring(0, from.indexOf("<")).trim() : finalCleanEmail;
                        if (nome.isBlank()) nome = finalCleanEmail;
                        Usuario novo = Usuario.builder()
                                .nome(nome)
                                .email(finalCleanEmail)
                                .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .papel(Papel.USUARIO)
                                .setor("Externo")
                                .ativo(true)
                                .build();
                        return usuarioRepository.save(novo);
                    });

            // Localizar primeiro serviço ativo do catálogo como padrão
            Long servicoId = servicoRepository.findAll().stream()
                    .filter(Servico::isAtivo)
                    .map(Servico::getId)
                    .findFirst()
                    .orElse(1L);

            String tituloChamado = subject.length() > 190 ? subject.substring(0, 190) : subject;
            NovoChamadoRequest request = new NovoChamadoRequest(tituloChamado, text, servicoId);

            var chamadoCriado = chamadoService.criarChamado(solicitante, request);
            log.info("[RESEND WEBHOOK] Chamado '{}' criado via e-mail de '{}'", chamadoCriado.protocolo(), finalCleanEmail);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "protocolo", chamadoCriado.protocolo(),
                    "email", finalCleanEmail
            ));

        } catch (Exception e) {
            log.warn("[RESEND WEBHOOK] Falha ao processar e-mail inbound: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
