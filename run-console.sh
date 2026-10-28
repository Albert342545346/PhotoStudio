#!/usr/bin/env sh
# КР 1: запуск консольного приложения (PostgreSQL должен быть запущен).
cd "$(dirname "$0")" && mvn -q compile exec:java
