#!/usr/bin/env bash

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

LOG_DIR="$JBOSS_HOME/standalone/log"

LOG_FILES=("$LOG_DIR/server.log")
OUTPUT_ZIP="appServerLogs.zip"

for file in "${LOG_FILES[@]}"
do
    if [ -f "$file" ]; then
        LOGS_TO_GATHER="$LOGS_TO_GATHER $file"
    fi
done

zip ${OUTPUT_ZIP} ${LOGS_TO_GATHER}