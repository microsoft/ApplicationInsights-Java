#!/bin/sh

while [ ! -f app.jar ]
do
  echo "waiting for app.jar ..."
  sleep 1
done

if [ ! -z "$AI_AGENT_MODE" ]; then

    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./${AI_AGENT_MODE}_applicationinsights.json ./aiagent/applicationinsights.json

    export JAVA_OPTS="-javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME $JAVA_OPTS"
fi

export JAVA_OPTS="-Xdebug -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n $JAVA_OPTS"

java $JAVA_OPTS -jar app.jar
