FROM eclipse-temurin:17-jre
WORKDIR /work/
COPY target/quarkus-app/ /work/quarkus-app/
EXPOSE 8081
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
CMD ["java","-jar","/work/quarkus-app/quarkus-run.jar"]
