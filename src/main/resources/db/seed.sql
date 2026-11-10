-- Dados de demonstracao. Idempotente: ids fixos + INSERT OR IGNORE.
-- Datas relativas a data de inicializacao para manter o criterio de frequencia (180 dias) valido.

INSERT OR IGNORE INTO cliente (id, nome, criado_em) VALUES
    (1,   'Ana Beatriz Lima',    datetime('now', '-900 days')),
    (2,   'Bruno Carvalho',      datetime('now', '-1200 days')),
    (3,   'Cliente Novo',        datetime('now', '-2 days')),
    (123, 'Carlos Eduardo Souza', datetime('now', '-800 days'));

INSERT OR IGNORE INTO produto
    (id, nome, tipo, rentabilidade_bp, risco, liquidez, prazo_min_meses, prazo_max_meses,
     valor_min_centavos, valor_max_centavos, isento_ir, carencia_meses, ativo, descricao)
VALUES
    (101, 'CDB Caixa 2026', 'CDB', 1200, 'BAIXO', 'DIARIA', 1, 60,
     100000, 100000000, 0, 0, 1,
     'CDB com liquidez diária e taxa fixa de 12% ao ano.'),
    (102, 'Fundo XPTO Multimercado', 'FUNDO', 1800, 'ALTO', 'VENCIMENTO', 6, 60,
     500000, 500000000, 0, 0, 1,
     'Fundo multimercado com estratégias de maior risco e retorno potencial.'),
    (103, 'LCI Caixa Imobiliária', 'LCI', 1050, 'BAIXO', 'VENCIMENTO', 3, 48,
     500000, 300000000, 1, 3, 1,
     'Letra de Crédito Imobiliário isenta de IR, com carência de 3 meses.'),
    (104, 'LCA Caixa Agro', 'LCA', 1100, 'BAIXO', 'VENCIMENTO', 3, 36,
     500000, 300000000, 1, 3, 1,
     'Letra de Crédito do Agronegócio isenta de IR, com carência de 3 meses.'),
    (105, 'Tesouro Selic 2029', 'TESOURO', 1100, 'BAIXO', 'DIARIA', 1, 48,
     10000, 100000000, 0, 0, 1,
     'Título público pós-fixado com alta liquidez.'),
    (106, 'Tesouro IPCA+ 2035', 'TESOURO', 1350, 'MEDIO', 'VENCIMENTO', 12, 120,
     10000, 100000000, 0, 0, 1,
     'Título público indexado à inflação, com risco de marcação a mercado.'),
    (107, 'Fundo Ações Caixa', 'FUNDO', 2200, 'ALTO', 'VENCIMENTO', 12, 120,
     100000, 100000000, 0, 0, 1,
     'Fundo de ações com alta volatilidade e potencial de retorno elevado.'),
    (108, 'CDB Caixa Liquidez Diária', 'CDB', 1000, 'BAIXO', 'DIARIA', 1, 24,
     50000, 5000000, 0, 0, 1,
     'CDB de curto prazo com resgate diário para reserva de emergência.'),
    (109, 'Fundo Renda Fixa Curto Prazo', 'FUNDO', 950, 'MEDIO', 'DIARIA', 1, 24,
     100000, 50000000, 0, 0, 1,
     'Fundo conservador de renda fixa com liquidez diária.'),
    (110, 'CDB Caixa Premium 2028', 'CDB', 1250, 'MEDIO', 'VENCIMENTO', 12, 36,
     5000000, 500000000, 0, 0, 1,
     'CDB exclusivo para aportes a partir de R$ 50 mil, com taxa diferenciada.'),
    (111, 'CDB Caixa Legado', 'CDB', 900, 'BAIXO', 'DIARIA', 1, 12,
     10000, 1000000, 0, 0, 0,
     'Produto descontinuado, mantido apenas para histórico.');

-- Cliente 123: perfil Moderado (65 pontos) — patrimônio de R$ 308 mil, 5 movimentações
-- nos últimos 180 dias e 40% do patrimônio em ativos sem liquidez diária.
INSERT OR IGNORE INTO investimento (id, cliente_id, tipo, valor_centavos, rentabilidade_bp, liquidez, data) VALUES
    (101, 123, 'CDB',              8000000, 1200, 'DIARIA',     date('now', '-420 days')),
    (102, 123, 'Tesouro Selic',   10000000, 1100, 'DIARIA',     date('now', '-300 days')),
    (103, 123, 'Fundo',           12000000, 1800, 'VENCIMENTO', date('now', '-150 days')),
    (104, 123, 'CDB',               500000, 1200, 'DIARIA',     date('now', '-90 days')),
    (105, 123, 'Fundo',             300000, 1800, 'VENCIMENTO', date('now', '-30 days'));

INSERT OR IGNORE INTO movimentacao (id, cliente_id, tipo, valor_centavos, data) VALUES
    (1001, 123, 'APORTE',  500000, date('now', '-5 days')),
    (1002, 123, 'APORTE', 1000000, date('now', '-20 days')),
    (1003, 123, 'RESGATE', 150000, date('now', '-40 days')),
    (1004, 123, 'APORTE',  250000, date('now', '-70 days')),
    (1005, 123, 'APORTE',  900000, date('now', '-120 days'));

-- Cliente 1: Conservador (30 pontos) — baixo volume, 100% líquido, poucas movimentações.
INSERT OR IGNORE INTO investimento (id, cliente_id, tipo, valor_centavos, rentabilidade_bp, liquidez, data) VALUES
    (11, 1, 'CDB',            2000000, 1000, 'DIARIA', date('now', '-365 days')),
    (12, 1, 'Tesouro Selic',  1000000, 1100, 'DIARIA', date('now', '-200 days'));

INSERT OR IGNORE INTO movimentacao (id, cliente_id, tipo, valor_centavos, data) VALUES
    (2001, 1, 'APORTE', 300000, date('now', '-15 days')),
    (2002, 1, 'APORTE', 200000, date('now', '-100 days'));

-- Cliente 2: Agressivo (96 pontos) — alto volume, 100% ilíquido, 15 movimentações.
INSERT OR IGNORE INTO investimento (id, cliente_id, tipo, valor_centavos, rentabilidade_bp, liquidez, data) VALUES
    (21, 2, 'Fundo Multimercado', 50000000, 1800, 'VENCIMENTO', date('now', '-400 days')),
    (22, 2, 'Fundo Ações',        30000000, 2200, 'VENCIMENTO', date('now', '-220 days'));

WITH RECURSIVE seq(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 15
)
INSERT OR IGNORE INTO movimentacao (id, cliente_id, tipo, valor_centavos, data)
SELECT 3000 + n,
       2,
       CASE WHEN n % 5 = 0 THEN 'RESGATE' ELSE 'APORTE' END,
       500000 + (n * 25000),
       date('now', '-' || (n * 11) || ' days')
FROM seq;
