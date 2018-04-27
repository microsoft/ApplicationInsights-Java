#!/bin/bash

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

if [ ! -z "$AI_AGENT_MODE" ]; then
    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./${AI_AGENT_MODE}_AI-Agent.xml ./aiagent/AI-Agent.xml
    echo "JAVA_OPTS=\"$JAVA_OPTS -javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME $CATALINA_OPTS\"" >> $JBOSS_HOME/standalone/standalone.conf
fi

$JBOSS_HOME/bin/standalone.sh -b 0.0.0.0