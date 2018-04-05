#!/bin/bash

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

if [ -z "$1" ]; then
	echo "Nothing given to undeploy"
	exit 2
fi

WARFILE=`readlink -f $1`
DEPLOYED_WAR=$JBOSS_HOME/standalone/deployments/$WARFILE

if [ ! -e $1 ]; then
	echo "File '$DEPLOYED_WAR' does not exist" >&2
	exit 3
fi

rm $DEPLOYED_WAR