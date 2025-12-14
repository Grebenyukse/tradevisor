#!/bin/bash

echo "🧹 Очистка Docker окружения..."

# Остановка всех контейнеров
echo "⏹️  Остановка всех контейнеров..."
docker stop $(docker ps -aq) 2>/dev/null || echo "Нет контейнеров для остановки"

# Удаление всех контейнеров
echo "🗑️  Удаление всех контейнеров..."
docker rm $(docker ps -aq) 2>/dev/null || echo "Нет контейнеров для удаления"

# Удаление всех образов (опционально - закомментировано для безопасности)
#echo "🖼️  Удаление всех образов..."
#docker rmi $(docker images -q) 2>/dev/null || echo "Нет образов для удаления"

# Удаление всех volumes
echo "💾 Удаление всех volumes..."
docker volume prune -f

# Удаление всех networks
echo "🌐 Удаление всех networks..."
docker network prune -f

# Удаление dangling образов
echo "🧹 Удаление dangling образов..."
docker image prune -f

# Удаление build cache
echo "⚡ Удаление build cache..."
docker builder prune -f

# Показ статуса
echo "✅ Docker окружение очищено!"
docker ps -a
docker volume ls
docker network ls
