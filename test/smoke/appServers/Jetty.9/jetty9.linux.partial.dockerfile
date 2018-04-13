FROM @JRE@

WORKDIR /usr/local/docker-compile

# update packages and install dependencies: wget procps (we need 'ps' for debugging)
RUN apt-get update \
	&& apt-get install -y wget procps

ENV JETTY_FULL_VERSION 9.4.9.v20180320

# install jetty
RUN wget http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/$JETTY_FULL_VERSION/jetty-distribution-$JETTY_FULL_VERSION.tar.gz \
	&& tar xzvf jetty-distribution-$JETTY_FULL_VERSION.tar.gz \
	&& mv ./jetty-distribution-$JETTY_FULL_VERSION /opt/jetty-distribution-$JETTY_FULL_VERSION

ENV JETTY_HOME /opt/jetty-distribution-$JETTY_FULL_VERSION

RUN mkdir -p /opt/jetty-base
ENV JETTY_BASE /opt/jetty-base/

RUN mkdir -p /root/docker-stage
ADD ./*.sh /root/docker-stage/

ENV JETTY_STOP_PORT 38899
ENV JETTY_STOP_KEY stopitplease

WORKDIR /root/docker-stage

RUN java -jar $JETTY_HOME/start.jar jetty.base=$JETTY_BASE --add-to-start=http,jsp,deploy,jstl --update-ini jetty.http.port=8080 --update-ini jetty.deploy.extractWars=true
RUN java -jar $JETTY_HOME/start.jar jetty.base=$JETTY_BASE --add-to-start=console-capture --update-ini jetty.console-capture.timezone=PST

RUN cp -r $JETTY_HOME/demo-base/webapps/ROOT $JETTY_BASE/webapps/

EXPOSE 8080

CMD ./startServer.sh