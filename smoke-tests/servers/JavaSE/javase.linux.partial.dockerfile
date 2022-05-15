FROM @JRE@

USER root
WORKDIR /root/docker-compile

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
