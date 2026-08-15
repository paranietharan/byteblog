FROM maven:3-eclipse-temurin-26 AS builder

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package \
    && cp target/byteblog-*.jar /workspace/application.jar

FROM eclipse-temurin:25.0.3_9-jre-alpine AS runtime

RUN apk upgrade --no-cache \
    && addgroup -S byteblog \
    && adduser -S -G byteblog -h /app byteblog

WORKDIR /app

COPY --from=builder --chown=byteblog:byteblog /workspace/application.jar /app/application.jar

USER byteblog:byteblog

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/application.jar"]
