-- ============================================================
-- V5: Índices de Alta Performance para tb_chamado e tb_notificacao
-- ============================================================

-- Acelera consultas de chamados do solicitante logado
CREATE INDEX IF NOT EXISTS idx_chamado_solicitante_id ON tb_chamado(solicitante_id);

-- Acelera consultas e contagens da fila do técnico e status
CREATE INDEX IF NOT EXISTS idx_chamado_tecnico_status ON tb_chamado(tecnico_id, status);

-- Acelera filtros combinados de status e setor (listagem da fila do setor)
CREATE INDEX IF NOT EXISTS idx_chamado_status_setor ON tb_chamado(status, setor);

-- Acelera a ordenação padrão por data de abertura (mais recentes primeiro)
CREATE INDEX IF NOT EXISTS idx_chamado_data_abertura_desc ON tb_chamado(data_abertura DESC);

-- Acelera busca rápida por serviço
CREATE INDEX IF NOT EXISTS idx_chamado_servico_id ON tb_chamado(servico_id);

-- Acelera busca por protocolo
CREATE INDEX IF NOT EXISTS idx_chamado_protocolo ON tb_chamado(protocolo);

-- Acelera listagem e contagem de notificações não lidas por usuário
CREATE INDEX IF NOT EXISTS idx_notificacao_usuario_lida ON tb_notificacao(usuario_id, mensagem_lida);
