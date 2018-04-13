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
java -jar "$JETTY_HOME/start.jar" "$@" STOP.PORT=$JETTY_STOP_PORT STOP.KEY=$JETTY_STOP_KEY