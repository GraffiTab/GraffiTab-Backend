#!/bin/bash

MYSQL_IMAGE=graffitab/mysql:5.6
cd mysql
docker rmi $MYSQL_IMAGE -f
docker build -t $MYSQL_IMAGE .