# Stage 1: Build the app using Maven with Java 23
FROM eclipse-temurin:23-jdk-alpine AS build
WORKDIR /app
COPY . .

# Install maven in the alpine container
RUN apk add --no-cache maven

RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]