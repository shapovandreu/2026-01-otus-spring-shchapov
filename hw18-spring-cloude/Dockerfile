# Образ приложения на основе заранее собранного jar.
# Сначала соберите модуль (mvn package), затем docker compose up --build.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
