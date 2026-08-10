# hw18-spring-cloude — Spring Cloud. Отказоустойчивость и мониторинг

Приложение библиотеки (книги / авторы / жанры / комментарии) на базе `hw10-rest`, в котором
**внешние вызовы изолированы circuit breaker'ом**. Согласно уточнению преподавателя, внешними
вызовами считаются **обращения к БД** (вызовы репозиториев из сервисного слоя).

## Почему Resilience4j, а не Hystrix

Netflix Hystrix (и Hystrix Javanica) **deprecated** и совместим только со Spring Boot 2.x /
Spring Cloud Hoxton. Весь репозиторий построен на **Spring Boot 3.5.7 / Java 17**, где Hystrix
не запускается. Поэтому, как и разрешено условием задания, используется **Resilience4j** —
официально поддерживаемая в Spring Cloud замена Hystrix. Аннотационный стиль
(`@CircuitBreaker`, `@Retry`, fallback-методы) идейно повторяет подход Hystrix Javanica.

## Что обёрнуто в circuit breaker

Аннотации `@CircuitBreaker` + `@Retry` с fallback-методами навешаны на читающие методы сервисов
(каждый из них делает обращение к БД):

| Сервис | Метод | Fallback |
|--------|-------|----------|
| `AuthorServiceImpl`  | `findAll`                 | пустой список |
| `GenreServiceImpl`   | `findAll`                 | пустой список |
| `BookServiceImpl`    | `findAll`, `findById`     | пустой список / `Optional.empty()` |
| `CommentServiceImpl` | `findByBookId`, `findById`| пустой список / `Optional.empty()` |

При недоступности БД вызов не пробрасывает исключение наружу, а возвращает «деградированный»
ответ (graceful degradation), а circuit breaker после серии ошибок переходит в `OPEN` и
перестаёт нагружать БД.

Бизнес-исключение `EntityNotFoundException` помечено в `ignore-exceptions` — оно не считается
сбоем БД и не учитывается при подсчёте ошибок circuit breaker'а.

## Конфигурация (application.yml)

- `resilience4j.circuitbreaker` — COUNT_BASED окно на 10 вызовов, порог ошибок 50%, минимум 5
  вызовов, `wait-duration-in-open-state = 10s`, авто-переход в `HALF_OPEN`.
- `resilience4j.retry` — до 3 попыток с паузой 200мс.
- Инстансы: `authorService`, `genreService`, `bookService`, `commentService`.

## Эмуляция сбоя БД (для демонстрации)

Чтобы воспроизвести отказ внешнего вызова, есть переключатель `FaultInjector`, управляемый через REST:

```bash
# включить сбой — вызовы к БД начнут падать
curl -X POST http://localhost:8080/api/fault/enable
# выключить сбой — БД снова доступна
curl -X POST http://localhost:8080/api/fault/disable
```

Сценарий демонстрации:
1. `GET /api/books` — возвращаются книги (БД доступна).
2. `POST /api/fault/enable` — включаем сбой.
3. Несколько раз `GET /api/books` — ответ деградирует (пустой список), пишутся WARN-логи fallback'а.
4. После превышения порога ошибок circuit breaker переходит в `OPEN`
   (см. `GET /actuator/circuitbreakers`).
5. `POST /api/fault/disable` — через `wait-duration-in-open-state` CB переходит в `HALF_OPEN`,
   затем в `CLOSED`, данные снова отдаются из БД.

## Мониторинг (Actuator + Prometheus + Grafana)

Современный аналог Turbine Dashboard.

Actuator-эндпоинты:
- `GET /actuator/health` — в т.ч. состояние circuit breaker'ов;
- `GET /actuator/circuitbreakers`, `GET /actuator/circuitbreakerevents`;
- `GET /actuator/metrics`, `GET /actuator/prometheus` — метрики `resilience4j_circuitbreaker_*`.

Поднять стек мониторинга:

```bash
# 1) собрать jar (Maven не на PATH — см. раздел "Сборка")
mvn -DskipTests package
# 2) поднять app + prometheus + grafana
docker compose up --build
```

- Приложение: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (anonymous включён; admin/admin). Дашборд
  **«Resilience4j Circuit Breakers»** провижинится автоматически.

## Сборка

Maven не на PATH — используется бандл IntelliJ и JDK 17:

```powershell
$env:JAVA_HOME = "C:\Users\Andreu\.jdks\axiomjdk-17.0.17"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.6.1\plugins\maven\lib\maven3\bin\mvn.cmd" verify
```

`verify` включает checkstyle (конфиг OtusTeam).

## Тесты

`CircuitBreakerFallbackTest` проверяет:
- при недоступности БД возвращается fallback (пустой список) и circuit breaker переходит в `OPEN`;
- после «восстановления» БД circuit breaker возвращается в `CLOSED` и отдаёт реальные данные.
