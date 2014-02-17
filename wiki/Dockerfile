# RUN-USING:         docker run -d=true -link=wiki_redis:redis -name wiki_webapp docker.anychart.dev/wiki_prod_webapp bash -c 'service nginx start && supervisord'
FROM docker.anychart.dev/ubuntu-oracle-java
MAINTAINER Aleksandr Batsuev <batsuev@gmail.com>

RUN add-apt-repository ppa:nginx/stable
RUN apt-get update && apt-get -y upgrade

RUN apt-get -y install nginx-full && apt-get clean
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

RUN mkdir -p /var/log/supervisor
RUN mkdir -p /app/wiki

EXPOSE 49001:49001
VOLUME ["/wiki_data:/wiki"]

ADD configs/wiki.supervisor.conf /etc/supervisor/conf.d/wiki.conf
ADD configs/app.nginx.conf /etc/nginx/conf.d/app.nginx.conf
ADD static /app/wiki/static
ADD src/templates /app/wiki/src/templates
ADD target/wiki-0.1.0-SNAPSHOT-standalone.jar ./wiki.jar