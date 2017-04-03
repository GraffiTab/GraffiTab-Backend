#!/bin/bash

export REDIS_PASSWORD=$1

docker-compose down

if [ $# != 1 ]; then
   echo "Usage: $0 <redis-password>"
   exit 1
fi

docker-compose up
