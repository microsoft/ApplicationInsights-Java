FROM @JRE@

USER root
WORKDIR /root/docker-compile

# TODO add label?

RUN mkdir /root/docker-stage

# update packages and install dependencies: wget
RUN apt-get update \
	&& apt-get install -y wget

ENV TOMCAT_MAJOR_VERSION 7
ENV TOMCAT_FULL_VERSION 7.0.84

# install tomcat (FXIME gpg?)
RUN wget https://archive.apache.org/dist/tomcat/tomcat-$TOMCAT_MAJOR_VERSION/v$TOMCAT_FULL_VERSION/bin/apache-tomcat-$TOMCAT_FULL_VERSION.tar.gz \
	&& wget https://archive.apache.org/dist/tomcat/tomcat-$TOMCAT_MAJOR_VERSION/v$TOMCAT_FULL_VERSION/bin/apache-tomcat-$TOMCAT_FULL_VERSION.tar.gz.sha1 \
	&& sha1sum --check apache-tomcat-$TOMCAT_FULL_VERSION.tar.gz.sha1 \
	&& tar xzvf apache-tomcat-$TOMCAT_FULL_VERSION.tar.gz \
	&& mv ./apache-tomcat-$TOMCAT_FULL_VERSION /opt/apache-tomcat-$TOMCAT_FULL_VERSION

ENV CATALINA_HOME /opt/apache-tomcat-$TOMCAT_FULL_VERSION
ENV CATALINA_BASE /opt/apache-tomcat-$TOMCAT_FULL_VERSION

ADD ./deploy.sh /root/docker-stage/deploy.sh
ADD ./tailLastLog.sh /root/docker-stage/tailLastLog.sh

EXPOSE 8080

WORKDIR /root/docker-stage
CMD $CATALINA_HOME/bin/catalina.sh run