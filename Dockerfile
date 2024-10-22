# Use a lightweight JDK 17 image
FROM openjdk:17-jdk-slim AS build

# Set the working directory for the build
WORKDIR /app

# Copy the Gradle build files and build the project
COPY ./gradlew .
COPY ./gradle ./gradle
COPY ./src ./src

# Use a lightweight JDK 17 image for the final image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the build stage into the container
COPY --from=build /app/build/libs/*.jar app.jar

# Copy the config.properties file
COPY ./src/main/resources/config.properties config.properties

EXPOSE 1211

# Run the Kotlin Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
