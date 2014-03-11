# RUN-USING:         docker run -d=true -link=wiki_redis:redis -name wiki_webapp docker.anychart.dev/wiki_prod_webapp bash -c 'service nginx start && supervisord'
FROM docker.anychart.dev/ubuntu-oracle-java
MAINTAINER Aleksandr Batsuev <batsuev@gmail.com>

RUN add-apt-repository ppa:nginx/stable
RUN apt-get update && apt-get -y upgrade

RUN apt-get -y install nginx-full && apt-get clean
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle
