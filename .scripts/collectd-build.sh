#!/usr/bin/env bash
pwd
ls -FAl
./gradlew --info --stacktrace -DisBuildServer=true --warning-mode=all :collectd:build
