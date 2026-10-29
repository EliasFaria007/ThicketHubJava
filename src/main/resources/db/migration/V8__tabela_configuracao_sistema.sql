-- ============================================================
-- V8: Tabela para persistência de configurações corporativas (LDAP, E-mail, Geral)
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_configuracao_sistema (
    chave       VARCHAR(100) PRIMARY KEY,
    valor       TEXT         NOT NULL,
    descricao   VARCHAR(255),
    atualizado_em TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para buscas por prefixo de chave (ex: 'ldap.%', 'email.%')
CREATE INDEX IF NOT EXISTS idx_configuracao_sistema_chave ON tb_configuracao_sistema(chave);
