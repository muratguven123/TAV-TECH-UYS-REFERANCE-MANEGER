# ── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Build and install the shared library uys-events-contract
COPY uys-events-contract ./uys-events-contract
RUN mvn -f uys-events-contract/pom.xml clean install -DskipTests -q

# Build the referance-manager service
COPY referance_manager/referance_manager/pom.xml ./referance_manager/referance_manager/
COPY referance_manager/referance_manager/src ./referance_manager/referance_manager/src
RUN mvn -f referance_manager/referance_manager/pom.xml package -Dmaven.test.skip=true -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/referance_manager/referance_manager/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
