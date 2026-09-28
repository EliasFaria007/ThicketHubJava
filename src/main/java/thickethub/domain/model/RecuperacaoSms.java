package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Builder
@Table(name = "tb_recuperacao_sms")
public class RecuperacaoSms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String telefone;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "ticket_validacao", nullable = false, unique = true)
    private String ticketValidacao;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "contador_tentativas", nullable = false)
    @Builder.Default
    private Integer contadorTentativas = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean utilizado = false;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();
}
