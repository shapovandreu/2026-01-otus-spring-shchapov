# hw18-spring-cloude — План выполнения

**Тема:** Spring Cloud. Отказоустойчивость и мониторинг
**Цель:** сделать внешние вызовы приложения устойчивыми к ошибкам.
**Результат:** приложение с изолированными circuit breaker'ом внешними вызовами + мониторинг.

---

## 0. Ключевые решения (зафиксировано)

| Вопрос | Решение | Почему |
|--------|---------|--------|
| Библиотека отказоустойчивости | **Resilience4j** | Hystrix (Netflix) deprecated и **несовместим со Spring Boot 3.5.7** (последняя поддержка — Boot 2.x / Spring Cloud Hoxton). Задание явно разрешает Resilience4j. |
| Что считаем «внешним вызовом» | **Обращения к БД** (репозитории) | Так сказал преподаватель. Не нужно разбивать на микросервисы — circuit breaker оборачивает вызовы к БД. |
| Архитектура | **Один сервис** на базе `hw10-rest` | Чистый REST без Security/ACL — минимум лишней сложности. |
| Мониторинг (опционально) | **Actuator + Prometheus + Grafana** | Современный аналог Turbine Dashboard. Resilience4j отдаёт метрики circuit breaker в Actuator/Prometheus. |

**Стек:** Spring Boot 3.5.7, Java 17, Spring Cloud **2025.0.x** (release train под Boot 3.5.x), Resilience4j (`resilience4j-spring-boot3`), H2, Spring Data JPA.

---

## 1. Создание модуля

1. Скопировать структуру `hw10-rest` в `hw18-spring-cloude` (книги/авторы/жанры/комментарии: models, dto, repositories, services, controllers, templates, `schema.sql`, `data.sql`, `application.yml`).
2. В `pom.xml`:
   - `artifactId` → `hw18-spring-cloude`, `name` → `hw18-spring-cloude`.
   - Сохранить parent `spring-boot-starter-parent` 3.5.7 и блок checkstyle (как в остальных модулях — проверяется в фазе `verify`).

---

## 2. Зависимости (pom.xml)

Добавить к существующим (web, data-jpa, thymeleaf, validation, h2, lombok, test):

- `dependencyManagement` → **Spring Cloud BOM** `spring-cloud-dependencies` версии `2025.0.x` (свойство `<spring-cloud.version>`).
- `io.github.resilience4j:resilience4j-spring-boot3` — аннотации `@CircuitBreaker`, `@Retry`, `@TimeLimiter`, `@Bulkhead` (стиль, близкий к Hystrix Javanica).
- `org.springframework.boot:spring-boot-starter-aop` — нужен для работы аннотаций Resilience4j.
- `org.springframework.boot:spring-boot-starter-actuator` — health + метрики.
- `io.micrometer:micrometer-registry-prometheus` — экспорт метрик в Prometheus.

> Примечание: вместо `resilience4j-spring-boot3` можно взять `spring-cloud-starter-circuitbreaker-resilience4j` (программный `CircuitBreakerFactory`). Выбран аннотационный вариант — нагляднее и ближе по духу к «Hystrix Javanica».

---

## 3. Оборачивание внешних вызовов (БД) в Resilience4j

Применяем аннотации на **слое сервисов** (`*ServiceImpl`), где вызываются репозитории (= обращения к БД).

Для каждого внешнего вызова (например, в `BookServiceImpl`, `AuthorServiceImpl`, `GenreServiceImpl`, `CommentServiceImpl`):

- `@CircuitBreaker(name = "...", fallbackMethod = "...")` — основной механизм изоляции.
- `@Retry(name = "...")` — повтор при кратковременных сбоях (опционально, перед/вместе с CB).
- `@TimeLimiter` + `@Bulkhead` — опционально, для полноты картины (TimeLimiter требует возврата `CompletableFuture`).

**Fallback-методы:** та же сигнатура + параметр `Throwable`/`Exception` в конце. Возвращают «деградированный» ответ:
- для `findAll` → пустой список / закэшированные данные;
- для `findById` → `Optional.empty()` или дефолтный DTO;
- для модифицирующих операций → бросаем понятное доменное исключение (graceful degradation).

Минимально для зачёта достаточно обернуть **операции чтения** (`findAll`, `findById`) хотя бы в одном-двух сервисах, но лучше покрыть все читающие методы.

