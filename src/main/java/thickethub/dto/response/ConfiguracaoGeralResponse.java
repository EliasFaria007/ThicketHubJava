package thickethub.dto.response;

public record ConfiguracaoGeralResponse(
        boolean justificativaNotaBaixaObrigatoria,
        boolean alertaNotaBaixaAtivo,
        boolean fechamentoInatividadeAtivo,
        int diasInatividadeFechamento,
        boolean webhookAtivo,
        String webhookUrl,
        String webhookNivelMinimo
) {}
