# 첫 번째 스테이지: 빌드 스테이지
FROM gradle:jdk-21-and-23-graal-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# 소스 코드와 Gradle 래퍼 복사
COPY build.gradle settings.gradle ./
COPY service/queue-server/build.gradle service/queue-server/settings.gradle ./queue-server/

RUN ls
RUN ls ./queue-server
RUN cat queue-server/build.gradle
RUN cat settings.gradle
# 종속성 설치
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY service/queue-server/src ./service/queue-server/src

# 애플리케이션 빌드
RUN gradle  :service:queue-server:bootJar

# 두 번째 스테이지: 실행 스테이지
FROM container-registry.oracle.com/graalvm/jdk:21

# 작업 디렉토리 설정
WORKDIR /app


# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/service/queue-server/build/libs/*.jar app.jar

# 실행할 JAR 파일 지정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]