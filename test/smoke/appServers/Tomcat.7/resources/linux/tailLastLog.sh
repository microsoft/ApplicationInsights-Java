#!/bin/sh

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

NUM_LINES=25
if [ ! -z "$1" ]; then
	NUM_LINES=$1
fi

ls -1td $CATALINA_HOME/logs

tail -n$NUM_LINES `ls -1td $CATALINA_HOME/logs/* | head -n1`