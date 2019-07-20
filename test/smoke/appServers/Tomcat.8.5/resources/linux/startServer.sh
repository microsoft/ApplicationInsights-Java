#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
    echo "\$CATALINA_HOME not set" >&2
    exit 1
fi

if [ ! -z "$AI_AGENT_MODE" ]; then

    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./${AI_AGENT_MODE}_AI-Agent.xml ./aiagent/AI-Agent.xml

    export CATALINA_OPTS="-javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME $CATALINA_OPTS"
fi

export CATALINA_OPTS="-Xdebug -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n $CATALINA_OPTS"

$CATALINA_HOME/bin/catalina.sh run
