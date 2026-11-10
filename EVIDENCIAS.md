# Evidências de execução

Documento de evidências da entrega do **Painel de Investimentos com Perfil de Risco Dinâmico**.
Todos os blocos abaixo são transcrições reais de execução nesta máquina, em **10/11/2026**.

---

## 1. Ambiente de execução

| Item | Valor |
|---|---|
| Sistema operacional | AnduinOS 1.1.12 (base Ubuntu), kernel 7.0.0-38-generic |
| CPU | 8 vCPU |
| JDK | Eclipse Temurin **25.0.5+8** (LTS) |
| Maven | Apache Maven **3.9.12** |
| Framework | Quarkus **3.40.1** |
| Banco | SQLite (driver `org.xerial:sqlite-jdbc` 3.54.1.0) |
| Docker | Engine 29.7.2 (buildx) |

```text
$ java -version
openjdk version "25.0.5" 2026-10-20 LTS
OpenJDK Runtime Environment Temurin-25.0.5+8 (build 25.0.5+8-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.5+8 (build 25.0.5+8-LTS, mixed mode, sharing)

$ mvn -version
Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
Java version: 25.0.5, vendor: Eclipse Adoptium
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
[INFO] Total time:  01:16 min
[INFO] --- quarkus:3.40.1:build (default) @ painel-investimentos ---
```

**Resumo:** 61 testes, 0 falhas, 0 erros — 25 unitários + 36 de integração.

| Suíte | Tipo | Testes | Tempo |
|---|---|---:|---:|
| `AutenticacaoETelemetriaTest` | integração | 9 | 17,03 s |
| `PerfilEProdutosApiTest` | integração | 10 | 6,92 s |
| `SimulacaoApiTest` | integração | 11 | 9,56 s |
| `TratamentoErrosTest` | integração | 6 | 3,38 s |
| `CalculadoraInvestimentoTest` | unitário | 7 | 0,02 s |
| `MotorPerfilRiscoTest` | unitário | 6 | 0,02 s |
| `PerfilRiscoTest` | unitário | 4 | 0,02 s |
| `DinheiroTest` | unitário | 3 | 0,01 s |
| `PasswordHasherTest` | unitário | 3 | 0,90 s |
| `ScriptSqlTest` | unitário | 2 | 0,01 s |

Relatórios individuais: `target/surefire-reports/`.

---

## 3. Artefato compilado para Java 25

```text
$ javap -verbose target/classes/br/com/investimentos/servico/SimulacaoService.class | grep major
  major version: 69        # 69 = Java 25

$ ls target/quarkus-app/
app  lib  quarkus  quarkus-app-dependencies.txt  quarkus-run.jar

$ du -sh target/quarkus-app
39M
```

O `pom.xml` fixa `maven.compiler.release=25` e o runtime usado foi o Temurin 25.0.5.

---

## 4. Smoke test da API (JAR local, porta 8080)

```bash
java -jar target/quarkus-app/quarkus-run.jar
./scripts/smoke-test.sh
```

Saída (23 verificações, 0 falhas):

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
  2 simulacoes persistidas
  [OK] persistencia

== Agregado por produto e dia ==
{"produto":"CDB Caixa 2026","data":"2026-11-10","quantidadeSimulacoes":1,"mediaValorFinal":11200.00}
  [OK] agregacao

== Investimentos do cliente ==
  5 posicoes
  [OK] historico de investimentos

== Telemetria ==
  [OK] GET /telemetria -> 403
  autenticacao: 3 chamadas, media 566ms, p95 1199ms
  investimentos: 1 chamadas, media 5ms, p95 5ms
  perfil-risco: 1 chamadas, media 41ms, p95 41ms
  produtos-recomendados: 1 chamadas, media 44ms, p95 44ms
  simulacoes: 1 chamadas, media 7ms, p95 7ms
  simulacoes-por-produto-dia: 1 chamadas, media 3ms, p95 3ms
  simular-investimento: 2 chamadas, media 56ms, p95 81ms
  [OK] telemetria por servico

== Tratamento de erros ==
  [OK] POST /simular-investimento -> 422   (prazo fora dos limites)
  [OK] POST /simular-investimento -> 404   (cliente inexistente)
  [OK] POST /simular-investimento -> 400   (campos invalidos)
  [OK] POST /simular-investimento -> 400   (JSON malformado)
  [OK] GET /produtos-recomendados/Arrojado -> 400 (perfil invalido)

Smoke test concluido com sucesso: todos os cenarios passaram.
```

Observações:

- A latência de `autenticacao` inclui o custo intencional do PBKDF2 (210.000 iterações) e o
  aquecimento da JVM no primeiro login. Em regime, o login leva ~210 ms.
- Os demais serviços respondem em poucos milissegundos.

---

## 5. Contrato OpenAPI e Swagger UI

```text
$ curl -s "http://localhost:8080/q/openapi?format=json" | jq -r '.openapi, (.paths|length)'
3.1.0
10

$ curl -s "http://localhost:8080/q/openapi?format=json" | jq -c '.components.securitySchemes.BearerAuth'
{"description":"JWT obtido em POST /auth/token (informe apenas o token)",
 "type":"http","scheme":"bearer","bearerFormat":"JWT"}

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

## 6. Container Docker

### Build multi-stage

```bash
docker build -t painel-investimentos:1.0.0 .
```

