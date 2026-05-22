FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

COPY api/ ./api/

WORKDIR /app/services

COPY services/pom.xml ./pom.xml
COPY services/scraping-service/pom.xml ./scraping-service/pom.xml
COPY services/scraping-service/src ./scraping-service/src

RUN mvn clean package -pl scraping-service -am -DskipTests

# Stage 2: Lightweight runtime environment
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/services/scraping-service/target/scraping-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
