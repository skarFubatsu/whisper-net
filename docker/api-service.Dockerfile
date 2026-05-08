# syntax=docker/dockerfile:1.7

FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src/proto-schema/pom.xml src/proto-schema/pom.xml
COPY src/shared-kernel/pom.xml src/shared-kernel/pom.xml
COPY src/api-service/pom.xml src/api-service/pom.xml
COPY src/simulation-service/pom.xml src/simulation-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl src/api-service -am -DskipTests dependency:go-offline

COPY src/proto-schema/src src/proto-schema/src
COPY src/shared-kernel/src src/shared-kernel/src
COPY src/api-service/src src/api-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl src/api-service -am -DskipTests package

FROM gcr.io/distroless/java21-debian12:nonroot
WORKDIR /app

COPY --from=builder /workspace/src/api-service/target/api-service-0.1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
