# ====== Étape 1 : Build (JDK 21 + Maven) ======
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copie minimale pour profiter du cache Maven
COPY pom.xml .
# (facultatif mais accélère les builds suivants)
RUN mvn -q -B -DskipTests dependency:go-offline

# Puis le code
COPY src ./src

# Build du fat-jar Spring Boot
RUN mvn -q -B -DskipTests clean package

# ====== Étape 2 : Runtime (JRE 21) ======
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# ton app écoute en interne sur 9099
EXPOSE 9099

# Copie du jar produit à l'étape de build
COPY --from=build /app/target/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
