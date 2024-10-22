# Use a lightweight JDK 17 image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the Gradle build directory into the container
COPY ./build/libs/*.jar app.jar

# Copy the configuration file into the container
COPY ./src/main/resources/config.properties config.properties

RUN ls -la /app

EXPOSE 1211

# Run the Kotlin Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
