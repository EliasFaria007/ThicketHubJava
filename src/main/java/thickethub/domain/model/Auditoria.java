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
    @Column(name = "acao", nullable = false)
    private String acao;
    @Column(columnDefinition = "TEXT")
    private String detalhe;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "usuario_nome", nullable = false)
    private String usuarioNome;
    @Column(name = "ip_origem", nullable = false)
    private String ipOrigem;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
