# ===============================
# 1. Build Stage
# ===============================
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# 의존성 캐시
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon

# 소스 복사
COPY src src

# 빌드
RUN gradle clean bootJar --no-daemon

# ===============================
# 2. Runtime Stage
# ===============================
FROM eclipse-temurin:17-jre
WORKDIR /app

# jar 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# ===== 추후 MSA 분리 대비 ENV =====
ENV SERVICE_NAME=groom-monolith
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=local

# Feign / 내부 통신은 localhost
ENV INTERNAL_BASE_URL=http://localhost:${SERVER_PORT}

# JVM 컨테이너 최적화
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
