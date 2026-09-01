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
    private StatusChamado status;
    private PrioridadeChamado prioridade;
    private LocalDateTime dataAbertura;
    @Column(unique = true)
    private String protocolo;
            titulo,
            descricao,
            categoria,
            subcategoria,
            setorResponsavel,
            usuarioSolicitante,
            dataFechamento,
            filaSetor,
            setor;
    @ManyToOne
    private Servico servico;
    @ManyToOne
    private Solicitante solicitante;
    @ManyToOne
    private Tecnico tecnico;
    @Version Long versao;
    private String slaAtendimentoHoras,
            slaConsumidoMs,
            slaResolucaoHoras,
            slaRodandoDesde,
            primeiraRespostaEm,
            oQueFoiFeito,
            resolvidoEm,
            motivoReabertura,
            avaliacao;

    @ElementCollection
    @CollectionTable(name="tb_chamado_comentarios",
            joinColumns=@JoinColumn(name="chamado_id"))
    private Set<String> comentarios;

    @ElementCollection
    @CollectionTable(name="tb_chamado_historico",
            joinColumns=@JoinColumn(name="chamado_id"))
    private Set<String> historico;

}
