FROM docker.anychart.dev/ubuntu-oracle-java
MAINTAINER Aleksandr Batsuev <batsuev@gmail.com>

RUN apt-get -y install sphinxsearch && apt-get clean
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

RUN cd /usr/bin && wget https://raw.github.com/technomancy/leiningen/stable/bin/lein && chmod a+x lein