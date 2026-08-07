# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-jammy AS builder
WORKDIR /app
# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copy the source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar
# Render uses the PORT environment variable, Spring Boot will automatically pick this up if we pass it, but standard is 8080 internally
EXPOSE 8080
# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
