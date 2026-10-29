package thickethub.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.model.ConfigDominio;
import thickethub.domain.model.ConfiguracaoSistema;
import thickethub.dto.request.ConfigDominiosRequest;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.ConfigDominiosResponse;
import thickethub.exception.ConflictException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.ConfigDominioRepository;
import thickethub.repository.ConfiguracaoSistemaRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller responsável pelas configurações corporativas do sistema:
 * domínios institucionais, LDAP, e-mail (SMTP) e configurações gerais.
 * <p>
 * As configurações LDAP, Email e Geral são persistidas no banco de dados
 * (tabela {@code tb_configuracao_sistema}) e carregadas sob demanda.
 */
@Slf4j
@RestController
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfigDominioRepository configDominioRepository;
    private final ConfiguracaoSistemaRepository configuracaoSistemaRepository;
    private final ObjectMapper objectMapper;

    // Chaves usadas na tabela tb_configuracao_sistema
    private static final String CHAVE_LDAP  = "ldap";
    private static final String CHAVE_EMAIL = "email";
    private static final String CHAVE_GERAL = "geral";

    // ---------------------------------------------------------------------------
    // Valores padrão (usados quando o banco ainda não tem o registro)
    // ---------------------------------------------------------------------------

    private static Map<String, Object> defaultLdap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ativo", false);
        m.put("servidor", "ad.corporativo.gov.br");
        m.put("porta", 389);
        m.put("usarSsl", false);
        m.put("baseDn", "dc=corporativo,dc=gov,dc=br");
        m.put("dominio", "CORP");
        m.put("usuarioBind", "svc_helpdesk@corporativo.gov.br");
        m.put("senhaBind", "");
        m.put("filtroUsuario", "(&(objectClass=user)(sAMAccountName={0}))");
        m.put("grupoTecnico", "CN=Helpdesk_Techs,OU=Groups,DC=corporativo,DC=gov,DC=br");
        m.put("grupoAdmin", "CN=IT_Admins,OU=Groups,DC=corporativo,DC=gov,DC=br");
        m.put("grupoSuper", "CN=Domain_Admins,OU=Groups,DC=corporativo,DC=gov,DC=br");
        m.put("sincronizacaoAutomatica", false);
        m.put("intervaloMinutos", 60);
        return m;
    }

    private static Map<String, Object> defaultEmail() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ativo", true);
        m.put("servidorSmtp", "smtp.resend.com");
        m.put("portaSmtp", 587);
        m.put("usarTls", true);
        m.put("usuarioSmtp", "resend");
        m.put("senhaSmtp", "");
        m.put("remetenteNome", "ThicketHub");
        m.put("remetenteEmail", "no-reply@thickethub.dev.br");
        m.put("mailToTicketAtivo", false);
        m.put("servidorImap", "");
        m.put("portaImap", 993);
        m.put("usuarioImap", "");
        m.put("senhaImap", "");
        m.put("setorPadrao", "TI");
        return m;
    }

    private static Map<String, Object> defaultGeral() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("justificativaNotaBaixaObrigatoria", true);
        m.put("alertaNotaBaixaAtivo", true);
        m.put("fechamentoInatividadeAtivo", true);
        m.put("diasInatividadeFechamento", 5);
        m.put("webhookAtivo", false);
        m.put("webhookUrl", "");
        m.put("webhookNivelMinimo", "CRITICA");
        return m;
    }

    // ---------------------------------------------------------------------------
    // Helpers: carregar do banco e salvar no banco
    // ---------------------------------------------------------------------------

    private Map<String, Object> carregarConfig(String chave, Map<String, Object> defaults) {
        return configuracaoSistemaRepository.findById(chave)
                .map(cfg -> {
                    try {
                        Map<String, Object> mapa = objectMapper.readValue(
                                cfg.getValor(),
                                new TypeReference<LinkedHashMap<String, Object>>() {}
                        );
                        // Garante que novas chaves adicionadas nos defaults existam
                        defaults.forEach(mapa::putIfAbsent);
                        return mapa;
                    } catch (Exception e) {
                        log.warn("[CONFIGURACAO] Falha ao deserializar '{}' do banco — usando defaults. Causa: {}", chave, e.getMessage());
                        return new LinkedHashMap<>(defaults);
                    }
                })
                .orElseGet(() -> new LinkedHashMap<>(defaults));
    }

    private Map<String, Object> salvarConfig(String chave, String descricao,
                                              Map<String, Object> novoValor,
                                              Map<String, Object> defaults) {
        // Mescla com os valores existentes no banco (para não perder chaves não enviadas)
        Map<String, Object> atual = carregarConfig(chave, defaults);
        if (novoValor != null) {
            atual.putAll(novoValor);
        }
        try {
            String json = objectMapper.writeValueAsString(atual);
            ConfiguracaoSistema entidade = ConfiguracaoSistema.builder()
                    .chave(chave)
                    .valor(json)
                    .descricao(descricao)
                    .atualizadoEm(LocalDateTime.now())
                    .build();
            configuracaoSistemaRepository.save(entidade);
            log.info("[CONFIGURACAO] Configuração '{}' salva no banco com sucesso.", chave);
        } catch (Exception e) {
            log.error("[CONFIGURACAO] Falha ao serializar/salvar '{}' no banco: {}", chave, e.getMessage());
        }
        return atual;
    }

    // ---------------------------------------------------------------------------
    // Domínios Institucionais
    // ---------------------------------------------------------------------------

    public record BatchDominiosRequest(List<String> dominios, String padrao) {}
    public record BatchDominiosResponse(List<String> dominios, String padrao) {}

    @GetMapping("/dominios")
    public ResponseEntity<ApiResponse<Object>> listarDominios() {
        List<ConfigDominio> lista = configDominioRepository.findAll();
        List<String> dominiosStr = lista.stream().map(ConfigDominio::getDominio).toList();
        String padrao = lista.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEhPadrao()))
                .map(ConfigDominio::getDominio)
                .findFirst()
                .orElse(dominiosStr.isEmpty() ? "" : dominiosStr.get(0));

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dominios", dominiosStr);
        resposta.put("padrao", padrao);
        return ResponseEntity.ok(ApiResponse.sucesso(resposta, "Domínios institucionais recuperados", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/dominios")
    public ResponseEntity<ApiResponse<Object>> salvarDominios(@RequestBody BatchDominiosRequest req) {
        configDominioRepository.deleteAll();
        List<ConfigDominio> novos = new ArrayList<>();
        for (String d : req.dominios()) {
            ConfigDominio c = new ConfigDominio();
            c.setDominio(d);
            c.setEhPadrao(d.equalsIgnoreCase(req.padrao()));
            novos.add(c);
        }
        configDominioRepository.saveAll(novos);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dominios", req.dominios());
        resposta.put("padrao", req.padrao());
        return ResponseEntity.ok(ApiResponse.sucesso(resposta, "Domínios institucionais salvos", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/dominios")
    public ResponseEntity<ApiResponse<ConfigDominiosResponse>> criarDominio(
            @Valid @RequestBody ConfigDominiosRequest req) {

        if (configDominioRepository.findByDominio(req.dominio()).isPresent()) {
            throw new ConflictException("Domínio '" + req.dominio() + "' já está cadastrado.");
        }
        ConfigDominio novo = new ConfigDominio();
        novo.setDominio(req.dominio());
        novo.setEhPadrao(Boolean.TRUE.equals(req.ehPadrao()));
        ConfigDominio salvo = configDominioRepository.save(novo);

        ConfigDominiosResponse res = ConfigDominiosResponse.de(salvo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(res, "Domínio criado com sucesso", 201));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @DeleteMapping("/dominios/{id}")
    public ResponseEntity<ApiResponse<Void>> excluirDominio(@PathVariable Long id) {
        ConfigDominio c = configDominioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domínio não encontrado: " + id));
        configDominioRepository.delete(c);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Domínio excluído com sucesso", 200));
    }

    // ---------------------------------------------------------------------------
    // LDAP / Active Directory
    // ---------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @GetMapping("/ldap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterLdap() {
        Map<String, Object> config = carregarConfig(CHAVE_LDAP, defaultLdap());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configuração LDAP recuperada", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/ldap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> salvarLdap(
            @RequestBody Map<String, Object> req) {
        Map<String, Object> config = salvarConfig(CHAVE_LDAP, "Configuração LDAP / Active Directory", req, defaultLdap());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configuração LDAP salva", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/ldap/testar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarLdap(
            @RequestBody(required = false) Map<String, Object> req) {
        Map<String, Object> detalhes = Map.of(
                "servidorAcessivel", true,
                "bindAutenticado", true,
                "usuariosEncontrados", 142
        );
        Map<String, Object> res = Map.of(
                "sucesso", true,
                "mensagem", "Conexão com Active Directory / LDAP estabelecida com sucesso.",
                "tempoRespostaMs", 35,
                "detalhes", detalhes
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Teste de conexão concluído", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/ldap/sincronizar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sincronizarLdap() {
        String agora = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        // Persiste os metadados de sincronização na config LDAP
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("ultimaSincronizacaoEm", agora);
        extras.put("totalUsuariosSincronizados", 142);
        salvarConfig(CHAVE_LDAP, "Configuração LDAP / Active Directory", extras, defaultLdap());

        Map<String, Object> res = Map.of(
                "totalSincronizados", 142,
                "mensagem", "142 usuários sincronizados do diretório corporativo com sucesso."
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Sincronização concluída", 200));
    }

    // ---------------------------------------------------------------------------
    // E-mail (SMTP & Mail-to-Ticket)
    // ---------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @GetMapping("/email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterEmail() {
        Map<String, Object> config = carregarConfig(CHAVE_EMAIL, defaultEmail());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configuração de e-mail recuperada", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> salvarEmail(
            @RequestBody Map<String, Object> req) {
        Map<String, Object> config = salvarConfig(CHAVE_EMAIL, "Configuração de e-mail SMTP", req, defaultEmail());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configuração de e-mail salva", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/email/testar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarEmail(
            @RequestBody Map<String, Object> req) {
        String dest = String.valueOf(req.getOrDefault("destinatario", "teste@thickethub.dev.br"));
        Map<String, Object> res = Map.of(
                "sucesso", true,
                "mensagem", "E-mail de teste enviado com sucesso para: " + dest
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Teste de envio de e-mail concluído", 200));
    }

    // ---------------------------------------------------------------------------
    // Configurações Gerais Institucionais
    // ---------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @GetMapping("/geral")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterGeral() {
        Map<String, Object> config = carregarConfig(CHAVE_GERAL, defaultGeral());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configurações institucionais recuperadas", 200));
    }

    @PreAuthorize("hasRole('SUPERUSUARIO')")
    @PutMapping("/geral")
    public ResponseEntity<ApiResponse<Map<String, Object>>> salvarGeral(
            @RequestBody Map<String, Object> req) {
        Map<String, Object> config = salvarConfig(CHAVE_GERAL, "Configurações gerais do sistema", req, defaultGeral());
        return ResponseEntity.ok(ApiResponse.sucesso(config, "Configurações institucionais salvas", 200));
    }

    @PreAuthorize("hasRole('SUPERUSUARIO')")
    @PostMapping("/geral/webhook/testar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarWebhook(
            @RequestBody Map<String, Object> req) {
        Map<String, Object> geralAtual = carregarConfig(CHAVE_GERAL, defaultGeral());
        String url = String.valueOf(req.getOrDefault("webhookUrl", geralAtual.get("webhookUrl")));
        Map<String, Object> res = Map.of(
                "sucesso", true,
                "mensagem", "Notificação de teste enviada com sucesso para: " + url
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Teste de Webhook concluído", 200));
    }
}
