FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

COPY api/ ./api/

WORKDIR /app/services

COPY services/pom.xml ./pom.xml
COPY services/thesis-service/pom.xml ./thesis-service/pom.xml
COPY services/thesis-service/src ./thesis-service/src

RUN mvn clean package -pl thesis-service -am -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/services/thesis-service/target/thesis-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
