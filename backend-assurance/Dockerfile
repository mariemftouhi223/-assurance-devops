FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copier pom.xml pour cache des dépendances
COPY pom.xml .

# Télécharger dépendances (cache si pom.xml inchangé)
RUN mvn dependency:go-offline -B

# Copier code source
COPY src ./src

# Build optimisé
RUN mvn clean package -DskipTests -B -q

# Stage production
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Utilisateur non-root sécurisé
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copier JAR
COPY --from=build /app/target/*.jar app.jar

# Permissions
RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 9099

# JVM optimisée conteneur
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9099/actuator/health || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

