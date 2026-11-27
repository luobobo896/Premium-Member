# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY admin-server/ admin-server/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -f admin-server/pom.xml -pl admin-bootstrap -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /src/admin-server/admin-bootstrap/target/admin-bootstrap-0.1.0.jar /app/app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/urandom"
EXPOSE 5320
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:5320/api/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
