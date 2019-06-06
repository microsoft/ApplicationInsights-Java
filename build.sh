#!/bin/sh
export DOCKER_EXE=`which docker`
export PATH=.:$PATH
gradlew --info --stacktrace clean build jar
#gradlew --info --stacktrace build jar -DDEBUG=true run
gradlew smokeTest
