FROM @JRE@

USER root
WORKDIR /root/docker-compile

# update packages and install dependencies: wget
RUN if type "apt-get" > /dev/null; then \
      apt-get update && apt-get install -y wget procps; \
    else \
      yum install -y wget procps; \
    fi

ENV WILDFLY_FULL_VERSION 11.0.0.Final

# install tomcat
RUN wget http://download.jboss.org/wildfly/$WILDFLY_FULL_VERSION/wildfly-$WILDFLY_FULL_VERSION.tar.gz \
    && tar xzvf wildfly-$WILDFLY_FULL_VERSION.tar.gz \
    && mv ./wildfly-$WILDFLY_FULL_VERSION /opt/wildfly-$WILDFLY_FULL_VERSION

ENV JBOSS_HOME /opt/wildfly-$WILDFLY_FULL_VERSION

RUN mkdir /root/docker-stage
ADD ./*.sh /root/docker-stage/

# agent related stuff
RUN mkdir /root/docker-stage/aiagent
ENV AGENT_JAR_NAME @AGENT_JAR_NAME@
ADD ./aiagent/ /root/docker-stage/aiagent/
ADD ./*_applicationinsights.json /root/docker-stage/

EXPOSE 8080

WORKDIR /root/docker-stage
CMD ./startServer.sh
