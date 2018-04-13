#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

if [ ! -z "$AGENT_JAR_NAME" ]; then
    cp ./setenv.sh $CATALINA_HOME/bin/setenv.sh
fi

$CATALINA_HOME/bin/catalina.sh run