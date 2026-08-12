FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --create-home --uid 10001 appuser

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "/app/app.jar"]