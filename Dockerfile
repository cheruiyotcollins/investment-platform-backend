# Stage 1: Build the application with Maven and JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image with JRE only (Java 21)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the JAR file
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Copy environment file (optional)
# COPY .env.docker.docker .env.docker

EXPOSE 9002

ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN useradd -m myuser && chown -R myuser:myuser /app
USER myuser

# Use environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]