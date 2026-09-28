package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.TipoHistorico;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TipoHistorico tipo = TipoHistorico.STATUS;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Column(name = "autor_nome", nullable = false, length = 150)
    private String autorNome;

    @Column(name = "data_evento", nullable = false)
    @Builder.Default
    private LocalDateTime dataEvento = LocalDateTime.now();

    public LocalDateTime getCriadoEm() {
        return dataEvento;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.dataEvento = criadoEm;
    }

    public static class HistoricoChamadoBuilder {
        public HistoricoChamadoBuilder autor(Usuario autor) {
            this.autor = autor;
            if (autor != null && this.autorNome == null) {
                this.autorNome = autor.getNome();
            }
            return this;
        }

        public HistoricoChamadoBuilder criadoEm(LocalDateTime criadoEm) {
            this.dataEvento = criadoEm;
            return this;
        }
    }
}
