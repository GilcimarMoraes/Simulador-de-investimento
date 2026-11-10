# Evidências de execução

Documento de evidências da entrega do **Painel de Investimentos com Perfil de Risco Dinâmico**,
com persistência em **PostgreSQL 17**. Todos os blocos abaixo são transcrições reais de execução
nesta máquina, em **10/11/2026**.

---

## 1. Ambiente de execução

| Item | Valor |
|---|---|
| Sistema operacional | AnduinOS 1.1.12 (base Ubuntu), kernel 7.0.0-38-generic |
| CPU | 8 vCPU |
| JDK | Eclipse Temurin **25.0.5+8** (LTS) |
| Maven | Apache Maven **3.9.12** |
| Framework | Quarkus **3.40.1** |
| Banco | **PostgreSQL 17.11** (`postgres:17-alpine`) |
| Docker | Engine 29.7.2 (buildx) |

```text
$ java -version
openjdk version "25.0.5" 2026-10-20 LTS
OpenJDK Runtime Environment Temurin-25.0.5+8 (build 25.0.5+8-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.5+8 (build 25.0.5+8-LTS, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
Java version: 25.0.5, vendor: Eclipse Adoptium

$ docker exec painel-investimentos-db psql -U investimentos -d investimentos -t -c "select version();"
 PostgreSQL 17.11 on x86_64-pc-linux-musl, compiled by gcc (Alpine 15.2.0) 15.2.0, 64-bit
```

---

## 2. Build limpo com a suíte completa de testes

Comando executado na raiz do projeto:

```bash
mvn clean package
```

Resultado:

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Tests run: 9,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.api.AutenticacaoETelemetriaTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.api.PerfilEProdutosApiTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.api.SimulacaoApiTest
[INFO] Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.api.TratamentoErrosTest
[INFO] Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.dominio.DinheiroTest
[INFO] Tests run: 4,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.dominio.PerfilRiscoTest
[INFO] Tests run: 2,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.infra.ScriptSqlTest
[INFO] Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.seguranca.PasswordHasherTest
[INFO] Tests run: 7,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.servico.CalculadoraInvestimentoTest
[INFO] Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0 -- in br.com.investimentos.servico.MotorPerfilRiscoTest
[INFO] Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:23 min
[INFO] --- quarkus:3.40.1:build (default) @ painel-investimentos ---
```

**Resumo:** 61 testes, 0 falhas, 0 erros — 25 unitários + 36 de integração.

Os testes de integração sobem um **PostgreSQL 17 efêmero via Dev Services** (imagem
`postgres:17-alpine`), recriam as tabelas e executam contra o banco real.

| Suíte | Tipo | Testes | Tempo |
|---|---|---:|---:|
| `AutenticacaoETelemetriaTest` | integração | 9 | 22,26 s |
| `PerfilEProdutosApiTest` | integração | 10 | 6,11 s |
| `SimulacaoApiTest` | integração | 11 | 7,15 s |
| `TratamentoErrosTest` | integração | 6 | 3,30 s |
| `CalculadoraInvestimentoTest` | unitário | 7 | 0,02 s |
| `MotorPerfilRiscoTest` | unitário | 6 | 0,02 s |
| `PerfilRiscoTest` | unitário | 4 | 0,02 s |
| `PasswordHasherTest` | unitário | 3 | 0,84 s |
| `DinheiroTest` | unitário | 3 | 0,01 s |
| `ScriptSqlTest` | unitário | 2 | 0,01 s |

Relatórios individuais: `target/surefire-reports/`.

---

## 3. Artefato compilado para Java 25

```text
$ javap -verbose target/classes/br/com/investimentos/persistencia/SimulacaoRepository.class | grep major
  major version: 69        # 69 = Java 25

$ ls target/quarkus-app/
app  lib  quarkus  quarkus-app-dependencies.txt  quarkus-run.jar

$ du -sh target/quarkus-app
29M
```

O `pom.xml` fixa `maven.compiler.release=25` e o runtime usado foi o Temurin 25.0.5.

---

## 4. Stack completa via Docker Compose

```bash
docker compose up -d --build
```

```text
 Container painel-investimentos-db  Running
 Container painel-investimentos-db  Healthy
 Container painel-investimentos-api Starting
 Container painel-investimentos-api Started

