#!/bin/bash

PID=$(ps -ef | grep java.*\.jar | grep -v grep | awk '{ print $2 }')
if [ -z "$PID" ]
then
    client_result "Application is already stopped"
else
    echo "Stopping application with pid $PID"
    kill $PID
fi