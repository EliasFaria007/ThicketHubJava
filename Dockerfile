# =========================================================================
# Multi-stage Dockerfile otimizado para Northflank (Plano Free / Low Memory)
# Java 21 + Spring Boot 3
# =========================================================================

# -------------------------------------------------------------------------
# Estágio 1: Build da aplicação com Maven e JDK 21 Alpine
# -------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache do arquivo de dependências (evita re-download desnecessário de dependências)
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copia código-fonte e compila gerando o JAR executável
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------------------
# Estágio 2: Imagem final ultraleve de runtime (JRE 21 Alpine, ~150MB)
# -------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Criação de grupo e usuário sem privilégios de root por segurança
RUN addgroup -S thicketgroup && adduser -S thicketuser -G thicketgroup

# Copia o JAR empacotado pelo Maven (thickethub.jar)
COPY --from=builder /app/target/*.jar app.jar

# Ajuste de permissões de arquivo
RUN chown -R thicketuser:thicketgroup /app
USER thicketuser

# Configuração de variáveis de ambiente otimizadas para planos gratuitos (512MB RAM):
# - MaxRAMPercentage=75.0: Reserva 25% da memória física do container para Metaspace/Thread Stacks
# - UseSerialGC: Reduz drasticamente o consumo de memória e threads em CPUs de 0.2 a 0.5 vCPU
# - ExitOnOutOfMemoryError: Encerra o processo imediatamente em caso de OOM para permitir restart limpo
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# Health check para garantir disponibilidade do container
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:${PORT}/api/actuator/health || exit 1

# Inicialização com substituição de processo via exec
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
