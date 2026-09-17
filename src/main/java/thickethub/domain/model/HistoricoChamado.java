package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.StatusChamado;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_historico_chamado")
public class HistoricoChamado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chamado_id",nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEventoChamado tipo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name="autor_nome",nullable = false)
    private String autorNome;

    @Column(name="data_evento",nullable = false)
    private LocalDateTime dataEvento;
}
