FROM @JRE@

USER root
WORKDIR /root/docker-compile

# TODO add label?

# update packages and install dependencies: wget
RUN apt-get update \
	&& apt-get install -y wget

# install jetty
RUN wget http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/9.4.8.v20171121/jetty-distribution-9.4.8.v20171121.tar.gz \
	&& tar xzvf jetty-distribution-9.4.8.v20171121.tar.gz \
	&& mv ./jetty-distribution-9.4.8.v20171121 /opt/jetty-distribution-9.4.8.v20171121

ENV JETTY_HOME /opt/jetty-distribution-9.4.8.v20171121
ENV JETTY_BASE /opt/jetty-distribution-9.4.8.v20171121/demo-base

# FIXME could these live elsewhere? or does it matter?
ADD ./deploy.sh /opt/jetty-distribution-9.4.8.v20171121/demo-base/deploy.sh
ADD ./tailLastLog.sh /opt/jetty-distribution-9.4.8.v20171121/demo-base/tailLastLog.sh

EXPOSE 8080

WORKDIR /opt/jetty-distribution-9.4.8.v20171121/demo-base
CMD java -jar ../start.jar