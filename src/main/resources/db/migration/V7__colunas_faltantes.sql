-- ============================================================
-- V7: Colunas faltantes detectadas na validação do Hibernate
-- ============================================================

-- tb_servico: FK para SlaConfig (adicionada na entidade mas ausente no schema)
ALTER TABLE tb_servico ADD COLUMN IF NOT EXISTS sla_config_id BIGINT REFERENCES tb_sla_config(id);
