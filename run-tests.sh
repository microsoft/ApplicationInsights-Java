#!/bin/sh
export PATH=.:$PATH
#gradlew --info clean build jar --stacktrace
#gradlew --info build jar --stacktrace -DDEBUG=true run
#gradlew -DEBUG=true --info clean :core:test --debug-jvm --tests *DockerContextInitializerTests
#gradlew -DEBUG=true --info clean :core:test --debug-jvm --tests *DockerContextInitializerTests
#gradlew -DEBUG=true --info test --debug-jvm
#gradlew --info test
#gradlew -DEBUG=true --info :core:test --debug-jvm --tests *DockerContextInitializerTests
#gradlew --info :core:test
#gradlew --info :test:smoke:build run
#
#    --tests com.microsoft.applicationinsights.extensibility.initializer.docker.DockerContextInitializerTests*
#.testSDKInfoFileIsWrittenWithInstrumentationKey
gradlew --info --stacktrace build test

export DOCKER_EXE=`where docker`
export DOCKER_CLI_EXE=`where docker`
docker network rm aismoke-net
docker network prune --force

gradlew --info --stacktrace smokeTest
