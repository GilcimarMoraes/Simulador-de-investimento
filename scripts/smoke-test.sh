#!/usr/bin/env bash
#
# Smoke test ponta a ponta da API de investimentos.
# Valida autenticacao JWT, os 7 endpoints do desafio, persistencia e tratamento de erros.
# Requisitos: curl e jq. Uso: ./scripts/smoke-test.sh [url-base]
#
set -euo pipefail

BASE="${1:-${BASE_URL:-http://127.0.0.1:8080}}"
TOKEN=""
TOKEN_ADMIN=""
FALHAS=0

verde() { printf '\033[0;32m%s\033[0m\n' "$*"; }
vermelho() { printf '\033[0;31m%s\033[0m\n' "$*"; }
titulo() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }

ok() { verde "  [OK] $*"; }
falha() { vermelho "  [FALHA] $*"; FALHAS=$((FALHAS + 1)); }

# esperar_status <metodo> <caminho> <status-esperado> <token|-> [corpo]
esperar_status() {
  local metodo="$1" caminho="$2" esperado="$3" token="$4" corpo="${5:-}"
  local args=(-s -o /dev/null -w '%{http_code}' -X "$metodo" "$BASE$caminho")
  [ "$token" != "-" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$corpo" ] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  local obtido
  obtido=$(curl "${args[@]}")
  if [ "$obtido" = "$esperado" ]; then
    ok "$metodo $caminho -> $obtido"
  else
    falha "$metodo $caminho -> esperado $esperado, obtido $obtido"
  fi
}

# json <metodo> <caminho> <token|-> [corpo]
json() {
  local metodo="$1" caminho="$2" token="$3" corpo="${4:-}"
  local args=(-s -X "$metodo" "$BASE$caminho")
  [ "$token" != "-" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$corpo" ] && args+=(-H 'Content-Type: application/json' -d "$corpo")
  curl "${args[@]}"
}

titulo "Saude"
for _ in $(seq 1 30); do
  if curl -fsS "$BASE/q/health/ready" >/dev/null 2>&1; then
    ok "GET /q/health/ready"
    break
  fi
  sleep 1
done
curl -fsS "$BASE/q/health/ready" >/dev/null 2>&1 || { falha "aplicacao nao respondeu em $BASE"; exit 1; }

titulo "Autenticacao"
TOKEN=$(json POST /auth/token - '{"username":"demo","password":"demo123"}' | jq -r .accessToken)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && ok "login demo" || falha "login demo"

TOKEN_ADMIN=$(json POST /auth/token - '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
[ -n "$TOKEN_ADMIN" ] && [ "$TOKEN_ADMIN" != "null" ] && ok "login admin" || falha "login admin"

esperar_status POST /auth/token 401 - '{"username":"demo","password":"errada"}'
esperar_status GET /simulacoes 401 -

titulo "Perfil de risco"
PERFIL=$(json GET /perfil-risco/123 "$TOKEN")
echo "  $PERFIL" | jq -c '{clienteId, perfil, pontuacao}'
[ "$(echo "$PERFIL" | jq -r .perfil)" = "Moderado" ] && ok "cliente 123 -> Moderado" || falha "perfil inesperado"
[ "$(echo "$PERFIL" | jq -r .pontuacao)" = "65" ] && ok "pontuacao 65" || falha "pontuacao inesperada"
[ "$(echo "$PERFIL" | jq -r '.fatores | length')" = "4" ] && ok "4 fatores de composicao" || falha "fatores ausentes"

titulo "Produtos recomendados"
RECOMENDADOS=$(json GET /produtos-recomendados/Conservador "$TOKEN")
echo "  $(echo "$RECOMENDADOS" | jq -r 'length') produtos para Conservador"
[ "$(echo "$RECOMENDADOS" | jq -r '[.[].risco] | unique | join(",")')" = "Baixo" ] \
  && ok "somente risco Baixo" || falha "produto incompativel na recomendacao"

titulo "Simulacao"
SIM=$(json POST /simular-investimento "$TOKEN" '{"clienteId":123,"valor":10000.00,"prazoMeses":12,"tipoProduto":"CDB"}')
echo "  $SIM" | jq -c '{produto: .produtoValidado.nome, valorFinal: .resultadoSimulacao.valorFinal, liquido: .resultadoSimulacao.valorFinalLiquido, perfil: .perfilCliente}'
[ "$(echo "$SIM" | jq -r '.produtoValidado.id')" = "101" ] && ok "produto CDB Caixa 2026" || falha "produto inesperado"
[ "$(echo "$SIM" | jq -r '.resultadoSimulacao.valorFinal')" = "11200.00" ] && ok "valor final 11200.00" || falha "calculo inesperado"
[ "$(echo "$SIM" | jq -r '.resultadoSimulacao.valorFinalLiquido')" = "10960.00" ] && ok "valor liquido 10960.00" || falha "calculo liquido inesperado"

SIM2=$(json POST /simular-investimento "$TOKEN" '{"clienteId":123,"valor":60000.00,"prazoMeses":12,"tipoProduto":"CDB"}')
[ "$(echo "$SIM2" | jq -r '.produtoValidado.id')" = "110" ] && ok "produto premium para 60 mil" || falha "selecao premium inesperada"

titulo "Historico"
HISTORICO=$(json GET /simulacoes "$TOKEN")
echo "  $(echo "$HISTORICO" | jq -r 'length') simulacoes persistidas"
[ "$(echo "$HISTORICO" | jq -r 'length')" -ge 2 ] && ok "persistencia" || falha "historico vazio"

titulo "Agregado por produto e dia"
AGREGADO=$(json GET /simulacoes/por-produto-dia "$TOKEN")
echo "$AGREGADO" | jq -c '.[0] | {produto, data, quantidadeSimulacoes, mediaValorFinal}'
[ "$(echo "$AGREGADO" | jq -r 'length')" -ge 1 ] && ok "agregacao" || falha "agregado vazio"

titulo "Investimentos do cliente"
INVESTIMENTOS=$(json GET /investimentos/123 "$TOKEN")
echo "  $(echo "$INVESTIMENTOS" | jq -r 'length') posicoes"
[ "$(echo "$INVESTIMENTOS" | jq -r 'length')" = "5" ] && ok "historico de investimentos" || falha "posicoes inesperadas"

titulo "Telemetria"
esperar_status GET /telemetria 403 "$TOKEN"
TELEMETRIA=$(json GET /telemetria "$TOKEN_ADMIN")
echo "$TELEMETRIA" | jq -r '.servicos[] | "  \(.nome): \(.quantidadeChamadas) chamadas, media \(.mediaTempoRespostaMs)ms, p95 \(.p95TempoRespostaMs)ms"'
[ "$(echo "$TELEMETRIA" | jq -r '[.servicos[].nome] | index("simular-investimento")')" != "null" ] \
  && ok "telemetria por servico" || falha "servico sem telemetria"

titulo "Tratamento de erros"
esperar_status POST /simular-investimento 422 "$TOKEN" '{"clienteId":123,"valor":10000,"prazoMeses":72,"tipoProduto":"CDB"}'
esperar_status POST /simular-investimento 404 "$TOKEN" '{"clienteId":999,"valor":10000,"prazoMeses":12,"tipoProduto":"CDB"}'
esperar_status POST /simular-investimento 400 "$TOKEN" '{"clienteId":123,"valor":-1,"prazoMeses":0,"tipoProduto":""}'
esperar_status POST /simular-investimento 400 "$TOKEN" '{"clienteId":'
esperar_status GET /produtos-recomendados/Arrojado 400 "$TOKEN"

printf '\n'
if [ "$FALHAS" -eq 0 ]; then
  verde "Smoke test concluido com sucesso: todos os cenarios passaram."
  exit 0
fi
vermelho "Smoke test finalizado com $FALHAS falha(s)."
exit 1
