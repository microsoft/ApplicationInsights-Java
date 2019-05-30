#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

if [ ! -z "$AI_AGENT_CONFIG" ]; then
    echo "AI_AGENT_CONFIG=$AI_AGENT_CONFIG"
    echo "APPLICATION_INSIGHTS_CONFIG=$APPLICATION_INSIGHTS_CONFIG"
    cp -f ./setenv.sh $CATALINA_HOME/bin/setenv.sh
    cp -f ./${AI_AGENT_CONFIG}_AI-Agent.xml ./aiagent/AI-Agent.xml
    cp -f ./${APPLICATION_INSIGHTS_CONFIG}_ApplicationInsights.xml ./aiagent/ApplicationInsights.xml
fi

$CATALINA_HOME/bin/catalina.sh run