#!/usr/bin/env bash

if [ -z "$JETTY_BASE" ]; then
	echo "\$JETTY_BASE not set" >&2
	exit 1
fi

TODAYS_DATE=`date +%Y_%m_%d`
LOG_FILE=$JETTY_BASE/logs/$TODAYS_DATE.jetty.log

if [ ! -z "$1" ]; then
	LOG_FILE=$1
fi

NUM_LINES=50
if [ ! -z "$2" ]; then
	NUM_LINES=$2
fi

if ! tail -v -n$NUM_LINES $LOG_FILE; then
	echo "USAGE: tailLastLog.sh [logfile] [numlines]"
	echo "	logfile		- absolute path to logfile to tail (default="
	echo "	numlines	- number of lines to taile (default=50)"
fi