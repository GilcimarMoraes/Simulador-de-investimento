JAVA_HOME ?= $(shell dirname $$(dirname $$(readlink -f $$(which java))))
MVN ?= mvn

.PHONY: help build test run docker-build docker-up docker-down smoke clean

help:
	@echo "Alvos disponiveis:"
	@echo "  make build        - empacota a aplicacao (testes sao executados)"
	@echo "  make test         - executa testes unitarios e de integracao"
	@echo "  make run          - sobe a aplicacao local na porta 8080"
	@echo "  make docker-build - constroi a imagem Docker"
	@echo "  make docker-up    - sobe via docker compose"
	@echo "  make docker-down  - derruba o compose (mantem o volume de dados)"
	@echo "  make smoke        - executa o fluxo de smoke test contra localhost:8080"

build:
	$(MVN) -B clean package

test:
	$(MVN) -B test

run:
	$(MVN) -B quarkus:dev

docker-build:
	docker build -t painel-investimentos:1.0.0 .

docker-up:
	docker compose up --build -d
	@echo "API em http://localhost:8080/q/swagger-ui"

docker-down:
	docker compose down

smoke:
	./scripts/smoke-test.sh

clean:
	$(MVN) -B clean
