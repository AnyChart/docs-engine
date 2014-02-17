# RUN-USING:         docker run -d=true -link=wiki_redis:redis -name wiki_worker docker.anychart.dev/wiki_prod_worker bash -c 'supervisord'
FROM docker.anychart.dev/ubuntu-oracle-java

MAINTAINER Aleksandr Batsuev <batsuev@gmail.com>

RUN apt-get install -y git

RUN mkdir -p /var/log/supervisor
RUN mkdir -p /app/worker

VOLUME ["/wiki_data:/wiki"]

ADD configs/worker.supervisor.conf /etc/supervisor/conf.d/worker.conf
ADD target/worker-0.1.0-SNAPSHOT-standalone.jar ./worker.jar
ADD keys /app/keys