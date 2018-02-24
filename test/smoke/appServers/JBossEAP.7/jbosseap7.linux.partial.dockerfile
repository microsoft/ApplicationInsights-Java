FROM @JRE@

USER root
WORKDIR /root/docker-compile

# TODO add label?

RUN mkdir /root/docker-stage

# update packages and install dependencies: wget
RUN apt-get update \
	&& apt-get install -y wget \
	&& apt-get install -y procps

# add jboss installer
ADD @INSTALLER_FILENAME@ .

# TODO: install jboss

# TODO: set env vars

# add scripts
WORKDIR /root/docker-stage
ADD ./*.sh .

EXPOSE 8080

#TODO CMD