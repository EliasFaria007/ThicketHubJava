package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade para persistência das configurações corporativas do sistema
 * (LDAP, E-mail, Geral) no banco de dados.
 * <p>
 * Cada registro representa uma categoria de configuração, armazenada como JSON.
 * Chaves esperadas: "ldap", "email", "geral"
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_configuracao_sistema")
public class ConfiguracaoSistema {

    /** Chave única da categoria (ex: "ldap", "email", "geral") */
    @Id
    @Column(name = "chave", length = 100)
    private String chave;

    /** Valor serializado em JSON */
    @Column(name = "valor", columnDefinition = "TEXT", nullable = false)
    private String valor;

    /** Descrição legível da configuração */
    @Column(name = "descricao", length = 255)
    private String descricao;

    /** Data/hora da última atualização */
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    void atualizarTimestamp() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
