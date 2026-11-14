FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
COPY config ./config
RUN gradle --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd -r -u 10001 support
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER support
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
