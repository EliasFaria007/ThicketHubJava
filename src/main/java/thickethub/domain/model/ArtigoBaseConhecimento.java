package thickethub.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_base_conhecimento")
public class ArtigoBaseConhecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 250)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String resumo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(nullable = false, length = 100)
    private String setor;

    @Column(nullable = false, length = 100)
    private String categoria;

    @Column(length = 255)
    private String tags;

    @Builder.Default
    @Column(name = "util_sim", nullable = false)
    private Integer utilSim = 0;

    @Builder.Default
    @Column(name = "util_nao", nullable = false)
    private Integer utilNao = 0;

    @Builder.Default
    @Column(name = "visualizacoes", nullable = false)
    private Integer visualizacoes = 0;

    @Column(name = "autor_nome", length = 150)
    private String autorNome;

    @Column(name = "anexo_pdf_nome", length = 255)
    private String anexoPdfNome;

    @Column(name = "anexo_pdf_tamanho")
    private Long anexoPdfTamanho;

    @Column(name = "anexo_pdf_base64", columnDefinition = "TEXT")
    private String anexoPdfBase64;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
        if (atualizadoEm == null) atualizadoEm = LocalDateTime.now();
        if (utilSim == null) utilSim = 0;
        if (utilNao == null) utilNao = 0;
        if (visualizacoes == null) visualizacoes = 0;
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
