FROM eclipse-temurin:17-jre
WORKDIR /work/
COPY target/quarkus-app/ /work/quarkus-app/
EXPOSE 8083
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
CMD ["sh","-lc","java $JAVA_OPTS -Dalphavantage.api-key=${ALPHAVANTAGE_API_KEY} -jar /work/quarkus-app/quarkus-run.jar"]