-- Amplia tamanho da coluna telefone para comportar tanto telefones quanto emails
ALTER TABLE tb_recuperacao_sms ALTER COLUMN telefone TYPE VARCHAR(255);
ALTER TABLE tb_recuperacao_sms ADD COLUMN IF NOT EXISTS email VARCHAR(255);
