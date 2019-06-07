#!/bin/bash

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

if [ ! -z "$AI_AGENT_CONFIG" ]; then

    echo "AI_AGENT_CONFIG=$AI_AGENT_CONFIG"
    cp -f ./${AI_AGENT_CONFIG}_AI-Agent.xml ./aiagent/AI-Agent.xml

    if [ ! -z "$APPLICATION_INSIGHTS_CONFIG" ]; then
        echo "APPLICATION_INSIGHTS_CONFIG=$APPLICATION_INSIGHTS_CONFIG"
        cp -f ./${APPLICATION_INSIGHTS_CONFIG}_ApplicationInsights.xml ./aiagent/ApplicationInsights.xml
    fi

    echo "JAVA_OPTS=\"\$JAVA_OPTS -javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME\"" >> $JBOSS_HOME/bin/standalone.conf
fi

$JBOSS_HOME/bin/standalone.sh -b 0.0.0.0