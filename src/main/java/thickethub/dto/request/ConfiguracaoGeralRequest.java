package thickethub.dto.request;

public record ConfiguracaoGeralRequest(
        Boolean justificativaNotaBaixaObrigatoria,
        Boolean alertaNotaBaixaAtivo,
        Boolean fechamentoInatividadeAtivo,
        Integer diasInatividadeFechamento,
        Boolean webhookAtivo,
        String webhookUrl,
        String webhookNivelMinimo
) {}
