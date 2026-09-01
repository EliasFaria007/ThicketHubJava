package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.Papel;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_usuario")
public class Usuario {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false, unique = true)
    private String email;

    private String setor;
    private String telefone;
    private String localidade;
    private Boolean podeCadastrarUsuarios;

    @Column(nullable = false)
    private Boolean primeiroAcessoConcluido;

    @Column(nullable = false)
    private Boolean ativo;

    private LocalDateTime criadoEm;

    @ElementCollection
    @CollectionTable(name="tb_usuario_setores_liberados",
        joinColumns=@JoinColumn(name="usuario_id"))
    private Set<String> setoresLiberados;

}
