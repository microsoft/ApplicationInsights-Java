#!/bin/sh
export PATH=.:$PATH
gradlew --info build jar --stacktrace
