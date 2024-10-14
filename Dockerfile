FROM amazoncorretto:17-alpine-jdk

COPY out/artifacts/redes_jar/redes.jar app.jar

ENTRYPOINT ["java", "-jar","/app.jar"]