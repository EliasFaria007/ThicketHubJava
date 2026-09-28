-- ============================================================
-- V1: Criação das tabelas do ThicketHub
-- ============================================================

CREATE TABLE IF NOT EXISTS tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    papel VARCHAR(30) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255),
    email VARCHAR(150) NOT NULL UNIQUE,
    setor VARCHAR(100),
    telefone VARCHAR(30),
    localidade VARCHAR(100),
    pode_cadastrar_usuarios BOOLEAN DEFAULT FALSE,
    primeiro_acesso_concluido BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_usuario_setores_liberados (
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    setores_liberados VARCHAR(100) NOT NULL,
    PRIMARY KEY (usuario_id, setores_liberados)
);

CREATE TABLE IF NOT EXISTS tb_config_dominio (
    id BIGSERIAL PRIMARY KEY,
    dominio VARCHAR(100) NOT NULL UNIQUE,
    eh_padrao BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tb_servico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    setor VARCHAR(100) NOT NULL,
    prioridade VARCHAR(30) NOT NULL,
    sla_atendimento_horas INTEGER NOT NULL DEFAULT 4,
    sla_resolucao_horas INTEGER NOT NULL DEFAULT 24,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tb_sla_config (
    id BIGSERIAL PRIMARY KEY,
    prioridade VARCHAR(30) NOT NULL UNIQUE,
    sla_atendimento_horas INTEGER NOT NULL,
    sla_resolucao_horas INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_chamado (
    id BIGSERIAL PRIMARY KEY,
    protocolo VARCHAR(50) NOT NULL UNIQUE,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    prioridade VARCHAR(30) NOT NULL,
    setor VARCHAR(100),
    fila_setor VARCHAR(100),
    servico_id BIGINT REFERENCES tb_servico(id),
    solicitante_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    tecnico_id BIGINT REFERENCES tb_usuario(id),
    versao BIGINT NOT NULL DEFAULT 0,
    data_abertura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_fechamento TIMESTAMP,
    prazo_limite TIMESTAMP,
    sla_atendimento_horas INTEGER,
    sla_resolucao_horas INTEGER,
    sla_consumido_ms BIGINT DEFAULT 0,
    sla_rodando_desde TIMESTAMP,
    primeira_resposta_em TIMESTAMP,
    o_que_foi_feito TEXT,
    resolvido_em TIMESTAMP,
    motivo_reabertura VARCHAR(255),
    avaliacao INTEGER
);

CREATE TABLE IF NOT EXISTS tb_comentario (
    id BIGSERIAL PRIMARY KEY,
    chamado_id BIGINT NOT NULL REFERENCES tb_chamado(id) ON DELETE CASCADE,
    autor_id BIGINT NOT NULL REFERENCES tb_usuario(id),
    mensagem TEXT NOT NULL,
    flag_interno BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_historico_chamado (
    id BIGSERIAL PRIMARY KEY,
    chamado_id BIGINT NOT NULL REFERENCES tb_chamado(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    descricao TEXT,
    autor_id BIGINT REFERENCES tb_usuario(id),
    autor_nome VARCHAR(150) NOT NULL,
    data_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_chamado_vinculo (
    id BIGSERIAL PRIMARY KEY,
    chamado_origem_id BIGINT NOT NULL REFERENCES tb_chamado(id) ON DELETE CASCADE,
    chamado_destino_id BIGINT NOT NULL REFERENCES tb_chamado(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_notificacao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    chamado_id BIGINT REFERENCES tb_chamado(id) ON DELETE SET NULL,
    tipo VARCHAR(50) NOT NULL,
    mensagem TEXT NOT NULL,
    mensagem_lida BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_recuperacao_sms (
    id BIGSERIAL PRIMARY KEY,
    telefone VARCHAR(30) NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    ticket_validacao VARCHAR(255) NOT NULL UNIQUE,
    expira_em TIMESTAMP NOT NULL,
    contador_tentativas INTEGER NOT NULL DEFAULT 0,
    utilizado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_auditoria (
    id BIGSERIAL PRIMARY KEY,
    acao VARCHAR(100) NOT NULL,
    detalhe TEXT,
    usuario_id BIGINT,
    usuario_nome VARCHAR(150),
    ip_origem VARCHAR(50),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
