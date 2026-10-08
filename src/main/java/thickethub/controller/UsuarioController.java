package thickethub.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.model.Usuario;
import thickethub.dto.request.*;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.ImportarLoteResponse;
import thickethub.dto.response.UsuarioResponse;
import thickethub.security.SecurityUtils;
import thickethub.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO')")
    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        UsuarioResponse response = usuarioService.criarUsuario(logado, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.sucesso(response, "Usuário criado com sucesso", 201));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO')")
    @PostMapping("/importar-lote")
    public ResponseEntity<ApiResponse<ImportarLoteResponse>> importarLote(@Valid @RequestBody ImportarUsuariosLoteRequest request) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        ImportarLoteResponse response = usuarioService.importarEmLote(logado, request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Lote processado com sucesso", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.sucesso(usuarioService.listarTodos(), "Usuários listados", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO') or @chamadoSecurity.ehMesmoUsuario(#id, authentication.name)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.sucesso(usuarioService.buscarPorId(id), "Usuário encontrado", 200));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me() {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(UsuarioResponse.de(logado), "Perfil do usuário", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO') or @chamadoSecurity.ehMesmoUsuario(#id, authentication.name)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> atualizar(@PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest request) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        UsuarioResponse response = usuarioService.atualizar(logado, id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Usuário atualizado com sucesso", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desativar(@PathVariable Long id) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        usuarioService.desativar(logado, id);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Usuário desativado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO')")
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<ApiResponse<Void>> ativar(@PathVariable Long id) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        usuarioService.ativar(logado, id);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Usuário ativado", 200));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERUSUARIO', 'TECNICO')")
    @PostMapping("/{id}/resetar-senha")
    public ResponseEntity<ApiResponse<UsuarioResponse>> resetarSenhaManual(@PathVariable Long id) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        UsuarioResponse response = usuarioService.resetarSenhaManual(logado, id);
        return ResponseEntity.ok(ApiResponse.sucesso(response, "Senha resetada com sucesso", 200));
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<ApiResponse<Void>> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        usuarioService.alterarSenha(logado, request);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Senha alterada com sucesso", 200));
    }
}
