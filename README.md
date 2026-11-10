# Painel de Investimentos com Perfil de Risco Dinâmico

API REST para simulação de investimentos que calcula o **perfil de risco do cliente** a partir do
seu comportamento financeiro, recomenda produtos aderentes e persiste cada simulação realizada.

Projeto entregue para o desafio técnico de back-end (Java), implementado com **Quarkus 3.40.1**,
**Java 25** e **PostgreSQL 17**.

---

## Sumário

1. [Visão geral](#visão-geral)
2. [Stack e arquitetura](#stack-e-arquitetura)
3. [Como executar](#como-executar)
4. [Autenticação](#autenticação)
5. [Endpoints](#endpoints)
6. [Motor de recomendação (perfil de risco)](#motor-de-recomendação-perfil-de-risco)
7. [Regras de simulação](#regras-de-simulação)
8. [Modelo de dados](#modelo-de-dados)
9. [Segurança](#segurança)
10. [Telemetria](#telemetria)
11. [Testes](#testes)
12. [Estrutura do projeto](#estrutura-do-projeto)
13. [Decisões e trade-offs](#decisões-e-trade-offs)
14. [Evoluções sugeridas](#evoluções-sugeridas)
15. [Evidências](#evidências)

---

## Visão geral

A API cobre os requisitos do desafio:

| Requisito | Implementação |
|---|---|
| Receber envelope JSON de simulação | `POST /simular-investimento` com Bean Validation |
| Consultar parâmetros em banco SQL | Tabela `produto` no PostgreSQL (limites de valor, prazo, taxa, risco, liquidez) |
| Validar entrada com base nos parâmetros | Validação em duas camadas: sintática (Bean Validation) e de negócio (limites do produto no banco) |
| Filtrar produto adequado | Filtro por tipo/valor/prazo + ranking por aderência ao perfil e rentabilidade líquida |
| Calcular simulações | Juros compostos + IR regressivo + isenção de LCI/LCA |
| Retornar nome do produto e resultado | Envelope `produtoValidado` + `resultadoSimulacao` |
| Persistir simulação | Tabela `simulacao` (com IR, valor líquido e perfil no momento da simulação) |
| Listar simulações | `GET /simulacoes` (filtro opcional por cliente) |
| Valores por produto e dia | `GET /simulacoes/por-produto-dia` |
| Telemetria de volume e latência | `GET /telemetria` (média, p95, máximo e erros por serviço) |
| Autenticação | JWT RS256 emitido pela própria API (`POST /auth/token`) |
| Execução em container | `Dockerfile` multi-stage + `docker-compose.yml` |
| Motor de recomendação | Pontuação de 0 a 100 em quatro critérios comportamentais |

---

## Stack e arquitetura

- **Java 25** (Temurin 25.0.5) e **Quarkus 3.40.1**
- **RESTEasy Reactive + Jackson** (`quarkus-rest-jackson`)
- **PostgreSQL 17** com **Agroal + Narayana JTA**, JDBC puro e driver oficial
  (`org.postgresql:postgresql`); em dev/test o **Dev Services** sobe o banco automaticamente
- **SmallRye JWT** (emissão e validação RS256) e **Hibernate Validator**
- **SmallRye OpenAPI + Swagger UI** e **SmallRye Health**
- **JUnit 5 + RestAssured** para testes unitários e de integração
- **Docker** multi-stage (build com JDK 25, runtime com JRE 25 sem ferramentas de build)

A aplicação é organizada em camadas simples e explícitas, sem mágica de ORM:

```
                ┌──────────────────────────────────────────────┐
 HTTP + JWT     │ api/                                         │
 (Bearer)       │  Auth, Simulacao, Perfil, Produto,           │
                │  Investimento, Telemetria + Problem Details  │
                └───────────────────────┬──────────────────────┘
                                        │
                ┌───────────────────────▼──────────────────────┐
 Regras         │ servico/                                     │
                │  SimulacaoService · MotorPerfilRisco         │
                │  CalculadoraInvestimento · ProdutoService    │
                └───────────────────────┬──────────────────────┘
                                        │
                ┌───────────────────────▼──────────────────────┐
 Persistência   │ persistencia/ (JDBC puro, SQL parametrizado) │
                │ infra/ (bootstrap do schema e seed)          │
                └───────────────────────┬──────────────────────┘
                                        │
                              PostgreSQL 17 (JDBC)
```

Decisões de design relevantes:

- **Domínio puro e testável**: `MotorPerfilRisco` e `CalculadoraInvestimento` são funções sem
  dependência de banco, HTTP ou relógio — por isso são cobertas por testes unitários rápidos.
- **Sem ORM**: as consultas são explícitas e o schema é versionado em SQL (`db/schema.sql`),
  o que mantém o startup rápido e o comportamento previsível.
- **Erros padronizados**: toda falha de negócio/aplicação retorna *Problem Details* (RFC 9457)
  com `requestId` para correlação.

---

## Como executar

### Opção 1 — Docker Compose (recomendado)

Pré-requisitos: Docker com Compose v2.

```bash
docker compose up --build
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/q/swagger-ui>
- Health: <http://localhost:8080/q/health/ready>

O Compose sobe dois serviços: `postgres` (PostgreSQL 17) e `api`. O banco usa o volume nomeado
`painel-investimentos-postgres`, portanto os dados sobrevivem a `docker compose down` (use
`docker compose down -v` para apagar). O PostgreSQL também fica exposto em
`localhost:15433` para inspeção com `psql` ou clientes gráficos.

### Opção 2 — Local

Pré-requisitos: **JDK 25** e Maven 3.9+.

```bash
export JAVA_HOME=/caminho/para/jdk-25
mvn quarkus:dev          # modo dev, porta 8080
# ou
mvn clean package && java -jar target/quarkus-app/quarkus-run.jar
```

O modo `dev` usa **Dev Services**: se `QUARKUS_DATASOURCE_JDBC_URL` não estiver configurada, o
Quarkus sobe um container PostgreSQL descartável automaticamente (requer Docker). Para usar um
banco fixo, suba apenas o serviço de banco do Compose e exporte as variáveis:

```bash
docker compose up -d postgres
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:15433/investimentos
export QUARKUS_DATASOURCE_USERNAME=investimentos
export QUARKUS_DATASOURCE_PASSWORD=investimentos
mvn quarkus:dev
```

Sem Docker, aponte as mesmas variáveis para um PostgreSQL existente: o schema e o seed são
criados automaticamente no primeiro start.

### Opção 3 — Makefile

```bash
make build        # empacota (com testes)
make test         # somente testes
make run          # quarkus:dev
make docker-up    # compose
make smoke        # smoke test contra localhost:8080
```

### Smoke test

Com a aplicação no ar:

```bash
./scripts/smoke-test.sh
# ou
./scripts/smoke-test.sh http://localhost:8080
```

O script valida autenticação, os sete endpoints do desafio, persistência, agregação, telemetria e
os principais cenários de erro (401, 400, 404, 422, 403). Requer `curl` e `jq`.

---

## Autenticação

A API usa **JWT RS256** emitido por `POST /auth/token`. As senhas são armazenadas com
**PBKDF2-HMAC-SHA256** (210.000 iterações, salt aleatório por usuário).

### Usuários de demonstração (seed)

| Usuário | Senha | Papéis | Cliente vinculado |
|---|---|---|---|
| `demo` | `demo123` | `USER` | 123 |
| `admin` | `admin123` | `ADMIN`, `USER` | 123 |
| `ana` | `ana123` | `USER` | 1 |

> **Atenção:** credenciais e par de chaves JWT de desenvolvimento estão versionados apenas para
> facilitar a avaliação. Em produção, use variáveis de ambiente/secret manager e remova o seed.

### Obtendo um token

```bash
curl -s -X POST http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo123"}'
```

```json
{
  "accessToken": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "usuario": "demo",
  "roles": ["USER"],
  "clienteId": 123,
  "expiraEm": "2026-11-10T15:05:26Z"
}
```

O token contém `iss`, `sub`, `upn`, `groups` (papéis), `cliente_id`, `iat`, `exp` e `jti`.
`GET /auth/me` devolve os dados da sessão.

### Proteção dos endpoints

- Todos os endpoints de negócio exigem `Authorization: Bearer <token>`.
- `GET /telemetria` exige o papel **ADMIN**; com `USER` retorna **403**.
- Token ausente, inválido ou expirado retorna **401** com o cabeçalho `WWW-Authenticate: Bearer`
  (comportamento padrão da camada de segurança do Quarkus, conforme RFC 6750).
- Demais erros retornam Problem Details com `requestId`.

### Chaves JWT

- Chave privada: `src/main/resources/jwt/privateKey.pem` (`smallrye.jwt.sign.key.location`)
- Chave pública: `src/main/resources/jwt/publicKey.pem` (`mp.jwt.verify.publickey.location`)

Em produção, sobrescreva com:

```bash
export SMALLRYE_JWT_SIGN_KEY_LOCATION=/run/secrets/jwt-private.pem
export MP_JWT_VERIFY_PUBLICKEY_LOCATION=/run/secrets/jwt-public.pem
```

Para integrar com **Keycloak/OAuth2**, basta apontar `mp.jwt.verify.publickey.location` para o
JWKS do provedor e desativar `POST /auth/token`; os demais endpoints já validam `iss` e papéis.

---

## Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/auth/token` | público | Emite o JWT |
| `GET` | `/auth/me` | USER/ADMIN | Dados do token atual |
| `POST` | `/simular-investimento` | USER/ADMIN | Simula e persiste um investimento |
| `GET` | `/simulacoes` | USER/ADMIN | Histórico de simulações (`?clienteId=`) |
| `GET` | `/simulacoes/por-produto-dia` | USER/ADMIN | Agregado por produto e dia (`?inicio=&fim=`) |
| `GET` | `/perfil-risco/{clienteId}` | USER/ADMIN | Perfil calculado + fatores |
| `GET` | `/produtos-recomendados/{perfil}` | USER/ADMIN | Produtos aderentes ao perfil |
| `GET` | `/investimentos/{clienteId}` | USER/ADMIN | Histórico de investimentos |
| `GET` | `/produtos` | USER/ADMIN | Catálogo parametrizado (extra) |
| `GET` | `/telemetria` | **ADMIN** | Volume e latência por serviço |
| `GET` | `/q/health`, `/q/openapi`, `/q/swagger-ui` | público | Health, contrato e UI |

### 1. Simulação — `POST /simular-investimento`

```bash
curl -s -X POST http://localhost:8080/simular-investimento \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"clienteId":123,"valor":10000.00,"prazoMeses":12,"tipoProduto":"CDB"}'
```

```json
{
  "produtoValidado": {
    "id": 101,
    "nome": "CDB Caixa 2026",
    "tipo": "CDB",
    "rentabilidade": 0.12,
    "risco": "Baixo"
  },
  "resultadoSimulacao": {
    "valorFinal": 11200.00,
    "rentabilidadeEfetiva": 0.12,
    "prazoMeses": 12,
    "valorInvestido": 10000.00,
    "valorFinalLiquido": 10960.00,
    "impostoRendimento": 240.00,
    "rentabilidadeLiquida": 0.096
  },
  "dataSimulacao": "2026-11-10T14:02:39.077Z",
  "simulacaoId": 1,
  "clienteId": 123,
  "perfilCliente": "Moderado",
  "alertas": []
}
```

O envelope mantém exatamente os campos do enunciado (`produtoValidado`, `resultadoSimulacao`,
`dataSimulacao`) e acrescenta campos de auditoria (`simulacaoId`, `perfilCliente`, `alertas`) e o
resultado líquido de impostos.

**Por que `valorFinal` é bruto?** O enunciado exemplifica R$ 10.000 a 12% a.a. por 12 meses
resultando em R$ 11.200,00 — ou seja, sem descontar IR. Para compatibilidade, `valorFinal` e
`rentabilidadeEfetiva` são brutos; o efeito do IR é exposto em `valorFinalLiquido`,
`impostoRendimento` e `rentabilidadeLiquida`.

**Regras de seleção do produto:**

1. O tipo é normalizado (aceita `cdb`, `Tesouro Direto` etc.) e validado contra o catálogo ativo.
2. O cliente precisa existir.
3. Os produtos do tipo são filtrados pelos limites de `valor` e `prazoMeses` gravados no banco.
4. Entre os candidatos, vence: maior aderência ao perfil de risco → maior rentabilidade líquida
   no prazo → menor `id` (desempate determinístico).
5. Se o produto escolhido tiver risco acima do perfil, a simulação é permitida mas retorna um
   `alerta` explícito.

Erros possíveis: `400` (payload/tipo inválido), `401` (sem token), `404` (cliente inexistente),
`422` (nenhum produto atende aos parâmetros).

### 2. Histórico — `GET /simulacoes`

```bash
curl -s "http://localhost:8080/simulacoes?clienteId=123" -H "Authorization: Bearer $TOKEN"
```

```json
[
  {
    "id": 1,
    "clienteId": 123,
    "produto": "CDB Caixa 2026",
    "valorInvestido": 10000.00,
    "valorFinal": 11200.00,
    "prazoMeses": 12,
    "dataSimulacao": "2026-11-10T14:02:39.077Z"
  }
]
```

### 3. Por produto e dia — `GET /simulacoes/por-produto-dia`

Parâmetros opcionais `inicio` e `fim` (`AAAA-MM-DD`); o padrão é os últimos 30 dias.
Intervalo máximo de 366 dias.

```json
[
  {
    "produto": "CDB Caixa 2026",
    "data": "2026-11-10",
    "quantidadeSimulacoes": 2,
    "mediaValorFinal": 16800.00
  }
]
```

### 4. Telemetria — `GET /telemetria` (ADMIN)

```json
{
  "servicos": [
    {
      "nome": "simular-investimento",
      "quantidadeChamadas": 9,
      "mediaTempoRespostaMs": 26,
      "p95TempoRespostaMs": 88,
      "maxTempoRespostaMs": 88,
      "erros": 0
    }
  ],
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-10" }
}
```

### 5. Perfil de risco — `GET /perfil-risco/{clienteId}`

```json
{
  "clienteId": 123,
  "perfil": "Moderado",
  "pontuacao": 65,
  "descricao": "Perfil equilibrado entre segurança e rentabilidade.",
  "fatores": [
    { "criterio": "Volume investido", "pontos": 26, "maximo": 30, "detalhe": "Patrimônio de R$ 308.000,00 em 5 posição(ões)." },
    { "criterio": "Frequência de movimentações", "pontos": 14, "maximo": 25, "detalhe": "5 movimentação(ões) nos últimos 180 dias." },
    { "criterio": "Preferência por rentabilidade", "pontos": 12, "maximo": 30, "detalhe": "39,9% do patrimônio em ativos sem liquidez diária." },
    { "criterio": "Apetite por retorno", "pontos": 13, "maximo": 15, "detalhe": "Rentabilidade média ponderada de 14,1% ao ano." }
  ],
  "totalInvestido": 308000.00,
  "percentualLiquidez": 0.6006,
  "movimentacoes180Dias": 5,
  "rentabilidadeMedia": 0.1407
}
```

### 6. Produtos recomendados — `GET /produtos-recomendados/{perfil}`

Perfis aceitos: `Conservador`, `Moderado`, `Agressivo` (tolerante a caixa e acento).

```json
[
  {
    "id": 101,
    "nome": "CDB Caixa 2026",
    "tipo": "CDB",
    "rentabilidade": 0.12,
    "risco": "Baixo",
    "liquidez": "Diária",
    "prazoMinimoMeses": 1,
    "prazoMaximoMeses": 60,
    "valorMinimo": 1000.00,
    "valorMaximo": 1000000.00
  }
]
```

Regra de compatibilidade:

| Perfil | Riscos recomendados |
|---|---|
| Conservador | Baixo |
| Moderado | Baixo e Médio |
| Agressivo | Baixo, Médio e Alto |

Ordenação: risco crescente e, dentro do risco, rentabilidade decrescente.

### 7. Histórico de investimentos — `GET /investimentos/{clienteId}`

```json
[
  { "id": 105, "tipo": "Fundo", "valor": 3000.00, "rentabilidade": 0.18, "data": "2026-10-11" },
  { "id": 104, "tipo": "CDB", "valor": 5000.00, "rentabilidade": 0.12, "data": "2026-08-12" }
]
```

---

## Motor de recomendação (perfil de risco)

A pontuação vai de **0 a 100** e é a soma de quatro critérios comportamentais. Todos os pesos e
faixas estão em `MotorPerfilRisco` e são cobertos por testes.

| Critério | Máximo | Regra |
|---|---|---|
| **Volume investido** | 30 | < R$ 10 mil = 4 · < R$ 50 mil = 12 · < R$ 200 mil = 20 · < R$ 1 mi = 26 · ≥ R$ 1 mi = 30 |
| **Frequência de movimentações** (180 dias) | 25 | 0 = 2 · 1–2 = 8 · 3–6 = 14 · 7–12 = 20 · > 12 = 25 |
| **Preferência por rentabilidade** | 30 | `round(% do patrimônio sem liquidez diária × 30)` |
| **Apetite por retorno** | 15 | < 8% = 2 · < 10% = 6 · < 12% = 10 · < 15% = 13 · ≥ 15% = 15 |

Classificação final:

| Pontuação | Perfil | Descrição |
|---|---|---|
| 0–39 | **Conservador** | Prioriza segurança e liquidez, baixa tolerância a oscilações |
| 40–69 | **Moderado** | Equilíbrio entre segurança e rentabilidade |
| 70–100 | **Agressivo** | Busca maior rentabilidade e aceita mais risco |

**Exemplo real (cliente 123 do seed):** patrimônio de R$ 308.000 (26) + 5 movimentações (14) +
39,9% ilíquido (12) + rentabilidade média de 14,07% (13) = **65 pontos → Moderado**.

A resposta inclui a lista `fatores` com pontos, máximo e detalhe textual de cada parcela, o que
torna o cálculo auditável — requisito importante em produtos financeiros.

---

## Regras de simulação

`CalculadoraInvestimento` aplica:

1. **Juros compostos**: `VF = VP × (1 + taxa anual)^(prazoMeses / 12)`
2. **IR regressivo** sobre o rendimento (tabela vigente para renda fixa):

| Prazo | Alíquota |
|---|---|
| até 180 dias | 22,5% |
| 181 a 360 dias | 20,0% |
| 361 a 720 dias | 17,5% |
| acima de 720 dias | 15,0% |

3. **Isenção**: LCI e LCA usam a flag `isento_ir` do produto e não retêm imposto.
4. **Fundos**: 20% até 180 dias (curto prazo) e 15% acima (longo prazo).
5. **IOF**: não incide, pois o prazo mínimo aceito é de 1 mês (30 dias).

Arredondamentos usam `BigDecimal`, `HALF_UP` e escala 2 para dinheiro; valores são persistidos em
**centavos inteiros** e taxas em **basis points**, evitando erros de ponto flutuante.

---

## Modelo de dados

PostgreSQL 17. O schema é aplicado de forma idempotente no startup (`DatabaseBootstrap` +
`db/schema.sql`) e os dados de demonstração usam ids fixos com `ON CONFLICT DO NOTHING`
(`db/seed.sql`). Instantes usam `TIMESTAMPTZ` (UTC), datas de negócio usam `DATE`, valores
monetários `BIGINT` (centavos) e flags `BOOLEAN`.

### Configuração do banco

| Variável | Padrão | Descrição |
|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | Dev Services (dev/test) | URL JDBC, ex. `jdbc:postgresql://localhost:15433/investimentos` |
| `QUARKUS_DATASOURCE_USERNAME` | `investimentos` | Usuário |
| `QUARKUS_DATASOURCE_PASSWORD` | `investimentos` | Senha |
| `APP_DB_RECREATE` | `false` | Recria as tabelas no startup (o perfil `test` usa `true`) |
| `APP_DB_SEED` | `true` | Aplica os dados de demonstração |

### Tabelas

| Tabela | Papel |
|---|---|
| `cliente` | Clientes |
| `produto` | **Tabela parametrizada**: tipo, taxa (bp), risco, liquidez, faixas de valor/prazo, isenção, carência, ativo |
| `investimento` | Posições atuais (entrada do motor de perfil) |
| `movimentacao` | Aportes/resgates (critério de frequência) |
| `simulacao` | Simulações persistidas, incluindo IR, valor líquido e perfil no momento |
| `requisicao_log` | Amostras de telemetria por requisição |
| `usuario` | Usuários da API (hash PBKDF2, papéis, cliente) |

O seed cria 4 clientes com perfis distintos (Conservador, Moderado, Agressivo e sem histórico),
10 produtos ativos (CDB, LCI, LCA, Tesouro, Fundos) e 1 produto inativo para validar o filtro.

**Recriar o banco:** `APP_DB_RECREATE=true` (o perfil `test` já faz isso a cada execução).

### Catálogo de produtos (seed)

| ID | Produto | Tipo | Taxa a.a. | Risco | Liquidez | Prazo (meses) | Valor mínimo |
|---|---|---|---|---|---|---|---|
| 101 | CDB Caixa 2026 | CDB | 12,0% | Baixo | Diária | 1–60 | R$ 1.000 |
| 102 | Fundo XPTO Multimercado | Fundo | 18,0% | Alto | Vencimento | 6–60 | R$ 5.000 |
| 103 | LCI Caixa Imobiliária | LCI | 10,5% | Baixo | Vencimento | 3–48 | R$ 5.000 |
| 104 | LCA Caixa Agro | LCA | 11,0% | Baixo | Vencimento | 3–36 | R$ 5.000 |
| 105 | Tesouro Selic 2029 | Tesouro | 11,0% | Baixo | Diária | 1–48 | R$ 100 |
| 106 | Tesouro IPCA+ 2035 | Tesouro | 13,5% | Médio | Vencimento | 12–120 | R$ 100 |
| 107 | Fundo Ações Caixa | Fundo | 22,0% | Alto | Vencimento | 12–120 | R$ 1.000 |
| 108 | CDB Caixa Liquidez Diária | CDB | 10,0% | Baixo | Diária | 1–24 | R$ 500 |
| 109 | Fundo Renda Fixa Curto Prazo | Fundo | 9,5% | Médio | Diária | 1–24 | R$ 1.000 |
| 110 | CDB Caixa Premium 2028 | CDB | 12,5% | Médio | Vencimento | 12–36 | R$ 50.000 |
| 111 | CDB Caixa Legado (inativo) | CDB | 9,0% | Baixo | Diária | 1–12 | R$ 100 |

---

## Segurança

- **JWT RS256** com `iss` validado, expiração de 1 hora e `jti`.
- **Senhas**: PBKDF2-HMAC-SHA256, 210.000 iterações, salt de 16 bytes, comparação em tempo
  constante (`MessageDigest.isEqual`). A verificação é executada mesmo para usuário inexistente,
  reduzindo diferença de tempo entre "usuário não existe" e "senha errada".
- **Autorização por papel**: `USER` para o fluxo de negócio, `ADMIN` para telemetria.
- **Validação de entrada** com Jakarta Bean Validation (tipos, faixas, tamanhos) e validação de
  negócio contra os parâmetros do banco.
- **SQL parametrizado** em 100% das consultas (sem concatenação de entrada do usuário).
- **Problem Details (RFC 9457)** com `type`, `title`, `status`, `detail`, `instance`,
  `requestId` e lista de erros de campo; stack traces nunca são expostos (apenas logados).
- **CORS restrito** (origem configurável) e cabeçalhos `X-Content-Type-Options`,
  `X-Frame-Options` e `Referrer-Policy`.
- **Correlação**: `X-Request-Id` aceito (quando válido) ou gerado, devolvido em toda resposta.
- **Container**: usuário não-root (uid 10001), healthcheck e runtime sem JDK/compilador.

### Envelope de erro

```json
{
  "type": "https://painel-investimentos.local/erros/validacao",
  "title": "Dados de entrada inválidos",
  "status": 400,
  "detail": "Um ou mais campos não passaram na validação.",
  "instance": "/simular-investimento",
  "timestamp": "2026-11-10T14:03:04.149Z",
  "requestId": "dd6e1923-9d35-4ecc-9a6e-dcf994bf99b6",
  "erros": [
    { "campo": "prazoMeses", "mensagem": "prazoMeses deve ser no mínimo 1" },
    { "campo": "valor", "mensagem": "valor deve ser maior que zero" }
  ]
}
```

---

## Telemetria

Um filtro JAX-RS (`FiltroObservabilidade`) mede cada endpoint anotado com `@Telemetrado` e grava
uma amostra em `requisicao_log`: serviço lógico, método, caminho, status, duração em ms, usuário e
timestamp UTC.

A consulta agrega por serviço:

- `quantidadeChamadas`
- `mediaTempoRespostaMs` (`AVG`)
- `p95TempoRespostaMs` (posição na lista ordenada de durações)
- `maxTempoRespostaMs`
- `erros` (status ≥ 400)

A gravação de telemetria roda em transação própria e **nunca derruba a requisição original**:
falhas são apenas logadas. O endpoint `/telemetria` também é instrumentado (auto-observação).

---

## Testes

```bash
mvn test
```

**61 testes, todos verdes** — 25 unitários e 36 de integração:

| Suíte | Tipo | Cobre |
|---|---|---|
| `CalculadoraInvestimentoTest` | unitário | Juros compostos, tabela de IR, isenção, fundos, arredondamento, entradas inválidas |
| `MotorPerfilRiscoTest` | unitário | Perfis Conservador/Moderado/Agressivo, faixas 39/40 e 69/70, tabelas de pontuação, cliente sem histórico |
| `PerfilRiscoTest` / `DinheiroTest` | unitário | Parsing tolerante, compatibilidade de risco, conversão centavos/basis points |
| `PasswordHasherTest` | unitário | Salt, verificação, hashes malformados |
| `ScriptSqlTest` | unitário | Parser de scripts SQL |
| `SimulacaoApiTest` | integração | Envelope do desafio, persistência, agregação diária, seleção premium, 404/422/400 |
| `PerfilEProdutosApiTest` | integração | Perfis do seed, fatores, recomendações por perfil, catálogo, investimentos |
| `AutenticacaoETelemetriaTest` | integração | Login, `/auth/me`, 401, 403, agregação de telemetria |
| `TratamentoErrosTest` | integração | JSON malformado, 404, 405, 415, correlation id |

Os testes de integração sobem a aplicação com o perfil `test`, que usa um PostgreSQL efêmero
via **Dev Services** (Docker), recria as tabelas (`app.db.recreate=true`) e usa porta HTTP
aleatória (`quarkus.http.test-port=0`) para não conflitar com uma instância local. Para rodar
sem Dev Services, exporte `QUARKUS_DATASOURCE_JDBC_URL`/`USERNAME`/`PASSWORD` antes do `mvn test`.

---

## Estrutura do projeto

```
painel-investimentos/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── Makefile
├── README.md
├── EVIDENCIAS.md
├── scripts/
│   └── smoke-test.sh
└── src/
    ├── main/
    │   ├── java/br/com/investimentos/
    │   │   ├── api/            # resources, DTOs, filtros, OpenAPI
    │   │   │   └── erro/       # Problem Details e mappers
    │   │   ├── dominio/        # records, enums e exceções
    │   │   ├── infra/          # datas, parser SQL, bootstrap do banco
    │   │   ├── persistencia/   # repositórios JDBC
    │   │   ├── seguranca/      # JWT e hash de senha
    │   │   └── servico/        # motor de perfil, calculadora, orquestração
    │   └── resources/
    │       ├── application.properties
    │       ├── db/schema.sql
    │       ├── db/seed.sql
    │       └── jwt/*.pem
    └── test/java/br/com/investimentos/…
```

---

## Decisões e trade-offs

| Decisão | Motivo |
|---|---|
| **PostgreSQL 17** | Banco relacional escolhido para a entrega. Tipos nativos (`TIMESTAMPTZ`, `DATE`, `BOOLEAN`), transações, concorrência e pool de conexões — pronto para múltiplas instâncias, ao contrário de um arquivo local. |
| **JDBC puro em vez de ORM** | Schema e consultas explícitos e revisáveis, sem mapeamento implícito. O custo é mais código de mapeamento; em troca, o SQL é portável e o comportamento previsível. |
| **Dinheiro em centavos / taxas em basis points** | Precisão determinística em cálculos e agregações (`AVG`, somas) sem `double`. |
| **`valorFinal` bruto + campos líquidos** | Compatibilidade com o envelope de exemplo do enunciado **e** transparência tributária. |
| **Perfil calculado on-line** | O perfil é sempre derivado do comportamento atual; não há cache nem perfil "declarado" que possa divergir dos dados. |
| **Simulação não bloqueia risco incompatível** | O desafio pede o produto que atende valor/prazo/tipo; bloquear por perfil impediria simulações legítimas. A divergência é sinalizada em `alertas`. |
| **401 sem corpo** | A falha de autenticação ocorre na camada HTTP do Quarkus (antes do roteamento REST), que responde 401 + `WWW-Authenticate`; os demais erros usam Problem Details. |
| **Pool de conexões (`min=2`, `max=10`)** | Aproveita a concorrência do PostgreSQL sem esgotar conexões; o pool é gerenciado pelo Agroal. |
| **Sem paginação em `/simulacoes`** | O enunciado pede "todas as simulações"; o filtro por cliente já limita o volume. |

### Limitações conhecidas

- Os testes de integração usam Dev Services e, portanto, exigem Docker (ou um PostgreSQL externo apontado por variáveis de ambiente).
- Usuários e chaves JWT de demonstração estão no repositório (documentado acima).
- Não há controle de propriedade por cliente (qualquer `USER` consulta qualquer `clienteId`).
  Em um cenário real, o `cliente_id` do token deve ser comparado com o recurso solicitado.
- Telemetria é gravada de forma síncrona (uma escrita por requisição).
- `GET /simulacoes` pode crescer sem paginação em bases grandes.

---

## Evoluções sugeridas

1. **Versionamento de schema com Flyway/Liquibase** e migrações automatizadas em produção.
2. **Keycloak/OAuth2** com JWKS e remoção do endpoint de token local.
3. **ABAC/ownership**: validar `cliente_id` do token contra o recurso e adicionar escopos.
4. **OpenTelemetry** para métricas/traces e agregação assíncrona de telemetria.
5. **Cache** do catálogo de produtos e do perfil (com invalidação por evento).
6. **Pact/contratos** e testes de carga (k6/Gatling) no pipeline.
7. **Idempotência** da simulação via `Idempotency-Key`.

---

## Evidências

O arquivo [EVIDENCIAS.md](EVIDENCIAS.md) reúne as saídas reais de build, testes, smoke test e
verificação do container utilizadas na validação desta entrega, além do roteiro para reproduzir
cada uma delas.

Para publicar em um repositório Git público:

```bash
git init
git add .
git commit -m "feat: painel de investimentos com perfil de risco dinamico"
git remote add origin <url-do-repositorio>
git push -u origin main
```
