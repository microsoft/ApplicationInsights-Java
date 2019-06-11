#!/usr/bin/env bash

if [ -z "$JETTY_BASE" ]; then
	echo "\$JETTY_BASE not set" >&2
	exit 1
fi

if [ -z "$JETTY_HOME" ]; then
	echo "\$JETTY_HOME not set" >&2
	exit 2
fi

if [ -z "$JETTY_STOP_PORT" ]; then
    echo "\$JETTY_STOP_PORT not set" >&2
    exit 3
fi

if [ -z "$JETTY_STOP_KEY" ]; then
    echo "\$JETTY_STOP_KEY not set" >&2
    exit 4
fi

cd $JETTY_BASE
export JETTY_ARGS="STOP.PORT=$JETTY_STOP_PORT STOP.KEY=$JETTY_STOP_KEY $JETTY_ARGS"

if [ ! -z "$APPLICATION_INSIGHTS_CONFIG" ]; then

    echo "APPLICATION_INSIGHTS_CONFIG=$APPLICATION_INSIGHTS_CONFIG"
    cp -f /root/docker-stage/${APPLICATION_INSIGHTS_CONFIG}_ApplicationInsights.xml /root/docker-stage/aiagent/ApplicationInsights.xml

    export JAVA_OPTIONS="-javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME $JAVA_OPTIONS"

elif [ ! -z "$AI_AGENT_CONFIG" ]; then

    echo "AI_AGENT_CONFIG=$AI_AGENT_CONFIG"
    cp -f /root/docker-stage/${AI_AGENT_CONFIG}_AI-Agent.xml /root/docker-stage/aiagent/AI-Agent.xml

    export JAVA_OPTIONS="-javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME $JAVA_OPTIONS"
fi

$JETTY_HOME/bin/jetty.sh run
