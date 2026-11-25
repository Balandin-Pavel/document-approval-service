# Document Approval Service

Учебный Java backend-проект: документы проходят состояния
`DRAFT -> SUBMITTED -> APPROVED / REJECTED`.
Автор создаёт и отправляет документ; согласующий принимает решение.
Каждый переход записывается в историю событий в той же транзакции.

## Стек

Java 17, Spring Boot 4.1.3, Spring MVC, Spring Data JPA, Spring Security,
PostgreSQL, Flyway, Maven, JUnit 5, MockMvc. Код разделён на HTTP API,
предметную логику, хранение и настройки доступа.

## Запуск

Нужны JDK 17+, Docker и Maven 3.6.3+ (или включённый Maven Wrapper).

```bash
docker compose up -d --wait db
./mvnw test
./mvnw spring-boot:run
```

Сервер по умолчанию слушает только `127.0.0.1:8080`.
База доступна только на `127.0.0.1:5433`.

Демонстрационные учётные записи **только для локальной разработки**:

| Роль | Пользователь | Пароль по умолчанию |
|---|---|---|
| Автор | `alice` | `author_local` |
| Согласующий | `bob` | `approver_local` |

Пароли задаются переменными `AUTHOR_PASSWORD`, `APPROVER_PASSWORD`,
`DB_PASSWORD`; не добавляйте реальные значения в Git. В этом pet-проекте
учётные записи хранятся в памяти. Для внешнего развёртывания нужны
полноценная система идентификации и HTTPS.

## Быстрый сценарий

```bash
curl -u alice:author_local -H 'Content-Type: application/json' \
  -d '{"title":"Договор","body":"Текст на согласование"}' \
  http://127.0.0.1:8080/api/documents
```

Из ответа возьмите `id` документа:

```bash
curl -u alice:author_local -X POST http://127.0.0.1:8080/api/documents/<id>/submit
curl -u bob:approver_local -X POST -H 'Content-Type: application/json' \
  -d '{"comment":"Согласовано"}' \
  http://127.0.0.1:8080/api/documents/<id>/approve
curl -u alice:author_local http://127.0.0.1:8080/api/documents/<id>/events
```

## API

| Метод | Путь | Доступ |
|---|---|---|
| POST | `/api/documents` | Автор |
| GET | `/api/documents`, `/api/documents/{id}` | Автор видит свои; согласующий — отправленные |
| POST | `/api/documents/{id}/submit` | Автор документа |
| POST | `/api/documents/{id}/approve` | Согласующий |
| POST | `/api/documents/{id}/reject` | Согласующий; комментарий обязателен |
| GET | `/api/documents/{id}/events` | Тот, кому виден документ |
| GET | `/actuator/health` | Без авторизации |

Список документов поддерживает `page` и `size` (1–100). Ошибки состояния
и конкурентного обновления дают HTTP 409. В таблице документов есть
оптимистическая блокировка `@Version`. Схему БД создаёт Flyway; Hibernate
её проверяет при запуске.

## Проверка

```bash
./mvnw test
```

Интеграционные тесты поднимают контекст приложения, применяют миграцию в H2
в режиме PostgreSQL и проверяют API через MockMvc вместе с Spring Security.
Отдельный локальный запуск с настоящим PostgreSQL описан выше.
