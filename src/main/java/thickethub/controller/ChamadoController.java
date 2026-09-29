package thickethub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.*;
import thickethub.security.SecurityUtils;
import thickethub.service.ChamadoService;

import java.util.List;

@RestController
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> criar(@Valid @RequestBody NovoChamadoRequest request) {
        Usuario solicitante = SecurityUtils.getUsuarioLogado();
        ChamadoDetalheResponse response = chamadoService.criarChamado(solicitante, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(response, "Chamado aberto com sucesso", 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginacaoResponse<ChamadoResumoResponse>>> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) PrioridadeChamado prioridade,
            @RequestParam(required = false) Long servicoId,
            @RequestParam(required = false) String setor,
            @RequestParam(required = false) String fila,
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 15, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {

        Usuario usuarioLogado = SecurityUtils.getUsuarioLogado();
        PaginacaoResponse<ChamadoResumoResponse> response = chamadoService.listar(
                usuarioLogado, status, prioridade, servicoId, setor, fila, busca, pageable);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Chamados listados", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeVisualizar(#id, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.buscar(id), "Chamado carregado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeVisualizarPorProtocolo(#protocolo, authentication.name)")
    @GetMapping("/protocolo/{protocolo}")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> buscarPorProtocolo(@PathVariable String protocolo) {
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.buscarPorProtocolo(protocolo), "Chamado carregado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarChamadoRequest request) {
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.atualizar(id, request), "Chamado atualizado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/assumir")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> assumir(@PathVariable Long id) {
        Usuario tecnico = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.assumirChamado(tecnico, id), "Chamado assumido", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/pausar")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> pausar(@PathVariable Long id) {
        Usuario tecnico = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.pausarChamado(tecnico, id), "Chamado pausado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/retomar")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> retomar(@PathVariable Long id) {
        Usuario tecnico = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.retomarChamado(tecnico, id), "Chamado retomado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/resolver")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> resolver(
            @PathVariable Long id,
            @Valid @RequestBody ResolverChamadoRequest request) {
        Usuario tecnico = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.resolverChamado(tecnico, id, request), "Chamado resolvido", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/devolver")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> devolver(
            @PathVariable Long id,
            @Valid @RequestBody DevolverChamadoRequest request) {
        Usuario tecnico = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.devolverChamado(tecnico, id, request), "Chamado devolvido para a fila", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeEncaminharPara(#id, authentication.name, #request.filaSetor)")
    @PostMapping("/{id}/encaminhar")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> encaminhar(
            @PathVariable Long id,
            @Valid @RequestBody EncaminharChamadoRequest request) {
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.encaminhar(id, request), "Chamado encaminhado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> reabrir(
            @PathVariable Long id,
            @Valid @RequestBody ReabrirChamadoRequest request) {
        Usuario admin = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.reabrirChamado(admin, id, request.justificativa()), "Chamado reaberto", 200));
    }

    @PreAuthorize("@chamadoSecurity.ehSolicitante(#id, authentication.name)")
    @PostMapping("/{id}/avaliar")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> avaliar(
            @PathVariable Long id,
            @Valid @RequestBody AvaliarChamadoRequest request) {
        Usuario solicitante = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.avaliarChamado(solicitante, id, request), "Chamado avaliado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeVisualizar(#id, authentication.name)")
    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ApiResponse<ComentarioResponse>> adicionarComentario(
            @PathVariable Long id,
            @Valid @RequestBody NovoComentarioRequest request) {
        Usuario autor = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.adicionarComentario(autor, id, request), "Comentário adicionado", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeVisualizar(#id, authentication.name)")
    @GetMapping("/{id}/comentarios")
    public ResponseEntity<ApiResponse<List<ComentarioResponse>>> listarComentarios(@PathVariable Long id) {
        Usuario usuario = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.listarComentarios(usuario, id), "Comentários listados", 200));
    }

    @PreAuthorize("@chamadoSecurity.podeAtuarNoSetor(#id, authentication.name)")
    @PostMapping("/{id}/vincular")
    public ResponseEntity<ApiResponse<Void>> vincularChamado(
            @PathVariable Long id,
            @Valid @RequestBody VincularChamadoRequest request) {
        Usuario usuario = SecurityUtils.getUsuarioLogado();
        chamadoService.vincularChamado(usuario, id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Chamados vinculados com sucesso", 200));
    }

    @GetMapping("/metricas")
    public ResponseEntity<ApiResponse<MetricasResponse>> metricas() {
        Usuario usuario = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(chamadoService.obterMetricas(usuario), "Métricas carregadas", 200));
    }
}