$ docker compose ps
painel-investimentos-api | Up 6 seconds (healthy) | 0.0.0.0:8080->8080/tcp
painel-investimentos-db  | Up 5 minutes (healthy) | 0.0.0.0:15433->5432/tcp
```

| Item | Valor |
|---|---|
| Imagem da API | `painel-investimentos:1.0.0` |
| Image ID | `e9685535477d` |
| Tamanho | 546 MB (JRE 25 + aplicação) |
| Base de build | `maven:3.9-eclipse-temurin-25` |
| Base de runtime | `eclipse-temurin:25-jre` |
| Banco | `postgres:17-alpine`, volume `painel-investimentos-postgres` |

```text
$ docker logs painel-investimentos-api | grep -E "PostgreSQL|started in"
INFO [br.com.investimentos.infra.DatabaseBootstrap] (main) Banco PostgreSQL inicializado com sucesso.
INFO [io.quarkus] (main) painel-investimentos 1.0.0 on JVM (powered by Quarkus 3.40.1) started in 4.357s.

$ docker exec painel-investimentos-api id
uid=10001(app) gid=999(app) groups=999(app)
```

A API roda como usuário **não-root**, só inicia depois que o PostgreSQL fica `healthy`
(`depends_on.condition: service_healthy`) e o healthcheck nativo consulta
`/q/health/ready` — que agora valida também a conexão com o banco:

```text
$ curl -s http://localhost:8080/q/health/ready
{"status":"UP","checks":[{"name":"Database connections health check","status":"UP",
 "data":{"<default>":"UP"}}]}
```

---

## 5. Smoke test ponta a ponta (23 verificações)

```bash
./scripts/smoke-test.sh
```

```text
== Saude ==
  [OK] GET /q/health/ready

== Autenticacao ==
  [OK] login demo
  [OK] login admin
  [OK] POST /auth/token -> 401
  [OK] GET /simulacoes -> 401

== Perfil de risco ==
{"clienteId":123,"perfil":"Moderado","pontuacao":65}
  [OK] cliente 123 -> Moderado
  [OK] pontuacao 65
  [OK] 4 fatores de composicao

== Produtos recomendados ==
  5 produtos para Conservador
  [OK] somente risco Baixo

== Simulacao ==
{"produto":"CDB Caixa 2026","valorFinal":11200.00,"liquido":10960.00,"perfil":"Moderado"}
  [OK] produto CDB Caixa 2026
  [OK] valor final 11200.00
  [OK] valor liquido 10960.00
  [OK] produto premium para 60 mil

== Historico ==
  6 simulacoes persistidas
  [OK] persistencia

== Agregado por produto e dia ==
{"produto":"CDB Caixa 2026","data":"2026-11-10","quantidadeSimulacoes":3,"mediaValorFinal":11200.00}
  [OK] agregacao

== Investimentos do cliente ==
  5 posicoes
  [OK] historico de investimentos

== Telemetria ==
  [OK] GET /telemetria -> 403
  autenticacao: 9 chamadas, media 497ms, p95 1119ms
  investimentos: 3 chamadas, media 7ms, p95 8ms
  perfil-risco: 3 chamadas, media 61ms, p95 85ms
  produtos-recomendados: 5 chamadas, media 34ms, p95 71ms
  simulacoes: 3 chamadas, media 10ms, p95 11ms
  simulacoes-por-produto-dia: 3 chamadas, media 6ms, p95 7ms
  simular-investimento: 14 chamadas, media 33ms, p95 89ms
  telemetria: 2 chamadas, media 17ms, p95 19ms
  [OK] telemetria por servico

== Tratamento de erros ==
  [OK] POST /simular-investimento -> 422   (prazo fora dos limites)
  [OK] POST /simular-investimento -> 404   (cliente inexistente)
  [OK] POST /simular-investimento -> 400   (campos invalidos)
  [OK] POST /simular-investimento -> 400   (JSON malformado)
  [OK] GET /produtos-recomendados/Arrojado -> 400 (perfil invalido)

Smoke test concluido com sucesso: todos os cenarios passaram.
```

A latência de `autenticacao` inclui o custo intencional do PBKDF2 (210.000 iterações) e o
aquecimento da JVM; em regime, o login leva ~210 ms. Os demais serviços respondem em poucos
milissegundos.

---

## 6. Contrato OpenAPI e Swagger UI

```text
$ curl -s "http://localhost:8080/q/openapi?format=json" | jq -r '.openapi, (.paths|length)'
3.1.0
10

$ curl -s "http://localhost:8080/q/openapi?format=json" | jq -r '.components.securitySchemes|keys[0]'
BearerAuth

$ curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/q/swagger-ui/
200
```

Rotas publicadas no contrato:

```text
/auth/me
/auth/token
/investimentos/{clienteId}
/perfil-risco/{clienteId}
/produtos
/produtos-recomendados/{perfil}
/simulacoes
/simulacoes/por-produto-dia
/simular-investimento
/telemetria
```

---

## 7. Estado do PostgreSQL

```text
$ docker exec painel-investimentos-db psql -U investimentos -d investimentos -c "\dt"
 Schema |      Name      | Type  |     Owner
