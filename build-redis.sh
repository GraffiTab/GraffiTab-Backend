#!/bin/bash

REDIS_IMAGE=graffitab/redis:3
cd redis
docker rmi $REDIS_IMAGE -f
docker build -t $REDIS_IMAGE .