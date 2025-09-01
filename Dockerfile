# Java 21 런타임 환경
FROM eclipse-temurin:21-jre-slim

# 빌드 결과 JAR 파일 복사 (GitHub Actions에서 빌드 후 전달)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 컨테이너 시작 시 실행 명령
ENTRYPOINT ["java", "-jar", "/app.jar"]
