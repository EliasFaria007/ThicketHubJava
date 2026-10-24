-- ============================================================
-- V8: Adicionar domínio institucional @defensoria.mg.def.br
-- ============================================================

INSERT INTO tb_config_dominio (dominio, eh_padrao, ativo)
VALUES ('defensoria.mg.def.br', true, true)
ON CONFLICT (dominio) DO UPDATE 
SET ativo = true;

UPDATE tb_config_dominio 
SET eh_padrao = (dominio = 'defensoria.mg.def.br');
