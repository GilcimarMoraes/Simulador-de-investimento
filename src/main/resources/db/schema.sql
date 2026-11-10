-- Schema do banco SQLite do Painel de Investimentos.
-- Valores monetarios sao persistidos em centavos (INTEGER) e taxas em basis points
-- (INTEGER, 1200 = 12% a.a.) para evitar erro de ponto flutuante em calculos financeiros.

CREATE TABLE IF NOT EXISTS cliente (
    id        INTEGER PRIMARY KEY,
    nome      TEXT    NOT NULL,
    criado_em TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS produto (
    id                  INTEGER PRIMARY KEY,
    nome                TEXT    NOT NULL UNIQUE,
    tipo                TEXT    NOT NULL,
    rentabilidade_bp    INTEGER NOT NULL,
    risco               TEXT    NOT NULL,
    liquidez            TEXT    NOT NULL,
    prazo_min_meses     INTEGER NOT NULL,
    prazo_max_meses     INTEGER,
    valor_min_centavos  INTEGER NOT NULL,
    valor_max_centavos  INTEGER,
    isento_ir           INTEGER NOT NULL DEFAULT 0,
    carencia_meses      INTEGER NOT NULL DEFAULT 0,
    ativo               INTEGER NOT NULL DEFAULT 1,
    descricao           TEXT,
    CONSTRAINT ck_produto_risco    CHECK (risco IN ('BAIXO', 'MEDIO', 'ALTO')),
    CONSTRAINT ck_produto_liquidez CHECK (liquidez IN ('DIARIA', 'VENCIMENTO')),
    CONSTRAINT ck_produto_prazo    CHECK (prazo_min_meses > 0 AND (prazo_max_meses IS NULL OR prazo_max_meses >= prazo_min_meses)),
    CONSTRAINT ck_produto_valor    CHECK (valor_min_centavos > 0 AND (valor_max_centavos IS NULL OR valor_max_centavos >= valor_min_centavos))
);

CREATE INDEX IF NOT EXISTS ix_produto_tipo ON produto (tipo, ativo);

CREATE TABLE IF NOT EXISTS investimento (
    id                INTEGER PRIMARY KEY,
    cliente_id        INTEGER NOT NULL REFERENCES cliente (id),
    tipo              TEXT    NOT NULL,
    valor_centavos    INTEGER NOT NULL,
    rentabilidade_bp  INTEGER NOT NULL,
    liquidez          TEXT    NOT NULL,
    data              TEXT    NOT NULL,
    CONSTRAINT ck_investimento_liquidez CHECK (liquidez IN ('DIARIA', 'VENCIMENTO'))
);

CREATE INDEX IF NOT EXISTS ix_investimento_cliente ON investimento (cliente_id);

CREATE TABLE IF NOT EXISTS movimentacao (
    id             INTEGER PRIMARY KEY,
    cliente_id     INTEGER NOT NULL REFERENCES cliente (id),
    tipo           TEXT    NOT NULL,
    valor_centavos INTEGER NOT NULL,
    data           TEXT    NOT NULL,
    CONSTRAINT ck_movimentacao_tipo CHECK (tipo IN ('APORTE', 'RESGATE'))
);

CREATE INDEX IF NOT EXISTS ix_movimentacao_cliente_data ON movimentacao (cliente_id, data);

CREATE TABLE IF NOT EXISTS simulacao (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id                   INTEGER NOT NULL REFERENCES cliente (id),
    produto_id                   INTEGER NOT NULL REFERENCES produto (id),
    produto_nome                 TEXT    NOT NULL,
    tipo_produto                 TEXT    NOT NULL,
    valor_investido_centavos     INTEGER NOT NULL,
    valor_final_centavos         INTEGER NOT NULL,
    valor_final_liquido_centavos INTEGER NOT NULL,
    imposto_centavos             INTEGER NOT NULL,
    rentabilidade_efetiva_bp     INTEGER NOT NULL,
    prazo_meses                  INTEGER NOT NULL,
    perfil_cliente               TEXT    NOT NULL,
    data_simulacao               TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_simulacao_cliente_data ON simulacao (cliente_id, data_simulacao);
CREATE INDEX IF NOT EXISTS ix_simulacao_produto_data ON simulacao (produto_id, data_simulacao);
CREATE INDEX IF NOT EXISTS ix_simulacao_data ON simulacao (data_simulacao);

CREATE TABLE IF NOT EXISTS requisicao_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    servico    TEXT    NOT NULL,
    metodo     TEXT    NOT NULL,
    caminho    TEXT    NOT NULL,
    status     INTEGER NOT NULL,
    duracao_ms INTEGER NOT NULL,
    usuario    TEXT,
    data_hora  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_requisicao_servico_data ON requisicao_log (servico, data_hora);

CREATE TABLE IF NOT EXISTS usuario (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT    NOT NULL UNIQUE,
    senha_hash TEXT    NOT NULL,
    roles      TEXT    NOT NULL,
    cliente_id INTEGER REFERENCES cliente (id),
    ativo      INTEGER NOT NULL DEFAULT 1
);
