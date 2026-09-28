-- ============================================================
-- V2: Dados Iniciais (SLA, Domínios e Administrador Padrão)
-- ============================================================

INSERT INTO tb_config_dominio (dominio, eh_padrao, ativo)
VALUES 
    ('defensoria.mg.gov.br', true, true),
    ('dpmg.def.br', false, true)
ON CONFLICT (dominio) DO NOTHING;

INSERT INTO tb_sla_config (prioridade, sla_atendimento_horas, sla_resolucao_horas)
VALUES
    ('BAIXA', 8, 48),
    ('MEDIA', 4, 24),
    ('ALTA', 2, 8),
    ('CRITICA', 1, 4)
ON CONFLICT (prioridade) DO NOTHING;

-- Senha padrão para o admin: Admin@123 (BCrypt hash)
INSERT INTO tb_usuario (papel, nome, senha_hash, email, setor, primeiro_acesso_concluido, ativo, pode_cadastrar_usuarios)
VALUES
    ('ADMIN', 'Administrador do Sistema', '$2a$12$K8yFkE.Rvgv.k2/Kx8hAeuKkM2oY3B0YcWfZW9f1d0QZfJbEqfN2K', 'admin@defensoria.mg.gov.br', 'TI', true, true, true)
ON CONFLICT (email) DO NOTHING;
