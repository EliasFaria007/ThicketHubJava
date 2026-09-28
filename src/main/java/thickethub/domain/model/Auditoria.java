package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "acao", nullable = false, length = 100)
    private String acao;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nome", length = 150)
    private String usuarioNome;

    @Column(name = "ip_origem", length = 50)
    @Builder.Default
    private String ipOrigem = "0.0.0.0";

    @Column(name = "criado_em", nullable = false)
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    public static class AuditoriaBuilder {
        public AuditoriaBuilder usuario(Usuario usuario) {
            if (usuario != null) {
                this.usuarioId = usuario.getId();
                this.usuarioNome = usuario.getNome();
            }
            return this;
        }
    }
}
