# ------- BUILD -------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
# Skipper la compilation ET l’exécution des tests
RUN mvn -q clean package \
    -DskipTests=true -Dmaven.test.skip=true \
    -Dspotbugs.skip=true -Dcheckstyle.skip=true

# ------- RUNTIME -------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# user non-root
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser

EXPOSE 9099

# IMPORTANT : on force l’adresse et le port ici (plus aucun doute)
ENTRYPOINT ["java","-Dserver.port=9099","-Dserver.address=0.0.0.0","-Xmx512m","-Xms256m","-jar","/app/app.jar"]

# (optionnel) enlève le healthcheck runtime si curl/wget non installés
# HEALTHCHECK NONE
