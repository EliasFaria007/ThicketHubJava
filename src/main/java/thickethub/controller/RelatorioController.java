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

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER', 'TECNICO')")
    @GetMapping("/geral")
    public ResponseEntity<ApiResponse<MetricasResponse>> relatorioGeral() {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        MetricasResponse metricas = chamadoService.obterMetricas(logado);
        return ResponseEntity.ok(ApiResponse.sucesso(metricas, "Relatório geral consolidado", 200));
    }
}
