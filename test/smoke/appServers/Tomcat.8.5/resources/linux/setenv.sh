#!/usr/bin/env bash

CATALINA_OPTS="-javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME -Xdebug -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n $CATALINA_OPTS"
