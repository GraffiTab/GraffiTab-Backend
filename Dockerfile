FROM isuper/java-oracle:jdk_8
RUN mkdir -p /app
ADD environment.sh /app/environment.sh
ADD graffitab.jar /app/graffitab.jar
WORKDIR /app
ENV SERVER_PORT 8091
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8 -Xms150m -Xmx200m -XX:MaxMetaspaceSize=120m"
ENTRYPOINT [ "sh", "-c", ". /app/environment.sh && java $JAVA_OPTS -jar graffitab.jar --server.port=$SERVER_PORT" ]