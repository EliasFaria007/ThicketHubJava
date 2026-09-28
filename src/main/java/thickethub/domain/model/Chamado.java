package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.*;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(unique = true, nullable = false, length = 50)
    private String protocolo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusChamado status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrioridadeChamado prioridade;

    @Column(length = 100)
    private String setor;

    @Column(name = "fila_setor", length = 100)
    private String filaSetor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long versao = 0L;

    @Column(name = "data_abertura", nullable = false)
    @Builder.Default
    private LocalDateTime dataAbertura = LocalDateTime.now();

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(name = "prazo_limite")
    private LocalDateTime prazoLimite;

    @Column(name = "sla_atendimento_horas")
    private Integer slaAtendimentoHoras;

    @Column(name = "sla_resolucao_horas")
    private Integer slaResolucaoHoras;

    @Column(name = "sla_consumido_ms")
    @Builder.Default
    private Long slaConsumidoMs = 0L;

    @Column(name = "sla_rodando_desde")
    private LocalDateTime slaRodandoDesde;

    @Column(name = "primeira_resposta_em")
    private LocalDateTime primeiraRespostaEm;

    @Column(name = "o_que_foi_feito", columnDefinition = "TEXT")
    private String oQueFoiFeito;

    @Column(name = "resolvido_em")
    private LocalDateTime resolvidoEm;

    @Column(name = "motivo_reabertura")
    private String motivoReabertura;

    private Integer avaliacao;

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<HistoricoChamado> historico = new ArrayList<>();

    public LocalDateTime getCriadoEm() {
        return dataAbertura;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.dataAbertura = criadoEm;
    }
}
