#!/bin/bash
sleep 5
VAL=$(tail -n 5 /tmp/graffitab.log | grep "Started GraffitabApplication in")
COUNT=1
while [ -z "$VAL" ] && [ $COUNT -lt 80 ]; do
echo "Waiting for startup ... $COUNT"
sleep 2
COUNT=$((COUNT+1))
VAL=$(tail -n 5 /tmp/graffitab.log | grep "Started GraffitabApplication in")
done

if [ $COUNT = 80 ]
then
echo "Timeout starting application (160s) -- failing build"
exit 1
else
echo "Startup finished: $VAL"
fi