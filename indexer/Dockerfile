FROM docker.anychart.dev/wiki_indexer_base

RUN mkdir -p /sphinx/indexes && chown -R sphinxsearch /sphinx
RUN mkdir -p /sphinx/log/sphinx && chown -R sphinxsearch /sphinx
ADD target/indexer-0.1.0-SNAPSHOT-standalone.jar /app.jar