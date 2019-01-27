#!/bin/bash

PID=$(ps -ef | grep java.*\graffitab.jar | grep -v grep | awk '{ print $2 }')
if [ -z "$PID" ]
then
     echo "Application is already stopped"
     exit 0
else
    echo "Stopping application with pid $PID"
    sudo kill $PID
fi