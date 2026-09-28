package thickethub.dto.response;

import java.util.List;

public record ImportarLoteResponse(
        int total,
        int sucessos,
        int falhas,
        List<ItemSucesso> itensSucesso,
        List<ItemFalha> itensFalha
) {
    public record ItemSucesso(int linha, String email, Long id) {}
    public record ItemFalha(int linha, String email, String motivo) {}
}
