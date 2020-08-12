#!/usr/bin/env bash

if [ ! -f "$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH" ]; then
    echo "CDP_USER_SOURCE_FOLDER_CONTAINER_PATH ('$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH') does not exist" > &2
    exit 1
fi

GRALDEW_PATH="$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH/gradlew"
if [ ! -f "$GRADLEW_PATH" ]; then
    echo "GRADLEW_PATH ('') does not exist" > &2
    exit 1
fi

$GRADLEW_PATH -p "$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH" --info --stacktrace -DisBuildServer=true --warning-mode=all :collectd:build

