# Multi-stage build for vCraft Spring Boot Backend
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B || mvn dependency:go-offline -B || true

COPY src src
RUN ./mvnw clean package -DskipTests -B

# Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S vcraft && adduser -S vcraft -G vcraft
RUN mkdir -p /app/uploads && chown -R vcraft:vcraft /app

COPY --from=builder /build/target/*.jar app.jar

USER vcraft
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
