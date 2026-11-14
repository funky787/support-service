# Support Service

REST API для учёта обращений в техническую поддержку. Java + Spring Boot, Kotlin, PostgreSQL, Gradle, Flyway.

## Что умеет

Создание и редактирование заявок, категории, приоритеты, допустимые переходы статусов, история изменений, фильтрация и пагинация. Kotlin-сервис рассчитывает статистику за период.

## Архитектура

Проект разделён на слои по назначению классов:

```text
src/main/
├── java/com/example/support/
│   ├── controller/   # HTTP-запросы и ответы
│   ├── service/      # Бизнес-логика работы с заявками (Java)
│   ├── repository/   # Доступ к PostgreSQL через Spring Data JPA
│   ├── model/        # JPA-сущности и перечисления
│   ├── dto/          # Модели запросов и ответов REST API
│   ├── exception/    # Единая обработка ошибок API
│   ├── config/       # Настройки приложения (Clock)
│   └── SupportApplication.java
└── kotlin/com/example/support/
    ├── service/ReportService.kt   # Подсчёт статистики на Kotlin
    └── dto/SummaryResponse.kt     # Модель ответа отчёта
```

Поток запроса: `Controller → Service → Repository → PostgreSQL`. HTTP-модели (`dto`) не используются как JPA-сущности. Java-контроллер отчёта вызывает Kotlin-сервис; тот использует Java-репозиторий и сущности. `SupportApplication` находится в корневом пакете, поэтому Spring видит все вложенные слои.

## Требования

- JDK 21; Gradle 8.10.2 для первого запуска (до генерации Wrapper)
- Docker Desktop / Docker Engine + Docker Compose для запуска с PostgreSQL и интеграционных тестов

> В исходном архиве нет бинарного `gradle-wrapper.jar`. Выполните `gradle wrapper --gradle-version 8.10.2` (или `docker run --rm -v ${PWD}:/project -w /project gradle:8.10.2-jdk21 gradle wrapper` в PowerShell), затем добавьте созданные `gradlew`, `gradlew.bat` и `gradle/wrapper/gradle-wrapper.jar` в Git. После этого используйте `./gradlew` (Windows: `.\gradlew.bat`). Сам проект собирается Dockerfile без локального Gradle.

## Быстрый запуск

```bash
cp .env.example .env
# при необходимости поменять тестовый пароль в .env
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

Windows PowerShell: `Copy-Item .env.example .env`, затем `docker compose up --build -d`.

Swagger UI: http://localhost:8080/swagger-ui/index.html

Health: http://localhost:8080/actuator/health/readiness

Создать заявку (PowerShell):

```powershell
$body = '{"title":"Не работает принтер","description":"Кабинет 12","categoryId":2,"priority":"HIGH","dueAt":"2027-01-01T12:00:00Z"}'
Invoke-RestMethod http://localhost:8080/api/tickets -Method Post -ContentType application/json -Body $body
Invoke-RestMethod http://localhost:8080/api/tickets/1
```

Изменение статуса: `PATCH /api/tickets/1/status` с JSON `{"status":"IN_PROGRESS"}`. Следующий допустимый статус — `RESOLVED`. Также разрешено `CANCELLED` из `NEW` и `IN_PROGRESS`.

Статистика: `GET /api/reports/summary?from=2026-01-01T00:00:00Z&to=2027-01-01T00:00:00Z`.

## API

| Метод | URL | Назначение |
|---|---|---|
| GET | `/api/categories` | Категории |
| POST | `/api/tickets` | Создание |
| GET | `/api/tickets/{id}` | Получение |
| PUT | `/api/tickets/{id}` | Редактирование описания, приоритета, срока |
| PATCH | `/api/tickets/{id}/status` | Смена статуса |
| GET | `/api/tickets/{id}/history` | История статусов |
| GET | `/api/tickets?status=NEW&priority=HIGH&page=0&size=20` | Поиск и пагинация (также categoryId, overdue) |
| GET | `/api/reports/summary?from=...&to=...` | Статистика Kotlin |
| GET | `/actuator/health/readiness` | Готовность приложения |

Ошибки: 400 — неверные данные, 404 — заявка не найдена, 409 — запрещённая операция. У статусов конечные состояния `RESOLVED`, `CANCELLED`.

## Тесты и покрытие

```bash
gradle check jacocoTestReport
```

Интеграционные тесты используют PostgreSQL через Testcontainers, поэтому Docker должен работать. Отчёты: `build/reports/tests/test/index.html` и `build/reports/jacoco/test/html/index.html`.

## Данные, резервное копирование, восстановление

Данные БД хранятся в named volume `support_pgdata` и остаются после `docker compose down` (без `-v`). Проверка: создать заявку, выполнить `docker compose down`, затем `docker compose up -d` и прочитать её по ID.

Linux/Git Bash:

```bash
bash scripts/backup.sh
bash scripts/restore-test.sh backups/<имя-файла>.sql
```

PowerShell:

```powershell
New-Item -ItemType Directory -Force backups | Out-Null
docker compose exec -T db pg_dump -U support -d support_db --no-owner --no-acl | Out-File -Encoding utf8 backups\support.sql
```

Для надёжного бинарного переноса используйте `pg_dump -Fc` в Linux/Git Bash. Восстановление в отдельную базу выполняет скрипт `restore-test.sh`. ВНИМАНИЕ: тестовая база `support_restore` перед восстановлением удаляется и создаётся заново; основная `support_db` не затрагивается.

## Диагностика

- PostgreSQL недоступен: `docker compose ps`, `docker compose logs db`, `docker compose exec db pg_isready -U support -d support_db`; проверить DB_URL и пароль.
- Health check неуспешен: `docker compose logs app`, `curl http://localhost:8080/actuator/health`, проверить доступность БД и порт 8080.
- Ошибка миграции: смотреть `docker compose logs app`, проверить версию и checksum миграций в `flyway_schema_history`. Не изменять уже применённые миграции — добавлять `V2__...sql`.

## CI/CD и откат

Workflow `.github/workflows/ci.yml`: PR — проверка и тесты; push в `main` — проверка, сборка и публикация образа в GHCR с тегом SHA коммита. Развёртывание на учебном сервере включается установкой repository variable `DEPLOY_ENABLED=true` и GitHub Secrets `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `GHCR_PULL_TOKEN`. Сервер должен иметь Docker Compose, доступ к GHCR, папку `/opt/support-service` и защищённый `/opt/support-service/.env` с `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

Откат на сервере (из `/opt/support-service`):

```bash
export REGISTRY_IMAGE=ghcr.io/owner/support-service
export PREVIOUS_TAG=<SHA-previous-good-commit>
bash scripts/rollback.sh
```

Внешний учебный сервер и его секреты не включены в репозиторий; фактический успешный деплой и откат необходимо провести на доступном стенде. Для возврата к предыдущей версии миграции должны оставаться обратно совместимыми.

## Документы

`docs/specification.md` — требования и истории; `docs/er.mmd` и `docs/process.mmd` — диаграммы; `docs/test-plan.md` — тест-план; `docs/defects.md` — шаблон фиксации дефекта; `docs/project-report.md` — итоговый отчёт и ограничения; `docs/http-requests.http` — HTTP-запросы для IntelliJ.
