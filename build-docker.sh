#!/bin/bash

ENVNAME=local
SERVER_IMAGE=graffitab/graffitab-server:0.1-local

docker rmi $SERVER_IMAGE -f

./gradlew stage -Penv=$ENVNAME
docker build -t $SERVER_IMAGE .
# mkdir -p ~/mysql-data

