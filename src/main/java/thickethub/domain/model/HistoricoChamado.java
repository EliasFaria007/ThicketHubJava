package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.StatusChamado;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_historico_chamado")
public class HistoricoChamado extends Chamado{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public static void auteracao{

        if (this.status != new status){
            histChamado = new HistoricoChamado(
                    this.id,
                    this.status,
                    this.tipo,descricao,autorNamo
            );
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusChamado status;


}
