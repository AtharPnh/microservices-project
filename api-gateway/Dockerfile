FROM amazoncorretto:8u372-alpine3.17-jre

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]