#!/bin/bash

if [ -z "$WILDFLY_HOME" ]; then
    echo "\$WILDFLY_HOME not set" >&2
    exit 1
fi

if [ ! -z "$AI_AGENT_MODE" ]; then
    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./${AI_AGENT_MODE}_AI-Agent.xml ./aiagent/AI-Agent.xml

    echo "JAVA_OPTS=\"\$JAVA_OPTS -javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME\"" >> $WILDFLY_HOME/bin/standalone.conf
fi

$WILDFLY_HOME/bin/standalone.sh -b 0.0.0.0
