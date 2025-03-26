FROM eclipse-temurin:21-jdk as build

WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x ./mvnw
RUN ./mvnw install -DskipTests

FROM eclipse-temurin:21-jre

VOLUME /tmp
COPY --from=build /workspace/app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"] 