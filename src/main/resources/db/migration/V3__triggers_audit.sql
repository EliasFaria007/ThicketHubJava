-- ============================================================
-- V3: Índices para otimização de consultas e auditoria
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_chamado_solicitante ON tb_chamado(solicitante_id);
CREATE INDEX IF NOT EXISTS idx_chamado_tecnico ON tb_chamado(tecnico_id);
CREATE INDEX IF NOT EXISTS idx_chamado_status ON tb_chamado(status);
CREATE INDEX IF NOT EXISTS idx_chamado_setor ON tb_chamado(setor);
CREATE INDEX IF NOT EXISTS idx_chamado_data_abertura ON tb_chamado(data_abertura);

CREATE INDEX IF NOT EXISTS idx_notificacao_usuario ON tb_notificacao(usuario_id, mensagem_lida);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario ON tb_auditoria(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_criado_em ON tb_auditoria(criado_em);
