#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
    echo "\$CATALINA_HOME not set" >&2
    exit 1
fi

if [ ! -z "$AI_AGENT_MODE" ]; then
    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./setenv.sh $CATALINA_HOME/bin/setenv.sh
    cp -f ./${AI_AGENT_MODE}_ApplicationInsights.json ./aiagent/ApplicationInsights.json
fi

$CATALINA_HOME/bin/catalina.sh run