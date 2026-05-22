FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

COPY api/ ./api/

WORKDIR /app/services

COPY services/pom.xml ./pom.xml
COPY services/vector-search-service/pom.xml ./vector-search-service/pom.xml
COPY services/vector-search-service/src ./vector-search-service/src

RUN mvn clean package -pl vector-search-service -am -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/services/vector-search-service/target/vector-search-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
