#!/bin/bash

if [ -z "$JETTY_BASE" ]; then
	echo "\$JETTY_BASE not set" >&2
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

cp $WARFILE $JETTY_BASE/webapps