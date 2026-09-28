package thickethub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.dto.request.SlaConfigRequest;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.SlaConfigResponse;
import thickethub.service.SlaService;

import java.util.List;

@RestController
@RequestMapping("/sla")
@RequiredArgsConstructor
public class SlaController {

    private final SlaService slaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlaConfigResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.sucesso(slaService.listarTodos(), "Configurações de SLA listadas", 200));
    }

    @GetMapping("/{prioridade}")
    public ResponseEntity<ApiResponse<SlaConfigResponse>> buscarPorPrioridade(@PathVariable PrioridadeChamado prioridade) {
        return ResponseEntity.ok(ApiResponse.sucesso(slaService.buscarPorPrioridade(prioridade), "Configuração de SLA encontrada", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping
    public ResponseEntity<ApiResponse<SlaConfigResponse>> salvarOuAtualizar(@Valid @RequestBody SlaConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.sucesso(slaService.salvarOuAtualizar(request), "Configuração de SLA salva", 200));
    }
}
