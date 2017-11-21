
FROM @JRE@

USER root
WORKDIR /root/docker-compile

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

ADD ./deploy.sh /root/docker-stage/deploy.sh
ADD ./tomcat-users.xml ${CATALINA_HOME}/conf/tomcat-users.xml

WORKDIR /root/docker-stage
EXPOSE 8080

CMD $CATALINA_HOME/bin/catalina.sh run