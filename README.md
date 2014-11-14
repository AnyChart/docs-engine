# Deploying

## Install required software (ubuntu trusty server)

Добавить в конец /etc/apt/sources.list:
```
deb http://nginx.org/packages/mainline/ubuntu/ trusty nginx
deb-src http://nginx.org/packages/mainline/ubuntu/ trusty nginx
```

Добавить ключ nginx:
```
wget http://nginx.org/keys/nginx_signing.key
apt-key add nginx_signing.key
rm nginx_signing.key
```

Обновить репы:
```
apt-get update
```

Установить нужный софт:
```
apt-get install openjdk-7-jre supervisor nginx git redis-server
```

## Configure firewall
```
ufw allow ssh
ufw allow http
ufw enable
ufw status
```
status должен выдать такое:
```
Status: active

To                         Action      From
--                         ------      ----
22                         ALLOW       Anywhere
80                         ALLOW       Anywhere
22 (v6)                    ALLOW       Anywhere (v6)
80 (v6)                    ALLOW       Anywhere (v6)
```

# App structure
```
mkdir /apps
mkdir /apps/wiki
mkdir /apps/wiki/data
mkdir /apps/wiki/keys
```

# supervisor configuration
Создаем файл `/etc/supervisor/conf.d/wiki.conf`
```
[program:wiki]
command=java -Dprod=true -jar /apps/wiki/wiki-1.1-standalone.jar docs.anychart.com
stdout_logfile=/var/log/supervisor/docs.out.log
stderr_logfile=/var/log/supervisor/docs.err.log
```
Для staging:
1. Удаляем `-Dprod=true`
2. Меняем `docs.anychart.com` на `docs.anychart.stg`

Применяем изменения:
```
supervisorctl reread
supervisorctl update
supervisorctl status
```
Выдаст:
```
root@vm89445:~# supervisorctl reread
wiki: available
root@vm89445:~# supervisorctl update
wiki: added process group
root@vm89445:~# supervisorctl status
wiki                             BACKOFF    Exited too quickly (process log may have details)
```
`BACKOFF` это нормально - мы еще не выложили приложение.

# nginx configuration
Создаем `/etc/nginx/conf.d/docs.anychart.com.conf`
```
upstream http_backend {
    server 127.0.0.1:9095;
    keepalive 32;  # both http-kit and nginx are good at concurrency
}

server {
    listen       80;
    server_name  docs.anychart.com;

    location =/ { return 302 /7.2.0; }

    location / {
	proxy_pass  http://http_backend;

        # tell http-kit to keep the connection
        proxy_http_version 1.1;
        proxy_set_header Connection "";

        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Host $http_host;
    }
}
```
Для staging меняем:
1. `server_name  docs.anychart.com;` на `server_name  docs.anychart.stg;`
2. `location =/ { return 302 /7.2.0; }` на `location =/ { return 302 /develop; }`

Перезапускаем nginx:
```
service nginx configtest
service nginx restart
```
Должен выдать:
```
root@vm89445:~# service nginx configtest
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
root@vm89445:~# service nginx restart
 * Restarting nginx nginx        [ OK ]
 ```
 
 # Docs git key
 Создаем файл `/apps/wiki/keys/git`:
```
#!/bin/sh
exec /usr/bin/ssh -o StrictHostKeyChecking=no -i /apps/wiki/keys/id_rsa "$@"
```

Так же кладем приватный `id_rsa` и публичный `id_rsa.pub` ключи в папку `/apps/wiki/keys`
Я использую ключи anychart large conference room. По этим ключам должен быть разрешен доступ ко всем проектам, которые есть в playground.

Настраиваем права:
```
cd /apps/wiki/keys
chmod +x git
chmod go-rwx id_rsa
chmod go-rwx id_rsa.pub
```

Подключаем ключи:
```
export GIT_SSH="/apps/wiki/keys/git"
```

# Wiki projects
```
cd /apps/wiki/data
git clone git@github.com:AnyChart/ACDVF-docs.git repo
mkdir versions
```
git clone должен пройти без ошибок. Если что-то не так - значит косяк с ключами.

# Build docs-engine
Собираем (ЛОКАЛЬНО У СЕБЯ) движок документации. В экстремальных ситуациях это делается локально.
Для сборки требуется leiningen (гуглится)
Сборка `prod` версии делается из ветки `master`, сборка staging версии - из ветки `staging`

Собираем:
```
lein uberjar
```

Должен отдать что-то вроде:
```
Created /Users/alex/Work/anychart/docs-engine/target/uberjar/wiki-1.1.jar
Created /Users/alex/Work/anychart/docs-engine/target/uberjar/wiki-1.1-standalone.jar
```

# Manual deploy
Подключаемся к серверу по sftp (в отдельном окошке) и заливаем получившуюся wiki-1.1-standalone.jar:
```
sftp root@server-ip
sftp> cd /apps/wiki
sftp> put /Users/alex/Work/anychart/docs-engine/target/uberjar/wiki-1.1-standalone.jar
```

# Запускаем движок wiki:
По ssh:
```
supervisorctl
wiki                             FATAL      Exited too quickly (process log may have details)
supervisor> start wiki
wiki: started
supervisor> status
wiki                             RUNNING    pid 14435, uptime 0:00:02
```
И ждем порядка 30 секунд (запуск)

# Запускаем initial update
```
curl http://127.0.0.1:9095/_pls_
```
Должен выдать ok и через некоторое время сообщение в slack что все ок.

# Проверяем что все ок
Если речь о `docs.anychart.com` и новый DNS еще не прописался, то правим в /etc/hosts у себя запись. Если о `docs.anychart.stg` то так же правим запись + правим вот этот файл:
https://docs.google.com/a/anychart.com/document/d/18tpNDXyuTbDk_kHa2kvrvVDWG2VrowH9-H824VD2Sl8/edit и сообщаем всем, что надо обновить запись

# Закрываем хуки паролем
# Разрешаем github без пароля
# [только для stg] Пробрасываем хуки по ip
Правим `/etc/nginx/conf.d/default.conf`
```
server {
    listen       80;
    server_name  localhost;

    location / {
        return 404;
    }

    location =/_pls_ {
        proxy_pass  http://127.0.0.1:9095;
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```
Перезапускаем nginx:
```
service nginx restart
```

Открываем в браузере:
`http://server-ip/_pls_` - должен нормально открыться и сказать ok (не забываем заменить server-ip)

# [только для stg] Обновляем хуки

!server-ip не забываем менять на актуальный ip!

Открываем https://github.com/AnyChart/ACDVF-docs/settings/hooks и правим/добавляем запись:  
payload url: `http://server-ip/_pls_`  
Остальные поля без изменений.

# [только для stg] Отключаем отдачу для всех кроме тех, у кого есть правильный /etc/hosts
# Правим настройки деплоя для travis
