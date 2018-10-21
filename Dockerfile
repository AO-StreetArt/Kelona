################################################################

# Dockerfile to build Kelona Container Images

################################################################


FROM openjdk:8-jdk-alpine
MAINTAINER Alex Barry
VOLUME /tmp
ADD build/libs/avc-0.0.1.jar app.jar
ADD src/resources/log4j2.yaml log4j2.yaml
ADD src/resources/application.properties application.properties
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar
