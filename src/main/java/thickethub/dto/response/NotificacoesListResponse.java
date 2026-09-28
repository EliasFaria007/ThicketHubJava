package thickethub.dto.response;

import java.util.List;

public record NotificacoesListResponse(
        List<NotificacaoResponse> notificacoes,
        long totalNaoLidas
) {}
