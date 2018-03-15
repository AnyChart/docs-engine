[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)

# AnyChart Docs Engine

[AnyChart Documentation](https://docs.anychart.com/) is a service with all the reference information that users might
 need while working with AnyChart products. The application consists of two parts: the web part and the generator.
The Web part is just a site responsible for displaying the content, and the generator is a parser whose main task is
to parse the [corresponding repository](https://github.com/AnyChart/docs.anychart.com).
Each article there is written using Markdown format. Both application parts communicate via Redis queues. 
The application backend is written on Clojure and the frontend on Javascript with jQuery.



[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=master)](https://travis-ci.com/AnyChart/docs-engine)
[Production](http://docs.anychart.com)

[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=staging)](https://travis-ci.com/AnyChart/docs-engine)
[Staging](http://docs.anychart.stg)


## Install dependencies

```
sudo apt-get install phantomjs

sudo apt-get install redis-server

sudo apt-get install sphinxsearch
sudo mkdir -p /var/data/sphinx/

sudo npm install grunt-cli -g
```


## Setup local database
The application uses PostgreSQL, so you need to create database and user:
```
CREATE USER docs_user WITH PASSWORD 'pass';
CREATE DATABASE docs_db;
GRANT ALL PRIVILEGES ON DATABASE docs_db TO docs_user;
psql -p5432 -d docs_db -U docs_user -W
```

## License
If you have any questions regarding licensing - please contact us. <sales@anychart.com>
