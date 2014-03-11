Building:

       cd worker
       lein uberjar
       docker build -no-cache -t=docker.anychart.dev/wiki_prod_worker .
       docker push docker.anychart.dev/wiki_prod_worker:1.0.0

       cd ../wiki
       lein uberjar
       docker build -no-cache -t=docker.anychart.dev/wiki_prod_webapp:1.0.0 .
       docker push docker.anychart.dev/wiki_prod_webapp

       cd ../indexer
       lein uberjar
       docker build -no-cache -t=docker.anychart.dev/wiki_prod_indexer:1.0.0 .
       docker push docker.anychart.dev/wiki_prod_webapp

Deploying:

       docker run -name wiki_redis -p 6379 -d=true docker.anychart.dev/wiki_redis /usr/bin/redis-server
       docker run -d=true -link=wiki_redis:redis -v=/wiki_data:/wiki -name wiki_worker docker.anychart.dev/wiki_prod_worker:1.0.0 bash -c 'supervisord'
       docker run -d=true -link=wiki_redis:redis -v=/wiki_data:/wiki -p=49001:49001 -name wiki_webapp docker.anychart.dev/wiki_prod_webapp:1.0.0 bash -c 'service nginx start && supervisord'

Development:

	docker run -t -i -link=wiki_redis:redis -p=49000:49000 -v=/wiki_data:/wiki -v=/Work/anychart/docs.anychart.com:/app -name=wiki_worker docker.anychart.dev/wiki_worker bash

	docker run -t -i -v /wiki_data:/wiki -p 49005:49005 -link wiki_redis:redis -name wiki_indexer docker.anychart.dev/wiki_indexer bash

	docker run -t -i -link=wiki_redis:redis -link=wiki_indexer:indexer -v=/wiki_data:/wiki -v=/Work/anychart/docs.anychart.com:/app -p=49001:49001 -p=49002:49002 -name=wiki_webapp docker.anychart.dev/wiki_webapp bash