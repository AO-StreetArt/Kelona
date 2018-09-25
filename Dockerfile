################################################################

# Dockerfile to build Kelona Container Images

################################################################


FROM openjdk:8-jdk-alpine
MAINTAINER Alex Barry
VOLUME /tmp
ADD build/libs/avc-0.0.1.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar
