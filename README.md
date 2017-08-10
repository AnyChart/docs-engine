[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)

# AnyChart Docs Engine


[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=master)](https://travis-ci.com/AnyChart/docs-engine)
[Production](http://docs.anychart.com)

[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=staging)](https://travis-ci.com/AnyChart/docs-engine)
[Staging](http://docs.anychart.stg)


## Install dependencies

```
sudo apt-get install phantomjs

sudo apt-get install redis-server
```


## Setup local database
```
CREATE USER docs_user WITH PASSWORD 'pass';
CREATE DATABASE docs_db;
GRANT ALL PRIVILEGES ON DATABASE docs_db TO docs_user;
psql -p5432 -d docs_db -U docs_user -W
```