package thickethub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.AuditoriaResponse;
import thickethub.dto.response.PaginacaoResponse;
import thickethub.security.SecurityUtils;
import thickethub.service.AuditoriaService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO')")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginacaoResponse<AuditoriaResponse>>> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {

        Usuario logado = SecurityUtils.getUsuarioLogado();
        PaginacaoResponse<AuditoriaResponse> response = auditoriaService.listar(
                logado, usuarioId, acao, inicio, fim, pageable);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Logs de auditoria listados", 200));
    }
}
