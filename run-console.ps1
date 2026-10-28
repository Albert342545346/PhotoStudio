# КР 1: запуск консольного приложения (PostgreSQL должен быть запущен).
# Если кириллица в консоли отображается неверно: chcp 65001
Set-Location $PSScriptRoot
mvn -q compile exec:java
