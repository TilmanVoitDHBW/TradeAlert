# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .                         
RUN mvn package -DskipTests -pl stock-service -am

# Stage 2: Run
FROM eclipse-temurin:17-jre
WORKDIR /work/
COPY --from=build /app/stock-service/target/quarkus-app/ /work/quarkus-app/
EXPOSE 8083
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
CMD ["java", "-jar", "/work/quarkus-app/quarkus-run.jar"]