package thickethub.dto.response;

import java.util.List;

public record RelatoriosResponse(
        long totalChamados,
        List<ItemPorSetor> porSetor,
        List<ItemSimples> porStatus,
        List<ItemPorPrioridade> porPrioridade,
        List<ItemSimples> porServico,
        List<ItemTecnico> tecnicos,
        PeriodoResumo periodo,
        TemposMedios tempos,
        UsuariosResumo usuarios,
        SatisfacaoResumo satisfacao,
        List<EncaminhamentoResumo> encaminhamentos,
        List<EvolucaoTemporal> evolucaoMensal,
        List<ItemMapaCalor> mapaCalor,
        double fcrTaxa
) {
    public record ItemMapaCalor(int diaSemana, int hora, long total) {}
    public record ItemPorSetor(String chave, long total, long resolvidos, long atrasados) {}
    public record ItemSimples(String chave, long total) {}
    public record ItemPorPrioridade(String chave, long total, long slaCumprido, long slaTotal) {}
    public record ItemTecnico(String chave, long atendidos, long emAberto, double tempoMedioResolucaoHoras, long slaCumprido, long slaTotal) {}
    public record PeriodoResumo(long resolvidos, long cancelados, long reabertos) {}
    public record TemposMedios(double medioAtendimentoHoras, double medioResolucaoHoras) {}
    public record UsuariosResumo(long cadastrados, long ativos) {}
    public record SatisfacaoResumo(double media, long avaliacoes, List<ItemDistribuicaoNota> distribuicao) {}
    public record ItemDistribuicaoNota(int nota, long total) {}
    public record EncaminhamentoResumo(String protocolo, String descricao, String autorNome, String criadoEm) {}
    public record EvolucaoTemporal(String periodo, long abertos, long resolvidos) {}
}
