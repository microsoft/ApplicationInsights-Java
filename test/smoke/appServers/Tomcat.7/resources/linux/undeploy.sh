#!/bin/bash

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

if [ -z "$1" ]; then
	echo "Nothing given to undeploy"
	exit 0
fi

if [ ! -e $CATALINA_HOME/webapps/$1 ]; then
	echo "WAR File '$1' does not exist" >&2
	exit 2
fi

rm $CATALINA_HOME/webapps/$1