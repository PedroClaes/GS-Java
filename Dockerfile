# Build: compila dentro de um conteiner com Maven e JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

# Execucao: so o JRE e o jar, imagem bem menor
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/consertaai-api.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