---

## 4. Конфигурация Resilience4j (application.yml)

Настроить инстансы в `resilience4j.circuitbreaker.instances.*`:
- `sliding-window-type`, `sliding-window-size`,
- `failure-rate-threshold`,
- `wait-duration-in-open-state`,
- `permitted-number-of-calls-in-half-open-state`,
- `register-health-indicator: true` (чтобы CB попал в `/actuator/health`).

Аналогично `resilience4j.retry.instances.*` (`max-attempts`, `wait-duration`).

---

## 5. Демонстрация срабатывания circuit breaker

Чтобы показать отказоустойчивость, нужен способ «уронить» БД-вызов:
- **Вариант A (проще):** добавить тестовый эндпоинт/флаг, который заставляет репозиторный вызов кидать исключение (эмуляция недоступности БД), и показать переход CB в OPEN + срабатывание fallback.
- **Вариант B:** реально останавливать БД — для H2 in-memory неудобно; если время есть, можно вынести БД (PostgreSQL) в Docker и гасить контейнер.

Рекомендую **Вариант A** + описать сценарий в README: серия ошибок → CB OPEN → запросы идут в fallback, не нагружая БД → через `wait-duration` → HALF_OPEN → CLOSED.

---

## 6. Мониторинг (Actuator + Prometheus + Grafana)

1. **Actuator:** в `application.yml` открыть эндпоинты `health`, `metrics`, `prometheus`, `circuitbreakers`, `circuitbreakerevents`. Включить `management.health.circuitbreakers.enabled: true`.
2. **Prometheus:** `prometheus.yml` со scrape-конфигом на `/actuator/prometheus` приложения.
3. **Grafana:** дашборд по метрикам `resilience4j_circuitbreaker_*` (состояние, доля ошибок, число вызовов).
4. **docker-compose.yml** в `hw18-spring-cloude/`: сервисы `app`, `prometheus`, `grafana` (можно переиспользовать подход из hw17-docker).

Проверка: метрики `resilience4j_circuitbreaker_state`, `resilience4j_circuitbreaker_calls` видны в Prometheus и на дашборде Grafana.

---

## 7. Тесты

- Юнит-тест сервиса: при «падающем» репозитории вызывается **fallback** (мокаем репозиторий через `when(...).thenThrow(...)`, проверяем деградированный ответ).
- (Опц.) Интеграционный тест перехода CB в OPEN после N ошибок.
- Прогнать `mvn verify` (включает checkstyle — OtusTeam config).

---

## 8. Сборка и запуск

Maven не на PATH — использовать бандл IntelliJ и JDK 17 (см. memory build-setup):

```powershell
$env:JAVA_HOME = "C:\Users\Andreu\.jdks\axiomjdk-17.0.17"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd" verify
```

Запуск стека мониторинга: `docker compose up` из `hw18-spring-cloude/`.

---

## 9. README модуля

Описать: что обёрнуто в circuit breaker, конфигурацию Resilience4j, как воспроизвести срабатывание CB, ссылки на эндпоинты Actuator, как поднять Prometheus + Grafana, скриншот/описание дашборда.

---

## 10. Чек-лист соответствия заданию

- [ ] Внешние вызовы (обращения к БД) обёрнуты в circuit breaker — **Resilience4j** (вместо несовместимого Hystrix).
- [ ] Использован Resilience4j (разрешено заданием).
- [ ] Fallback-методы для деградации при сбоях.
- [ ] (Опц.) Мониторинг: Actuator + Prometheus + Grafana вместо Turbine Dashboard.
- [ ] Тест на срабатывание fallback.
- [ ] `mvn verify` зелёный (checkstyle проходит).

---

## Примечания / риски

- **Hystrix не используем намеренно** — он не запускается на Spring Boot 3.x. Это стоит явно отметить в README, чтобы не выглядело как отступление от задания (само задание разрешает Resilience4j).
- **Feign не нужен** — раз «внешние вызовы» = БД, межсервисного HTTP-вызова нет. Feign имеет смысл только при разбиении на микросервисы (не выбрано).
- TimeLimiter/Bulkhead — опциональны; добавлять, если хочется полнее раскрыть тему, но это усложняет сигнатуры (нужен `CompletableFuture`).
