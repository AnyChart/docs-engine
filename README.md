Building:

       cd worker
       lein uberjar
       docker build -t=docker.anychart.dev/wiki_prod_worker .
       docker push docker.anychart.dev/wiki_prod_worker

       cd ../wiki
       lein uberjar
       docker build -t=docker.anychart.dev/wiki_prod_webapp .
       docker push docker.anychart.dev/wiki_prod_webapp

Deploying:

       docker run -name wiki_redis -p 6379 -d=true docker.anychart.dev/wiki_redis /usr/bin/redis-server
       docker run -d=true -link=wiki_redis:redis -v=/wiki_data:/wiki -name wiki_worker docker.anychart.dev/wiki_prod_worker bash -c 'supervisord'
       docker run -d=true -link=wiki_redis:redis -v=/wiki_data:/wiki -p=49001:49001 -name wiki_webapp docker.anychart.dev/wiki_prod_webapp bash -c 'service nginx start && supervisord'