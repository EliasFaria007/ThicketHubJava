package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_comentario")
public class Comentario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Chamado chamado;
    @ManyToOne
    private Usuario autor;

    @Column(name="mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name="flag_interno")
    private Boolean flagInterno;

    @Column(name="data_criacao")
    private LocalDateTime dataCriacao;

}
