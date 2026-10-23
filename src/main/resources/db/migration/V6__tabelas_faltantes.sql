-- ============================================================
-- V6: Criação das tabelas faltantes
-- ============================================================

-- Base de Conhecimento (entidade ArtigoBaseConhecimento)
CREATE TABLE IF NOT EXISTS tb_base_conhecimento (
    id              BIGSERIAL PRIMARY KEY,
    titulo          VARCHAR(200)  NOT NULL,
    slug            VARCHAR(250)  NOT NULL,
    resumo          TEXT          NOT NULL,
    conteudo        TEXT          NOT NULL,
    setor           VARCHAR(100)  NOT NULL,
    categoria       VARCHAR(100)  NOT NULL,
    tags            VARCHAR(255),
    util_sim        INTEGER       NOT NULL DEFAULT 0,
    util_nao        INTEGER       NOT NULL DEFAULT 0,
    visualizacoes   INTEGER       NOT NULL DEFAULT 0,
    autor_nome      VARCHAR(150),
    anexo_pdf_nome  VARCHAR(255),
    anexo_pdf_tamanho BIGINT,
    anexo_pdf_base64  TEXT,
    criado_em       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_base_conhecimento_setor ON tb_base_conhecimento(setor);
CREATE INDEX IF NOT EXISTS idx_base_conhecimento_slug  ON tb_base_conhecimento(slug);

-- Tabela de setores liberados por entidade (entidade UsuarioSetorLiberado)
-- A V1 já criou tb_usuario_setores_liberados para o @ElementCollection.
-- Esta tabela é mapeada pela entidade @Entity UsuarioSetorLiberado com chave surrogate.
CREATE TABLE IF NOT EXISTS tb_usuario_setor_liberado (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    setor       VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_usuario_setor_liberado_usuario_id ON tb_usuario_setor_liberado(usuario_id);
