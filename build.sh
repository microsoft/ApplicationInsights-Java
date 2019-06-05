#!/bin/sh
export DOCKER_EXE=`which docker`
export PATH=.:$PATH
#gradlew --info clean build jar --stacktrace
#gradlew --info build jar --stacktrace -DDEBUG=true run
gradlew -DDEBUG=true smokeTest
