#!/bin/bash

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

if [ -z "$1" ]; then
	echo "Nothing given to deploy"
	exit 0
fi

if [ ! -e $1 ]; then
	echo "File '$1' does not exist" >&2
	exit 2
fi

WARFILE=`readlink -f $1`
BASEPATH=`basename $WARFILE .war`

rm $CATALINA_HOME/webapps/$WARFILE