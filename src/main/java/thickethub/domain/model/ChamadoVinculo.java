package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.TipoVinculo;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "tb_chamado_vinculo")
public class ChamadoVinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_origem_id", nullable = false)
    private Chamado chamadoOrigem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_destino_id", nullable = false)
    private Chamado chamadoDestino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoVinculo tipo;
}
