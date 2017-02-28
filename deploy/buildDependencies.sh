#!/bin/bash

# Build project dependencies (Gradle download)
source deploy/deployEnvironment.sh
./gradlew dependencies -Penv=$ENVNAME