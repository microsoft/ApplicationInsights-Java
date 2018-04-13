#!/usr/bin/env bash

if [ -z "$JETTY_BASE" ]; then
	echo "\$JETTY_BASE not set" >&2
	exit 1
fi

if [ -z "$1" ]; then
	echo "Nothing given to undeploy"
	exit 0
fi

if [ ! -e $JETTY_BASE/webapps/$1 ]; then
	echo "WAR File '$1' does not exist" >&2
	exit 2
fi

rm $JETTY_BASE/webapps/$1