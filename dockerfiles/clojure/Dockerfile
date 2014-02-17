FROM base

MAINTAINER Aleksandr Batsuev <batsuev@gmail.com>

RUN apt-get -y install software-properties-common
RUN add-apt-repository ppa:webupd8team/java
RUN apt-get update && apt-get -y upgrade

# automatically accept oracle license
RUN echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
# and install java 7 oracle jdk
RUN apt-get -y install oracle-java7-installer supervisor && apt-get clean
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle