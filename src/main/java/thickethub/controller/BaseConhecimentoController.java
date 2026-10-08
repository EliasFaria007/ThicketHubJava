package thickethub.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.model.ArtigoBaseConhecimento;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.ApiResponse;
import thickethub.exception.ResourceNotFoundException;
import thickethub.repository.ArtigoBaseConhecimentoRepository;
import thickethub.security.SecurityUtils;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/base-conhecimento")
@RequiredArgsConstructor
public class BaseConhecimentoController {

    private final ArtigoBaseConhecimentoRepository repository;

    public record ArtigoRequest(
            String titulo,
            String resumo,
            String conteudo,
            String setor,
            String categoria,
            List<String> tags,
            String anexoPdfNome,
            Long anexoPdfTamanho,
            String anexoPdfBase64
    ) {}

    public record ArtigoResponse(
            String id,
            String titulo,
            String slug,
            String resumo,
            String conteudo,
            String setor,
            String categoria,
            List<String> tags,
            Integer utilSim,
            Integer utilNao,
            Integer visualizacoes,
            String autorNome,
            String anexoPdfNome,
            Long anexoPdfTamanho,
            String anexoPdfBase64,
            String criadoEm,
            String atualizadoEm
    ) {
        public static ArtigoResponse de(ArtigoBaseConhecimento a) {
            if (a == null) return null;
            List<String> tagsList = a.getTags() != null && !a.getTags().isBlank()
                    ? Arrays.stream(a.getTags().split(",")).map(String::trim).filter(s -> !s.isBlank()).toList()
                    : List.of();

            DateTimeFormatter iso = DateTimeFormatter.ISO_DATE_TIME;
            return new ArtigoResponse(
                    String.valueOf(a.getId()),
                    a.getTitulo(),
                    a.getSlug(),
                    a.getResumo(),
                    a.getConteudo(),
                    a.getSetor(),
                    a.getCategoria(),
                    tagsList,
                    a.getUtilSim() != null ? a.getUtilSim() : 0,
                    a.getUtilNao() != null ? a.getUtilNao() : 0,
                    a.getVisualizacoes() != null ? a.getVisualizacoes() : 0,
                    a.getAutorNome() != null ? a.getAutorNome() : "Suporte Técnico",
                    a.getAnexoPdfNome(),
                    a.getAnexoPdfTamanho(),
                    a.getAnexoPdfBase64(),
                    a.getCriadoEm() != null ? a.getCriadoEm().format(iso) : LocalDateTime.now().format(iso),
                    a.getAtualizadoEm() != null ? a.getAtualizadoEm().format(iso) : LocalDateTime.now().format(iso)
            );
        }
    }

    public record AvaliarRequest(Boolean util) {}

