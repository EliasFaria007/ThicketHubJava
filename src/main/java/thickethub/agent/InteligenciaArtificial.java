package thickethub.agent;

import org.springframework.stereotype.Component;
import thickethub.domain.enums.PrioridadeChamado;

import java.util.Locale;

@Component
public class InteligenciaArtificial {

    /**
     * Sugere prioridade com base na análise de palavras-chave da descrição do chamado.
     */
    public PrioridadeChamado sugerirPrioridade(String titulo, String descricao) {
        if (titulo == null) titulo = "";
        if (descricao == null) descricao = "";
        String texto = (titulo + " " + descricao).toLowerCase(Locale.ROOT);

        if (texto.contains("urgente") || texto.contains("parou") || texto.contains("fora do ar")
                || texto.contains("servidor caiu") || texto.contains("crítico") || texto.contains("invasão")) {
            return PrioridadeChamado.CRITICA;
        }

        if (texto.contains("erro grave") || texto.contains("impede trabalho") || texto.contains("audiência")
                || texto.contains("prazo fatal") || texto.contains("falha de login geral")) {
            return PrioridadeChamado.ALTA;
        }

        if (texto.contains("lentidão") || texto.contains("dúvida") || texto.contains("relatório")
                || texto.contains("configuração") || texto.contains("acesso")) {
            return PrioridadeChamado.MEDIA;
        }

        return PrioridadeChamado.BAIXA;
    }
}
