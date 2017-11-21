#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

tail `ls -1td $CATALINA_HOME/logs/* | head -n1`