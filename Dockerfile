# syntax=docker/dockerfile:1.7
# ---------------------------------------------------------------------------
# Build multi-stage: compila com JDK 25 + Maven e publica apenas o runtime
# Quarkus (fast-jar) na imagem final, sem ferramentas de build.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package

# ---------------------------------------------------------------------------
# Runtime: JRE 25 enxuto, usuario sem privilegios e diretorio de dados dedicado.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home app

WORKDIR /app
COPY --from=build /build/target/quarkus-app/ ./

RUN mkdir -p /data && chown app:app /data

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError --enable-native-access=ALL-UNNAMED" \
    QUARKUS_DATASOURCE_JDBC_URL="jdbc:sqlite:/data/investimentos.db?journal_mode=WAL&busy_timeout=10000"

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/q/health/ready || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/quarkus-run.jar"]
