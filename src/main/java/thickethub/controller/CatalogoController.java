package thickethub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.dto.request.ServicoRequest;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.ServicoResponse;
import thickethub.service.ServicoService;

import java.util.List;

@RestController
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final ServicoService servicoService;

    @GetMapping("/servicos")
    public ResponseEntity<ApiResponse<List<ServicoResponse>>> listarServicos(
            @RequestParam(required = false, defaultValue = "true") Boolean apenasAtivos) {
        return ResponseEntity.ok(ApiResponse.sucesso(servicoService.listarTodos(apenasAtivos), "Serviços listados", 200));
    }

    @GetMapping("/servicos/setor/{setor}")
    public ResponseEntity<ApiResponse<List<ServicoResponse>>> listarPorSetor(@PathVariable String setor) {
        return ResponseEntity.ok(ApiResponse.sucesso(servicoService.listarPorSetor(setor), "Serviços por setor", 200));
    }

    @GetMapping("/servicos/{id}")
    public ResponseEntity<ApiResponse<ServicoResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.sucesso(servicoService.buscarPorId(id), "Serviço encontrado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/servicos")
    public ResponseEntity<ApiResponse<ServicoResponse>> criar(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(servicoService.criar(request), "Serviço criado", 201));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PutMapping("/servicos/{id}")
    public ResponseEntity<ApiResponse<ServicoResponse>> atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(ApiResponse.sucesso(servicoService.atualizar(id, request), "Serviço atualizado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @DeleteMapping("/servicos/{id}")
    public ResponseEntity<ApiResponse<Void>> desativar(@PathVariable Long id) {
        servicoService.desativar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Serviço desativado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PatchMapping("/servicos/{id}/ativar")
    public ResponseEntity<ApiResponse<Void>> ativar(@PathVariable Long id) {
        servicoService.ativar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Serviço ativado", 200));
    }
}
