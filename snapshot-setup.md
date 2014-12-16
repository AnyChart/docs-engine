# Restore docs from snapshot

## production on digitalocean
* Идем на digitalocean.com
* Идем в создание новой виртуалки
  * Hostname: docs.anychart.com
  * Size: $10, 1gb ram
  * Data Center - если NY3 лежит - выбираем San Francisco или Amsterdam 3
  * Enable backups - да
  * Image: выбираем *My snapshots* и там docs
  * Add ssh keys: все доступные
  * Создаем виртуалку
* Прописываем dns на 1and1
* Обновляем контент (как: https://docs.google.com/a/anychart.com/document/d/1mBspp-wLP9rBUyOrbQVmGQImtehBP2mR_1Wz3tFODw0/edit)
