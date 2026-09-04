# --- Stage 1: build the jar ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy the Maven wrapper first and pre-fetch dependencies (better layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Copy sources and package
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# --- Stage 2: minimal runtime image ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
