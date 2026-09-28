package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import thickethub.domain.enums.Papel;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "tb_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Papel papel;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 100)
    private String setor;

    @Column(length = 30)
    private String telefone;

    @Column(length = 100)
    private String localidade;

    @Column(name = "pode_cadastrar_usuarios")
    @Builder.Default
    private Boolean podeCadastrarUsuarios = false;

    @Column(name = "primeiro_acesso_concluido", nullable = false)
    @Builder.Default
    private Boolean primeiroAcessoConcluido = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_usuario_setores_liberados",
            joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "setores_liberados")
    @Builder.Default
    private Set<String> setoresLiberados = new HashSet<>();
}
