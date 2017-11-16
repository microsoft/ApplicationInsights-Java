#!/bin/sh

if [ -z $JBOSS_HOME ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

# TODO 