--------+----------------+-------+----------------
 public | cliente        | table | investimentos
 public | investimento   | table | investimentos
 public | movimentacao   | table | investimentos
 public | produto        | table | investimentos
 public | requisicao_log | table | investimentos
 public | simulacao      | table | investimentos
 public | usuario        | table | investimentos
(7 rows)
```

Tipos nativos verificados em `information_schema`:

```text
$ docker exec painel-investimentos-db psql ... -c "select column_name, data_type from information_schema.columns
                                                    where table_name in ('produto','simulacao')
                                                    and column_name in ('ativo','isento_ir','data_simulacao','valor_final_centavos','id');"
     column_name      |        data_type
----------------------+--------------------------
 ativo                | boolean
 id                   | bigint
 isento_ir            | boolean
 data_simulacao       | timestamp with time zone
 id                   | bigint
 valor_final_centavos | bigint
(6 rows)
```

Contagens após o fluxo de smoke:

```text
 clientes=4
 produtos=11          (10 ativos + 1 inativo)
 investimentos=9
 movimentacoes=22
 simulacoes=6
 telemetria=48
 usuarios=3
```

O schema é aplicado de forma idempotente no startup e o seed usa `ON CONFLICT DO NOTHING` com
ids fixos, portanto reinícios preservam os dados. O perfil `test` recria as tabelas a cada
execução (`app.db.recreate=true`).

---

## 8. Matriz de requisitos → evidência

| Requisito do desafio | Endpoint / artefato | Evidência |
|---|---|---|
| Receber envelope JSON de simulação | `POST /simular-investimento` | Smoke test: produto 101, R$ 11.200,00 |
| Consultar parâmetros em banco SQL | tabela `produto` + `GET /produtos` | 11 produtos no PostgreSQL (10 ativos) |
| Validar entrada com base nos parâmetros | `SimulacaoService` + Bean Validation | Testes 400/422 e smoke test |
| Filtrar produto adequado | ranking por perfil/rentabilidade | `selecionaProdutoPremium` (id 110 para R$ 60 mil) |
| Calcular simulações | `CalculadoraInvestimento` | `CalculadoraInvestimentoTest` (7 testes) |
| Retornar produto + resultado | envelope `produtoValidado`/`resultadoSimulacao` | Smoke test |
| Persistir simulação | tabela `simulacao` | 6 registros no PostgreSQL após o smoke |
| Endpoint de todas as simulações | `GET /simulacoes` | Smoke test + testes de integração |
| Valores por produto e dia | `GET /simulacoes/por-produto-dia` | Agregado com quantidade e média |
| Telemetria de volume/tempo | `GET /telemetria` (ADMIN) | 8 serviços com média/p95/máximo |
| Motor de recomendação | `MotorPerfilRisco` + `GET /perfil-risco` | 65 pontos → Moderado, com fatores |
| Produtos recomendados por perfil | `GET /produtos-recomendados/{perfil}` | Conservador = 5 produtos de risco baixo |
| Histórico de investimentos | `GET /investimentos/{clienteId}` | 5 posições do cliente 123 |
| Autenticação JWT | `POST /auth/token` + RS256 | 401 sem token, 403 sem ADMIN |
| Execução em container | `Dockerfile` + `docker-compose.yml` | API + PostgreSQL healthy |
| Documentação | README + OpenAPI 3.1 + Swagger UI | 10 rotas publicadas |
| Testes unitários e de integração | `src/test/java` | 61 testes verdes contra PostgreSQL real |

---

## 9. Reprodução

```bash
# 1. Testes e empacotamento (sobe PostgreSQL efêmero via Dev Services)
mvn clean package

# 2. Stack completa (API + PostgreSQL)
docker compose up -d --build

# 3. Validação ponta a ponta
./scripts/smoke-test.sh

# 4. Apenas o banco, para rodar a API localmente
docker compose up -d postgres
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:15433/investimentos
export QUARKUS_DATASOURCE_USERNAME=investimentos
export QUARKUS_DATASOURCE_PASSWORD=investimentos
java -jar target/quarkus-app/quarkus-run.jar
```

Pré-requisitos: JDK 25, Maven 3.9+, Docker (Compose v2) e `curl`/`jq` para o smoke test.
O `docker compose down -v` remove também o volume do PostgreSQL.

---

## 10. Publicação do código-fonte

O projeto está pronto para ser versionado e publicado:

```bash
git init
git add .
git commit -m "feat: painel de investimentos com perfil de risco dinamico (PostgreSQL)"
git remote add origin <URL_DO_REPOSITORIO_PUBLICO>
git push -u origin main
```

> Substitua `<URL_DO_REPOSITORIO_PUBLICO>` pela URL do Git público antes de entregar.
> Alternativamente, gere um ZIP do diretório `painel-investimentos/` (sem `target/`).
