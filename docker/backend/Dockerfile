# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /build
COPY backend/ .
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-noble

# Unprivileged runtime user (UID 10001) for TEN-63 / STRIDE E5 hardening.
RUN groupadd --system --gid 10001 app \
 && useradd  --system --uid 10001 --gid 10001 --home-dir /app --shell /usr/sbin/nologin app \
 && apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# bootJar produces app-<version>.jar (since TEN-67); copy via glob and normalise the name.
COPY --from=build --chown=app:app /build/app/build/libs/app-*.jar /app/app.jar

USER app:app
EXPOSE 8080

# /actuator/health is permitAll in SecurityConfig — safe public endpoint for liveness checks.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
