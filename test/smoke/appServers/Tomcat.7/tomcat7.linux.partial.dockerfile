
FROM @JRE@

USER root
WORKDIR /root/docker-compile

# TODO add label?

RUN mkdir /root/docker-stage

# update packages and install dependencies: wget
RUN apt-get update \
	&& apt-get install -y wget

# install tomcat (FXIME gpg?)
RUN wget http://www-eu.apache.org/dist/tomcat/tomcat-7/v7.0.82/bin/apache-tomcat-7.0.82.tar.gz \
	&& wget https://www.apache.org/dist/tomcat/tomcat-7/v7.0.82/bin/apache-tomcat-7.0.82.tar.gz.sha1 \
	&& sha1sum --check apache-tomcat-7.0.82.tar.gz.sha1 \
	&& tar xzvf apache-tomcat-7.0.82.tar.gz \
	&& mv ./apache-tomcat-7.0.82 /opt/apache-tomcat-7.0.82

ENV CATALINA_HOME /opt/apache-tomcat-7.0.82
ENV CATALINA_BASE /opt/apache-tomcat-7.0.82

ONBUILD ADD ./deploy.sh /root/docker-stage/deploy.sh
ONBUILD ADD ./tomcat-users.xml ${CATALINA_HOME}/conf/tomcat-users.xml

EXPOSE 8080

WORKDIR /root/docker-stage
CMD $CATALINA_HOME/bin/catalina.sh run