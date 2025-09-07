FROM eclipse-temurin:21
WORKDIR /opt/app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
CMD ["java", "-jar", "app.jar"]
