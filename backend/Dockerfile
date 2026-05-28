# ============================================================
# Stage 1 — build Maven com JDK 17
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache de dependencias: copia so o pom primeiro
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Copia codigo-fonte e empacota
COPY src ./src
RUN mvn -B -q -DskipTests package


# ============================================================
# Stage 2 — runtime JRE (Ubuntu 22.04 jammy, multi-arch)
# Sem dependencias extras - usamos bash /dev/tcp pro healthcheck.
# ============================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Jar empacotado pelo Spring Boot (finalName=app)
COPY --from=build /build/target/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
