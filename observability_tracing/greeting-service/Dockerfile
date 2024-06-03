FROM azul/zulu-openjdk-alpine:17.0.11-jdk AS build
WORKDIR /app

COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY gradle gradle
COPY src src

RUN ./gradlew build

FROM azul/zulu-openjdk-alpine:17-jre
COPY --from=build /app/build/libs/greeting-service-0.0.1-SNAPSHOT-TRACING.jar app.jar
COPY opentelemetry-javaagent.jar opentelemetry-javaagent.jar
ENTRYPOINT ["java","-javaagent:opentelemetry-javaagent.jar", "-jar","/app.jar"]

