package thickethub.dto.response;

import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Chamado;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoDetalheResponse(
        Long id,
        String protocolo,
        String titulo,
        String descricao,
        StatusChamado status,
        PrioridadeChamado prioridade,
        String setor,
        String filaSetor,
        Long servicoId,
        String servicoNome,
        Long solicitanteId,
        String solicitanteNome,
        String solicitanteEmail,
        Long tecnicoId,
        String tecnicoNome,
        Long versao,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        LocalDateTime prazoLimite,
        Integer slaAtendimentoHoras,
        Integer slaResolucaoHoras,
        Long slaConsumidoMs,
        LocalDateTime primeiraRespostaEm,
        String oQueFoiFeito,
        LocalDateTime resolvidoEm,
        String motivoReabertura,
        Integer avaliacao,
        List<ComentarioResponse> comentarios,
        List<HistoricoResponse> historico
) {
    public static ChamadoDetalheResponse de(Chamado chamado) {
        if (chamado == null) return null;

        List<ComentarioResponse> coms = chamado.getComentarios() != null
                ? chamado.getComentarios().stream().map(ComentarioResponse::de).toList()
                : List.of();

        List<HistoricoResponse> hists = chamado.getHistorico() != null
                ? chamado.getHistorico().stream().map(HistoricoResponse::de).toList()
                : List.of();

        return new ChamadoDetalheResponse(
                chamado.getId(),
                chamado.getProtocolo(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus(),
                chamado.getPrioridade(),
                chamado.getSetor(),
                chamado.getFilaSetor(),
                chamado.getServico() != null ? chamado.getServico().getId() : null,
                chamado.getServico() != null ? chamado.getServico().getNome() : null,
                chamado.getSolicitante() != null ? chamado.getSolicitante().getId() : null,
                chamado.getSolicitante() != null ? chamado.getSolicitante().getNome() : null,
                chamado.getSolicitante() != null ? chamado.getSolicitante().getEmail() : null,
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                chamado.getTecnico() != null ? chamado.getTecnico().getNome() : null,
                chamado.getVersao(),
                chamado.getDataAbertura(),
                chamado.getDataFechamento(),
                chamado.getPrazoLimite(),
                chamado.getSlaAtendimentoHoras(),
                chamado.getSlaResolucaoHoras(),
                chamado.getSlaConsumidoMs(),
                chamado.getPrimeiraRespostaEm(),
                chamado.getOQueFoiFeito(),
                chamado.getResolvidoEm(),
                chamado.getMotivoReabertura(),
                chamado.getAvaliacao(),
                coms,
                hists
        );
    }
}
