#!/bin/bash

if [ -z "$JBOSS_HOME" ]; then
	echo "\$JBOSS_HOME not set" >&2
	exit 1
fi

if [ ! -z "$AI_AGENT_MODE" ]; then
    echo "AI_AGENT_MODE=$AI_AGENT_MODE"
    cp -f ./${AI_AGENT_MODE}_AI-Agent.xml ./aiagent/AI-Agent.xml

    # there should only be one jar in this folder; so this command will work as long as that condition holds
    JBOSS_LOG_MANAGER_JAR=`ls $JBOSS_HOME/modules/system/layers/base/org/jboss/logmanager/main/*.jar`
    echo "JBOSS_LOG_MANAGER_JAR=$JBOSS_LOG_MANAGER_JAR"

    # these are for a known jboss issue with -javaagent
    echo "JAVA_OPTS=\"\$JAVA_OPTS -Djboss.modules.system.pkgs=org.jboss.byteman,org.jboss.logmanager,com.microsoft.applicationinsights.agent\"" >> $JBOSS_HOME/bin/standalone.conf
    echo "JAVA_OPTS=\"\$JAVA_OPTS -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Xbootclasspath/p:$JBOSS_LOG_MANAGER_JAR\"" >> $JBOSS_HOME/bin/standalone.conf

    echo "JAVA_OPTS=\"\$JAVA_OPTS -javaagent:/root/docker-stage/aiagent/$AGENT_JAR_NAME\"" >> $JBOSS_HOME/bin/standalone.conf
fi

$JBOSS_HOME/bin/standalone.sh -b 0.0.0.0