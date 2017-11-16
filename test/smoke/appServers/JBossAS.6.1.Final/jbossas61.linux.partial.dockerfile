FROM @JRE@

USER root
WORKDIR /root/docker-compile

# update current packages and install dependecies: wget
RUN apt-get update \
    && apt-get install -y wget

# install jboss
RUN wget http://download.jboss.org/jbossas/6.1/jboss-as-distribution-6.1.0.Final.zip \
    && unzip ./jboss-as-distribution-6.1.0.Final.zip -d /opt

ENV JBOSS_HOME /opt/jboss-6.1.0.Final

# create runas user
RUN adduser --disabled-password pilot

# change permissions for jboss
RUN chown -R pilot:pilot $JBOSS_HOME

USER pilot
WORKDIR /opt/jboss-6.1.0.Final/bin

EXPOSE 8080

ADD ./deploy.sh /home/pilot/deploy.sh

CMD [ "./run.sh", "-b 0.0.0.0", "-c jbossweb-standalone" ]