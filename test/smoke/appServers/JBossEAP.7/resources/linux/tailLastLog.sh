#!/bin/sh

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

NUM_LINES=25
if [ ! -z "$1" ]; then
	NUM_LINES=$1
fi

tail -v -n$NUM_LINES $JBOSS_HOME/standalone/log/server.log