    @PostConstruct
    public void seedSeVazio() {
        if (repository.count() == 0) {
            repository.save(ArtigoBaseConhecimento.builder()
                    .titulo("Como redefinir sua senha de rede e e-mail institucional")
                    .slug("como-redefinir-senha-de-rede")
                    .resumo("Passo a passo rápido para alteração e recuperação de senha expirada no Active Directory.")
                    .conteudo("1. Na tela de bloqueio do Windows (Ctrl+Alt+Del), selecione 'Alterar uma senha'.\n2. Digite sua senha antiga e defina a nova com pelo menos 8 caracteres.\n3. Se a senha já expirou, solicite o link de auto-recuperação por SMS.")
                    .setor("TI")
                    .categoria("Acessos e Contas")
                    .tags("senha,active directory,login,bloqueio")
                    .utilSim(54)
                    .utilNao(2)
                    .visualizacoes(180)
                    .autorNome("Administrador do Sistema")
                    .criadoEm(LocalDateTime.now().minusDays(30))
                    .atualizadoEm(LocalDateTime.now().minusDays(5))
                    .build());

            repository.save(ArtigoBaseConhecimento.builder()
                    .titulo("Como mapear impressoras do setor no Windows 11")
                    .slug("como-mapear-impressoras-windows-11")
                    .resumo("Instruções para localizar e adicionar impressoras de rede departamentais.")
                    .conteudo("1. Pressione Windows + R e digite '\\\\printserver\\'.\n2. Localize a impressora com a etiqueta do seu setor (ex: IMP-TI-01).\n3. Clique com o botão direito e selecione 'Conectar'.")
                    .setor("TI")
                    .categoria("Equipamentos e Impressão")
                    .tags("impressora,rede,windows 11,impressao")
                    .utilSim(38)
                    .utilNao(4)
                    .visualizacoes(112)
                    .autorNome("Administrador do Sistema")
                    .criadoEm(LocalDateTime.now().minusDays(20))
                    .atualizadoEm(LocalDateTime.now().minusDays(2))
                    .build());

            repository.save(ArtigoBaseConhecimento.builder()
                    .titulo("Configuração da VPN institucional para teletrabalho")
                    .slug("configuracao-vpn-institucional")
                    .resumo("Guia de instalação do cliente FortiClient/OpenVPN para acesso seguro aos sistemas.")
                    .conteudo("1. Baixe o cliente VPN oficial no portal institucional.\n2. No campo 'Gateway Remoto', insira 'vpn.defensoria.mg.gov.br'.\n3. Conecte com suas credenciais de rede.")
                    .setor("TI")
                    .categoria("Redes e Infraestrutura")
                    .tags("vpn,teletrabalho,acesso remoto,seguranca")
                    .utilSim(82)
                    .utilNao(5)
                    .visualizacoes(250)
                    .autorNome("Administrador do Sistema")
                    .criadoEm(LocalDateTime.now().minusDays(45))
                    .atualizadoEm(LocalDateTime.now().minusDays(1))
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtigoResponse>>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String setor,
            @RequestParam(required = false) String categoria
    ) {
        List<ArtigoBaseConhecimento> lista = repository.findAll();

        if (setor != null && !setor.isBlank()) {
            lista = lista.stream()
                    .filter(a -> a.getSetor().equalsIgnoreCase(setor))
                    .toList();
        }

        if (categoria != null && !categoria.isBlank()) {
            lista = lista.stream()
                    .filter(a -> a.getCategoria().equalsIgnoreCase(categoria))
                    .toList();
        }

        if (busca != null && !busca.isBlank()) {
            String b = busca.toLowerCase().trim();
            lista = lista.stream()
                    .filter(a -> a.getTitulo().toLowerCase().contains(b)
                            || a.getResumo().toLowerCase().contains(b)
                            || a.getConteudo().toLowerCase().contains(b)
                            || (a.getTags() != null && a.getTags().toLowerCase().contains(b)))
                    .toList();
        }

        List<ArtigoResponse> dtos = lista.stream().map(ArtigoResponse::de).toList();
        return ResponseEntity.ok(ApiResponse.sucesso(dtos, "Artigos listados com sucesso", 200));
    }

    @GetMapping("/{idOuSlug}")
    public ResponseEntity<ApiResponse<ArtigoResponse>> buscarPorIdOuSlug(@PathVariable String idOuSlug) {
        ArtigoBaseConhecimento artigo = null;
        try {
            Long id = Long.parseLong(idOuSlug);
            artigo = repository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (artigo == null) {
            artigo = repository.findBySlug(idOuSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + idOuSlug));
        }

        artigo.setVisualizacoes(artigo.getVisualizacoes() != null ? artigo.getVisualizacoes() + 1 : 1);
        repository.save(artigo);

        return ResponseEntity.ok(ApiResponse.sucesso(ArtigoResponse.de(artigo), "Artigo localizado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO', 'SUPER')")
    @PostMapping
    public ResponseEntity<ApiResponse<ArtigoResponse>> criar(@Valid @RequestBody ArtigoRequest req) {
        String nomeAutor = "Técnico";
        try {
            Usuario u = SecurityUtils.getUsuarioLogado();
            if (u != null && u.getNome() != null) nomeAutor = u.getNome();
        } catch (Exception ignored) {}

        String slug = gerarSlug(req.titulo());
        String tagsJoin = req.tags() != null ? String.join(",", req.tags()) : "";

        ArtigoBaseConhecimento novo = ArtigoBaseConhecimento.builder()
                .titulo(req.titulo())
                .slug(slug)
                .resumo(req.resumo() != null && !req.resumo().isBlank() ? req.resumo() : req.titulo())
                .conteudo(req.conteudo())
                .setor(req.setor() != null && !req.setor().isBlank() ? req.setor() : "TI")
                .categoria(req.categoria() != null && !req.categoria().isBlank() ? req.categoria() : "Geral")
                .tags(tagsJoin)
                .utilSim(0)
                .utilNao(0)
                .visualizacoes(0)
                .autorNome(nomeAutor)
                .anexoPdfNome(req.anexoPdfNome())
                .anexoPdfTamanho(req.anexoPdfTamanho())
                .anexoPdfBase64(req.anexoPdfBase64())
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

        repository.save(novo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(ArtigoResponse.de(novo), "Artigo cadastrado", 201));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO', 'SUPER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtigoResponse>> atualizar(@PathVariable Long id, @RequestBody ArtigoRequest req) {
        ArtigoBaseConhecimento artigo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + id));

        if (req.titulo() != null && !req.titulo().isBlank()) {
            artigo.setTitulo(req.titulo());
            artigo.setSlug(gerarSlug(req.titulo()));
        }
        if (req.resumo() != null) artigo.setResumo(req.resumo());
        if (req.conteudo() != null) artigo.setConteudo(req.conteudo());
        if (req.setor() != null) artigo.setSetor(req.setor());
        if (req.categoria() != null) artigo.setCategoria(req.categoria());
        if (req.tags() != null) artigo.setTags(String.join(",", req.tags()));
        if (req.anexoPdfNome() != null) {
            artigo.setAnexoPdfNome(req.anexoPdfNome().isBlank() ? null : req.anexoPdfNome());
        }
        if (req.anexoPdfTamanho() != null) {
            artigo.setAnexoPdfTamanho(req.anexoPdfTamanho() <= 0 ? null : req.anexoPdfTamanho());
        }
        if (req.anexoPdfBase64() != null) {
            artigo.setAnexoPdfBase64(req.anexoPdfBase64().isBlank() ? null : req.anexoPdfBase64());
        }
        artigo.setAtualizadoEm(LocalDateTime.now());

        repository.save(artigo);
        return ResponseEntity.ok(ApiResponse.sucesso(ArtigoResponse.de(artigo), "Artigo atualizado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable Long id) {
        ArtigoBaseConhecimento artigo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + id));
        repository.delete(artigo);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Artigo removido com sucesso", 200));
    }

    @PostMapping("/{id}/avaliar")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> avaliar(
            @PathVariable Long id,
            @RequestBody AvaliarRequest req
    ) {
        ArtigoBaseConhecimento artigo = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artigo não encontrado: " + id));

        if (Boolean.TRUE.equals(req.util())) {
            artigo.setUtilSim((artigo.getUtilSim() != null ? artigo.getUtilSim() : 0) + 1);
        } else {
            artigo.setUtilNao((artigo.getUtilNao() != null ? artigo.getUtilNao() : 0) + 1);
        }
        repository.save(artigo);

        Map<String, Integer> res = Map.of(
                "utilSim", artigo.getUtilSim(),
                "utilNao", artigo.getUtilNao()
        );
        return ResponseEntity.ok(ApiResponse.sucesso(res, "Avaliação registrada", 200));
    }

    private String gerarSlug(String input) {
        if (input == null) return "artigo-" + System.currentTimeMillis();
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
        return normalized.isBlank() ? "artigo-" + System.currentTimeMillis() : normalized;
    }
}
