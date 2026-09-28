package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.PrioridadeChamado;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_sla_config")
public class SlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private PrioridadeChamado prioridade;

    @Column(name = "sla_atendimento_horas", nullable = false)
    private Integer slaAtendimentoHoras;

    @Column(name = "sla_resolucao_horas", nullable = false)
    private Integer slaResolucaoHoras;

    public int getHorasUteis() {
        return slaResolucaoHoras != null ? slaResolucaoHoras : 24;
    }
}
