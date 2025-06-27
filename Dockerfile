# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
LABEL authors="A Baaki"
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Run Stage ----
FROM openjdk:17-jdk-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=build /app/target/project-tracker-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
