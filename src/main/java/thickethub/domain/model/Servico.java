package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.PrioridadeChamado;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "tb_servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 100)
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrioridadeChamado prioridade;

    @Column(name = "sla_atendimento_horas", nullable = false)
    @Builder.Default
    private Integer slaAtendimentoHoras = 4;

    @Column(name = "sla_resolucao_horas", nullable = false)
    @Builder.Default
    private Integer slaResolucaoHoras = 24;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_config_id")
    private SlaConfig slaConfig;

    public PrioridadeChamado getPrioridadePadrao() {
        return prioridade != null ? prioridade : PrioridadeChamado.MEDIA;
    }
}
