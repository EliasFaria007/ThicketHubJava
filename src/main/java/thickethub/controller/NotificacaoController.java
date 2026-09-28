package thickethub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.ApiResponse;
import thickethub.dto.response.NotificacaoResponse;
import thickethub.dto.response.NotificacoesListResponse;
import thickethub.dto.response.PaginacaoResponse;
import thickethub.security.SecurityUtils;
import thickethub.service.NotificacaoService;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping("/nao-lidas")
    public ResponseEntity<ApiResponse<NotificacoesListResponse>> listarNaoLidas() {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(notificacaoService.listarNaoLidas(logado.getId()), "Notificações não lidas", 200));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginacaoResponse<NotificacaoResponse>>> listarTodas(
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        return ResponseEntity.ok(ApiResponse.sucesso(notificacaoService.listarTodas(logado.getId(), pageable), "Histórico de notificações", 200));
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<ApiResponse<Void>> marcarComoLida(@PathVariable Long id) {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        notificacaoService.marcarComoLida(logado.getId(), id);
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Notificação marcada como lida", 200));
    }

    @PatchMapping("/marcar-todas-lidas")
    public ResponseEntity<ApiResponse<Void>> marcarTodasComoLidas() {
        Usuario logado = SecurityUtils.getUsuarioLogado();
        notificacaoService.marcarTodasComoLidas(logado.getId());
        return ResponseEntity.ok(ApiResponse.sucesso(null, "Todas as notificações foram marcadas como lidas", 200));
    }
}
