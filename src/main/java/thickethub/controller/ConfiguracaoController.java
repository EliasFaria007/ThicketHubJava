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

import java.util.List;

@RestController
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfigDominioRepository configDominioRepository;

    @GetMapping("/dominios")
    public ResponseEntity<ApiResponse<List<ConfigDominiosResponse>>> listarDominios() {
        List<ConfigDominiosResponse> lista = configDominioRepository.findAll().stream()
                .map(ConfigDominiosResponse::de)
                .toList();
        return ResponseEntity.ok(ApiResponse.sucesso(lista, "Domínios listados", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'SUPER')")
    @PostMapping("/dominios")
    public ResponseEntity<ApiResponse<ConfigDominiosResponse>> adicionarDominio(@Valid @RequestBody ConfigDominiosRequest request) {
        if (configDominioRepository.existsByDominio(request.dominio())) {
            throw new ConflictException("Domínio já cadastrado: " + request.dominio());
        }

        ConfigDominio cd = ConfigDominio.builder()
                .dominio(request.dominio().toLowerCase().trim())
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
}
