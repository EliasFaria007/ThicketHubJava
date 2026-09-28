package thickethub.dto.response;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.model.Servico;

public record ServicoResponse(
        Long id,
        String nome,
        String setor,
        PrioridadeChamado prioridade,
        Integer slaAtendimentoHoras,
        Integer slaResolucaoHoras,
        boolean ativo
) {
    public static ServicoResponse de(Servico s) {
        if (s == null) return null;
        return new ServicoResponse(
                s.getId(),
                s.getNome(),
                s.getSetor(),
                s.getPrioridade(),
                s.getSlaAtendimentoHoras(),
                s.getSlaResolucaoHoras(),
                s.isAtivo()
        );
    }
}
