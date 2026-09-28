package thickethub.dto.response;

public record MetricasResponse(
        long totalChamados,
        long chamadosAbertos,
        long chamadosEmAndamento,
        long chamadosResolvidos,
        long chamadosFechados,
        long chamadosCancelados,
        long chamadosPausados,
        long totalUsuarios,
        long totalTecnicos,
        long totalAdmins,
        double percentualSlaAtendido
) {}
