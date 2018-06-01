FROM @JRE@

USER root
WORKDIR /root/docker-compile

# update packages and install dependencies: wget
RUN apt-get update \
	&& apt-get install -y wget procps unzip zip

# add jboss zip
ADD ./@ZIP_FILENAME@ ./

RUN unzip ./@ZIP_FILENAME@ -d /opt
# FIXME can this env var be set automatically?
ENV JBOSS_HOME=/opt/@JBOSS_HOME_DIR@

RUN mkdir /root/docker-stage
WORKDIR /root/docker-stage

# add scripts
ADD ./*.sh ./

# agent related stuff
RUN mkdir /root/docker-stage/aiagent
ENV AGENT_JAR_NAME @AGENT_JAR_NAME@
ADD ./aiagent/ /root/docker-stage/aiagent/
ADD ./*_AI-Agent.xml /root/docker-stage/

EXPOSE 8080
CMD ./startServer.sh