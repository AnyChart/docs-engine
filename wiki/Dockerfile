FROM docker.anychart.dev/wiki_base

RUN mkdir -p /var/log/supervisor
RUN mkdir -p /app/wiki

ADD configs/wiki.supervisor.conf /etc/supervisor/conf.d/wiki.conf
ADD configs/app.nginx.conf /etc/nginx/conf.d/app.nginx.conf
ADD static /app/wiki/static
ADD src/templates /app/wiki/src/templates
ADD target/wiki-0.1.0-SNAPSHOT-standalone.jar ./wiki.jar