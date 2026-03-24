# Step 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application (skip tests to speed up)
RUN mvn clean package -DskipTests

# Step 2: Run stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
