#!/bin/bash

if [ -z "$WILDFLY_HOME" ]; then
    echo "\$WILDFLY_HOME not set" >&2
    exit 1
fi

if [ -z "$1" ]; then
    echo "Nothing given to deploy"
    exit 2
fi

if [ ! -e $1 ]; then
    echo "File '$1' does not exist" >&2
    exit 3
fi

WARFILE=`readlink -f $1`
BASEPATH=`basename $WARFILE .war`

cp $WARFILE $WILDFLY_HOME/standalone/deployments/
