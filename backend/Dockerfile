FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --no-transfer-progress dependency:go-offline

COPY src src
RUN ./mvnw --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S pedeja && adduser -S pedeja -G pedeja

WORKDIR /app
COPY --from=build --chown=pedeja:pedeja /workspace/target/*.jar app.jar

USER pedeja
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-jar", "app.jar"]
