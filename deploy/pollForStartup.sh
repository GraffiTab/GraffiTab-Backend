#!/bin/bash
sleep 5
VAL=$(tail -n 5 /tmp/graffitab.log | grep "Started GraffitabApplication in")
COUNT=1
while [ -z "$VAL" ] && [ $COUNT -lt 60 ]; do
echo "Waiting for startup ... $COUNT"
sleep 2
COUNT=$((COUNT+1))
VAL=$(tail -n 5 /tmp/graffitab.log | grep "Started GraffitabApplication in")
done

if [ $COUNT = 60 ]
then
echo "Timeout starting application (120s) -- failing build"
exit 1
else
echo "Startup finished: $VAL"
fi