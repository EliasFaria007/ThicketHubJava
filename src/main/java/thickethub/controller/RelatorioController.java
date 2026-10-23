package thickethub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.MetricasResponse;
import thickethub.security.SecurityUtils;
import thickethub.service.ChamadoService;

@RestController
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final ChamadoService chamadoService;
    private final thickethub.service.RelatorioService relatorioService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO')")
    @GetMapping
    public ResponseEntity<ApiResponse<thickethub.dto.response.RelatoriosResponse>> relatorioCompleto(
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate de,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate ate,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String setor,
            @org.springframework.web.bind.annotation.RequestParam(required = false) thickethub.domain.enums.StatusChamado status,
            @org.springframework.web.bind.annotation.RequestParam(required = false) thickethub.domain.enums.PrioridadeChamado prioridade,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long tecnicoId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long servicoId
    ) {
        var dados = relatorioService.gerarRelatorio(de, ate, setor, status, prioridade, tecnicoId, servicoId);
        return ResponseEntity.ok(ApiResponse.sucesso(dados, "Relatório consolidado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO')")
    @GetMapping("/geral")
    public ResponseEntity<ApiResponse<MetricasResponse>> relatorioGeral() {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        MetricasResponse metricas = chamadoService.obterMetricas(logado);
        return ResponseEntity.ok(ApiResponse.sucesso(metricas, "Relatório geral consolidado", 200));
    }
}
