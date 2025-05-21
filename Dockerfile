FROM maven:3.9-eclipse-temurin-21 AS builder
LABEL authors="axltorres"

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk AS runtime
LABEL authors="axltorres"

WORKDIR /app

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/ingSoftware
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=asd
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
