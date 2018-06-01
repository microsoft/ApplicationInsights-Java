#!/usr/bin/env bash

if [ -z "$JETTY_BASE" ]; then
	echo "\$JETTY_BASE not set" >&2
	exit 1
fi

TODAYS_DATE=`date +%Y_%m_%d`
LOGS_DIR="$JETTY_BASE/logs"

LOG_FILES=("$LOGS_DIR/$TODAYS_DATE.jetty.log")
OUTPUT_ZIP="appServerLogs.zip"

for file in "${LOG_FILES[@]}"
do
    if [ -f "$file" ]; then
        LOGS_TO_GATHER="$LOGS_TO_GATHER $file"
    fi
done

zip ${OUTPUT_ZIP} ${LOGS_TO_GATHER}