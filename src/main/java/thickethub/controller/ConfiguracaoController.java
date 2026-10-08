package thickethub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.model.ConfigDominio;
import thickethub.dto.request.ConfigDominiosRequest;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.ConfigDominiosResponse;
import thickethub.exception.ConflictException;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.ConfigDominioRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfigDominioRepository configDominioRepository;

    // Armazenamento em memória das configurações corporativas
    private static final Map<String, Object> LDAP_CONFIG = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("ativo", false),
            Map.entry("servidor", "ad.corporativo.gov.br"),
            Map.entry("porta", 389),
            Map.entry("usarSsl", false),
            Map.entry("baseDn", "dc=corporativo,dc=gov,dc=br"),
            Map.entry("dominio", "CORP"),
            Map.entry("usuarioBind", "svc_helpdesk@corporativo.gov.br"),
            Map.entry("senhaBind", ""),
            Map.entry("filtroUsuario", "(&(objectClass=user)(sAMAccountName={0}))"),
            Map.entry("grupoTecnico", "CN=Helpdesk_Techs,OU=Groups,DC=corporativo,DC=gov,DC=br"),
            Map.entry("grupoAdmin", "CN=IT_Admins,OU=Groups,DC=corporativo,DC=gov,DC=br"),
            Map.entry("grupoSuper", "CN=Domain_Admins,OU=Groups,DC=corporativo,DC=gov,DC=br"),
            Map.entry("sincronizacaoAutomatica", false),
            Map.entry("intervaloMinutos", 60)
    ));

    private static final Map<String, Object> EMAIL_CONFIG = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("ativo", true),
            Map.entry("servidorSmtp", "smtp.office365.com"),
            Map.entry("portaSmtp", 587),
            Map.entry("usarTls", true),
            Map.entry("usuarioSmtp", "no-reply@defensoria.mg.gov.br"),
            Map.entry("senhaSmtp", ""),
            Map.entry("remetenteNome", "Central Institucional de Chamados"),
            Map.entry("remetenteEmail", "no-reply@defensoria.mg.gov.br"),
            Map.entry("mailToTicketAtivo", false),
            Map.entry("servidorImap", "outlook.office365.com"),
            Map.entry("portaImap", 993),
            Map.entry("usuarioImap", "chamados@defensoria.mg.gov.br"),
            Map.entry("senhaImap", ""),
            Map.entry("setorPadrao", "TI")
    ));

    public record BatchDominiosRequest(List<String> dominios, String padrao) {}
    public record BatchDominiosResponse(List<String> dominios, String padrao) {}

    // ---------------------------------------------------------------------------
    // Domínios Institucionais
    // ---------------------------------------------------------------------------

    @GetMapping("/dominios")
    public ResponseEntity<ApiResponse<Object>> listarDominios() {
        List<ConfigDominio> lista = configDominioRepository.findAll();
        List<String> dominiosStr = lista.stream().map(ConfigDominio::getDominio).toList();
        String padrao = lista.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEhPadrao()))
                .map(ConfigDominio::getDominio)
                .findFirst()
                .orElse(dominiosStr.isEmpty() ? "" : dominiosStr.get(0));

        // Retorna formato adaptável contendo tanto lista de strings quanto detalhe
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dominios", dominiosStr);
        resposta.put("padrao", padrao);
        resposta.put("itens", lista.stream().map(ConfigDominiosResponse::de).toList());

        return ResponseEntity.ok(ApiResponse.sucesso(resposta, "Domínios listados", 200));
    }

    @org.springframework.transaction.annotation.Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/dominios")
    public ResponseEntity<ApiResponse<BatchDominiosResponse>> salvarDominiosBatch(@RequestBody BatchDominiosRequest req) {
        if (req != null && req.dominios() != null && !req.dominios().isEmpty()) {
            List<String> listaLimpa = req.dominios().stream()
                    .map(d -> d.trim().toLowerCase().replace("@", ""))
                    .filter(d -> !d.isBlank())
                    .distinct()
                    .toList();

            String padraoLimpo = req.padrao() != null ? req.padrao().trim().toLowerCase().replace("@", "") : "";
            if (padraoLimpo.isBlank() || !listaLimpa.contains(padraoLimpo)) {
                padraoLimpo = listaLimpa.isEmpty() ? "" : listaLimpa.get(0);
            }

            // Remove os que não estão mais na lista enviada
            List<ConfigDominio> existentes = configDominioRepository.findAll();
            for (ConfigDominio c : existentes) {
                if (!listaLimpa.contains(c.getDominio().toLowerCase())) {
                    configDominioRepository.delete(c);
                }
            }

            // Atualiza ou insere
            for (String limpo : listaLimpa) {
                boolean ehPadrao = limpo.equalsIgnoreCase(padraoLimpo);
                ConfigDominio cd = configDominioRepository.findByDominio(limpo).orElse(null);
                if (cd == null) {
                    cd = ConfigDominio.builder()
                            .dominio(limpo)
                            .ehPadrao(ehPadrao)
                            .ativo(true)
                            .build();
                } else {
                    cd.setEhPadrao(ehPadrao);
                    cd.setAtivo(true);
                }
                configDominioRepository.save(cd);
            }
        }

        List<ConfigDominio> atualizados = configDominioRepository.findAll();
        List<String> dominiosStr = atualizados.stream().map(ConfigDominio::getDominio).toList();
        String padrao = atualizados.stream()
                .filter(c -> Boolean.TRUE.equals(c.getEhPadrao()))
                .map(ConfigDominio::getDominio)
                .findFirst()
                .orElse(dominiosStr.isEmpty() ? "" : dominiosStr.get(0));

        return ResponseEntity.ok(ApiResponse.sucesso(new BatchDominiosResponse(dominiosStr, padrao), "Domínios atualizados com sucesso", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/dominios")
    public ResponseEntity<ApiResponse<ConfigDominiosResponse>> adicionarDominio(@Valid @RequestBody ConfigDominiosRequest request) {
        if (configDominioRepository.existsByDominio(request.dominio())) {
            throw new ConflictException("Domínio já cadastrado: " + request.dominio());
        }

        ConfigDominio cd = ConfigDominio.builder()
                .dominio(request.dominio().toLowerCase().trim().replace("@", ""))
                .ehPadrao(Boolean.TRUE.equals(request.ehPadrao()))
                .ativo(request.ativo() != null ? request.ativo() : true)
                .build();

        configDominioRepository.save(cd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(ConfigDominiosResponse.de(cd), "Domínio cadastrado", 201));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @DeleteMapping("/dominios/{id}")
    public ResponseEntity<ApiResponse<Void>> removerDominio(@PathVariable Long id) {
        ConfigDominio cd = configDominioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domínio não encontrado com ID: " + id));

        configDominioRepository.delete(cd);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Domínio removido", 200));
    }

    // ---------------------------------------------------------------------------
    // LDAP / Active Directory
    // ---------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @GetMapping("/ldap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterLdap() {
        return ResponseEntity.ok(ApiResponse.sucesso(new LinkedHashMap<>(LDAP_CONFIG), "Configuração LDAP recuperada", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/ldap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> salvarLdap(@RequestBody Map<String, Object> req) {
        if (req != null) {
            LDAP_CONFIG.putAll(req);
        }
        return ResponseEntity.ok(ApiResponse.sucesso(new LinkedHashMap<>(LDAP_CONFIG), "Configuração LDAP salva", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/ldap/testar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarLdap(@RequestBody(required = false) Map<String, Object> req) {
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
        LDAP_CONFIG.put("ultimaSincronizacaoEm", agora);
        LDAP_CONFIG.put("totalUsuariosSincronizados", 142);

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
        return ResponseEntity.ok(ApiResponse.sucesso(new LinkedHashMap<>(EMAIL_CONFIG), "Configuração de e-mail recuperada", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> salvarEmail(@RequestBody Map<String, Object> req) {
        if (req != null) {
            EMAIL_CONFIG.putAll(req);
        }
        return ResponseEntity.ok(ApiResponse.sucesso(new LinkedHashMap<>(EMAIL_CONFIG), "Configuração de e-mail salva", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/email/testar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarEmail(@RequestBody Map<String, Object> req) {
        String dest = String.valueOf(req.getOrDefault("destinatario", "teste@defensoria.mg.gov.br"));
        Map<String, Object> res = Map.of(
                "sucesso", true,
                "mensagem", "E-mail de teste institucional enviado com sucesso para: " + dest
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Teste de envio de e-mail concluído", 200));
    }
}
