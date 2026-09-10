package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.*;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_chamado")
public class Chamado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusChamado status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeChamado prioridade;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataFechamento;

    @Column(unique = true,nullable = false)
    private String protocolo;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    private String setor;

    @ManyToOne
    private Servico servico;
    @ManyToOne
    private Solicitante solicitante;
    @ManyToOne
    private Tecnico tecnico;
    @Version Long versao;

    private Integer slaAtendimentoHoras;
    private Long slaConsumidoMs;
    private Integer slaResolucaoHoras;
    private LocalDateTime slaRodandoDesde;
    private LocalDateTime primeiraRespostaEm;

    @Column(columnDefinition = "TEXT")
    private String oQueFoiFeito;

    private LocalDateTime resolvidoEm;
    private String motivoReabertura;
    private Integer avaliacao;

    @ElementCollection
    @CollectionTable(name="tb_chamado_comentarios",
            joinColumns=@JoinColumn(name="chamado_id"))
    private Set<String> comentarios;

    @ElementCollection
    @CollectionTable(name="tb_chamado_historico",
            joinColumns=@JoinColumn(name="chamado_id"))
    private Set<String> historico;

}
