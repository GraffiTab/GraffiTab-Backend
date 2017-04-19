#!/bin/bash

REDIS_IMAGE=graffitab/redis:3
cd redis
echo "$1" > password.txt
docker rmi $REDIS_IMAGE -f
docker build -t $REDIS_IMAGE .
rm password.txt