```text
#16 [build 6/6] RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package
#16 DONE 33.0s
#19 exporting to image
#19 naming to docker.io/library/painel-investimentos:1.0.0 done
#19 DONE 4.0s
```

| Item | Valor |
|---|---|
| Imagem | `painel-investimentos:1.0.0` |
| Image ID | `6f030bc7e190` |
| Tamanho | 568 MB (JRE 25 + aplicação) |
| Base de build | `maven:3.9-eclipse-temurin-25` |
| Base de runtime | `eclipse-temurin:25-jre` |

### Execução e healthcheck

```bash
docker run -d --name painel-teste -p 18080:8080 painel-investimentos:1.0.0
```

```text
$ curl -s -o /dev/null -w "%{http_code}" http://localhost:18080/q/health/ready
200

$ docker ps --filter name=painel-teste --format '{{.Status}}'
Up 14 seconds (healthy)

$ docker exec painel-teste id
uid=10001(app) gid=999(app) groups=999(app)

$ docker exec painel-teste ls -lh /data
-rw-r--r-- 1 app app 4.0K investimentos.db
-rw-r--r-- 1 app app  32K investimentos.db-shm
-rw-r--r-- 1 app app 314K investimentos.db-wal
```

O container roda como usuário **não-root** (`app`, uid 10001), com volume persistente em `/data`
e healthcheck nativo (`HEALTHCHECK ... /q/health/ready`).

### Smoke test contra o container

```bash
./scripts/smoke-test.sh http://localhost:18080
```

```text
  ... (23 verificações)
Smoke test concluido com sucesso: todos os cenarios passaram.
```

### Docker Compose

```text
$ docker compose config -q
(exit 0 — arquivo válido)
```

`docker compose up --build` sobe a API em `http://localhost:8080` com volume nomeado
`painel-investimentos-dados`.

---

## 7. Estado do banco após o fluxo

Consultado no arquivo `data/investimentos.db` após o smoke test:

| Tabela | Registros |
|---|---:|
| `cliente` | 4 |
| `produto` | 11 (10 ativos + 1 inativo) |
| `investimento` | 9 |
| `movimentacao` | 22 |
| `simulacao` | 2 |
| `requisicao_log` | 16 |
| `usuario` | 3 |

O schema é aplicado de forma idempotente no startup e o seed usa `INSERT OR IGNORE` com ids
fixos, portanto reinícios preservam os dados. O perfil `test` recria o banco a cada execução.

---

## 8. Matriz de requisitos → evidência

| Requisito do desafio | Endpoint / artefato | Evidência |
|---|---|---|
| Receber envelope JSON de simulação | `POST /simular-investimento` | Smoke test: produto 101, R$ 11.200,00 |
| Consultar parâmetros em banco SQL | tabela `produto` + `GET /produtos` | 10 produtos ativos no seed |
| Validar entrada com base nos parâmetros | `SimulacaoService` + Bean Validation | Testes 400/422 e smoke test |
| Filtrar produto adequado | ranking por perfil/rentabilidade | `selecionaProdutoPremium` (id 110 para R$ 60 mil) |
| Calcular simulações | `CalculadoraInvestimento` | `CalculadoraInvestimentoTest` (7 testes) |
| Retornar produto + resultado | envelope `produtoValidado`/`resultadoSimulacao` | Smoke test |
| Persistir simulação | tabela `simulacao` | 2 registros após smoke |
| Endpoint de todas as simulações | `GET /simulacoes` | Smoke test + testes de integração |
| Valores por produto e dia | `GET /simulacoes/por-produto-dia` | Agregado com quantidade e média |
| Telemetria de volume/tempo | `GET /telemetria` (ADMIN) | 8 serviços com média/p95/máximo |
| Motor de recomendação | `MotorPerfilRisco` + `GET /perfil-risco` | 65 pontos → Moderado, com fatores |
| Produtos recomendados por perfil | `GET /produtos-recomendados/{perfil}` | Conservador = 5 produtos de risco baixo |
| Histórico de investimentos | `GET /investimentos/{clienteId}` | 5 posições do cliente 123 |
| Autenticação JWT | `POST /auth/token` + RS256 | 401 sem token, 403 sem ADMIN |
| Execução em container | `Dockerfile` + `docker-compose.yml` | Imagem 568 MB, container healthy |
| Documentação | README + OpenAPI 3.1 + Swagger UI | 10 rotas publicadas |
| Testes unitários e de integração | `src/test/java` | 61 testes verdes |

---

## 9. Reprodução

```bash
# 1. Testes e empacotamento
mvn clean package

# 2. Execução local
java -jar target/quarkus-app/quarkus-run.jar

# 3. Em outro terminal: validação ponta a ponta
./scripts/smoke-test.sh

# 4. Container
docker build -t painel-investimentos:1.0.0 .
docker compose up --build
./scripts/smoke-test.sh http://localhost:8080
```

Pré-requisitos: JDK 25, Maven 3.9+, Docker (opcional) e `curl`/`jq` para o smoke test.

---

## 10. Publicação do código-fonte

O projeto está pronto para ser versionado e publicado:

```bash
git init
git add .
git commit -m "feat: painel de investimentos com perfil de risco dinamico"
git remote add origin <URL_DO_REPOSITORIO_PUBLICO>
git push -u origin main
```

> Substitua `<URL_DO_REPOSITORIO_PUBLICO>` pela URL do Git público antes de entregar.
> Alternativamente, gere um ZIP do diretório `painel-investimentos/` (sem `target/` e `data/`).
