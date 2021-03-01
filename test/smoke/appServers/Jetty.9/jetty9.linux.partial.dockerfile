FROM @JRE@

USER root
WORKDIR /root/docker-compile

# update packages and install dependencies: wget
RUN if type "apt-get" > /dev/null; then \
      apt-get update && apt-get install -y wget procps; \
    else \
      yum install -y wget procps; \
    fi

ENV JETTY_FULL_VERSION 9.4.17.v20190418

# install jetty
RUN wget https://repo1.maven.org/maven2/org/eclipse/jetty/jetty-distribution/$JETTY_FULL_VERSION/jetty-distribution-$JETTY_FULL_VERSION.tar.gz \
    && tar xzvf jetty-distribution-$JETTY_FULL_VERSION.tar.gz \
    && mv ./jetty-distribution-$JETTY_FULL_VERSION /opt/jetty-distribution-$JETTY_FULL_VERSION

ENV JETTY_HOME /opt/jetty-distribution-$JETTY_FULL_VERSION

ENV JETTY_STOP_PORT 38899
ENV JETTY_STOP_KEY stopitplease

RUN mkdir /opt/jetty-base
ENV JETTY_BASE /opt/jetty-base/

RUN java -jar $JETTY_HOME/start.jar jetty.base=$JETTY_BASE --add-to-start=http,jsp,deploy,jstl --update-ini jetty.http.port=8080 --update-ini jetty.deploy.extractWars=true

RUN cp -r $JETTY_HOME/demo-base/webapps/ROOT $JETTY_BASE/webapps/

RUN mkdir /root/docker-stage
ADD ./*.sh /root/docker-stage/

# agent related stuff
RUN mkdir /root/docker-stage/aiagent
ENV AGENT_JAR_NAME @AGENT_JAR_NAME@
ADD ./aiagent/ /root/docker-stage/aiagent/
ADD ./*_AI-Agent.xml /root/docker-stage/

EXPOSE 8080

WORKDIR /root/docker-stage
CMD ./startServer.sh
