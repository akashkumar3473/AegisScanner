# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Copy maven wrapper (optional)
COPY .mvn ./.mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src
RUN mvn clean package -DskipTests

# Package stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/scanner-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
