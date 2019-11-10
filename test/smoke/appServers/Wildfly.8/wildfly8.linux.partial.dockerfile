FROM @JRE@

USER root
WORKDIR /root/docker-compile

RUN mkdir /root/docker-stage

# update packages and install dependencies: wget
RUN if type "apt-get" > /dev/null; then \
      apt-get update && apt-get install -y wget procps; \
    else \
      yum install -y wget procps; \
    fi

ENV WILDFLY_VERSION 8.2.1.Final

# install wildfly
RUN wget https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && tar xzvf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv ./wildfly-$WILDFLY_VERSION /opt/wildfly-$WILDFLY_VERSION

ENV WILDFLY_HOME=/opt/wildfly-$WILDFLY_VERSION


ADD ./*.sh /root/docker-stage/

# agent related stuff
RUN mkdir /root/docker-stage/aiagent
ENV AGENT_JAR_NAME @AGENT_JAR_NAME@
ADD ./aiagent/ /root/docker-stage/aiagent/
ADD ./*_AI-Agent.xml /root/docker-stage/

EXPOSE 8080

WORKDIR /root/docker-stage
CMD ./startServer.sh
