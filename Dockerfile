# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Cache dependencies to speed up subsequent builds
RUN ./mvnw dependency:go-offline
COPY src ./src
# Build the JAR, skipping tests so the build doesn't fail if the DB isn't running yet
RUN ./mvnw clean package -DskipTests

# Stage 2: Production Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]