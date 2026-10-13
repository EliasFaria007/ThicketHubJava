-- ============================================================
-- V4: Adição de assinatura e termos de uso em tb_usuario
-- ============================================================

ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS assinatura TEXT;
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS incluir_assinatura_padrao BOOLEAN DEFAULT TRUE;
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS termos_versao VARCHAR(30);
ALTER TABLE tb_usuario ADD COLUMN IF NOT EXISTS termos_aceitos_em TIMESTAMP;
