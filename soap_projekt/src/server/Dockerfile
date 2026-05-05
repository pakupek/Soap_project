FROM eclipse-temurin:8-jre
WORKDIR /app
COPY out/artifacts/soap_projekt_jar/soap_projekt.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]