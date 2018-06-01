#!/usr/bin/env bash

if [ -z "$CATALINA_HOME" ]; then
	echo "\$CATALINA_HOME not set" >&2
	exit 1
fi

TODAYS_DATE=`date +%Y-%m-%d`
CATALINA_LOG_DIR="$CATALINA_HOME/logs"

LOG_FILES=("$CATALINA_LOG_DIR/catalina.$TODAYS_DATE.log" "$CATALINA_LOG_DIR/catalina.out")
OUTPUT_ZIP="appServerLogs.zip"

for file in "${LOG_FILES[@]}"
do
    if [ -f "$file" ]; then
        LOGS_TO_GATHER="$LOGS_TO_GATHER $file"
    fi
done

zip ${OUTPUT_ZIP} ${LOGS_TO_GATHER}