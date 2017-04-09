#!/bin/bash

ENVNAME=local
SERVER_IMAGE=graffitab/graffitab-server:0.1-local

docker rmi $SERVER_IMAGE -f

./gradlew clean stage -Penv=$ENVNAME -PexcludeDevtools=false
docker build -t $SERVER_IMAGE .
# mkdir -p ~/mysql-data

