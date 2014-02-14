Redis container (for pub/sub):

       docker run -name wiki_redis -p 6379 docker.anychart.dev/wiki_redis /usr/bin/redis-server

Worker container:

       docker run -t -i -p=49000:49000 -link=wiki_redis:redis -v=/wiki_data:/wiki -v=/Work/anychart/docs.anychart.com:/app docker.anychart.dev/wiki_worker bash
       cd /app/worker
       export LEIN_ROOT=1
       lein repl :start :host "0.0.0.0" :port 49000

Application container:

       docker run -t -i -p 49001:49001 -p 49002:49002 -v=/wiki_data/:/wiki -v=/Work/anychart/docs.anychart.com/:/app -link=wiki_redis:redis docker.anychart.dev/wiki_webapp bash
       cd /app/wiki
       export LEIN_ROOT=1
       lein repl :start :host "0.0.0.0" :port 49002