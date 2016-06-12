#!/bin/bash

echo "Starting Application..."
source environment.sh

# -Djava.security.egd=file:/dev/./urandom to avoid Tomcat hanging on startup Ubuntu 14.04
# http://stackoverflow.com/questions/25660899/spring-boot-actuator-application-wont-start-on-ubuntu-vps
java -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8 -Xms150m -Xmx200m -XX:MaxMetaspaceSize=120m -jar graffitab.jar --server.port=$DO_SERVER_PORT --db.username=$DO_MYSQL_DB_USERNAME --db.password=$DO_MYSQL_DB_PASSWORD &