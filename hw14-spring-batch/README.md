# hw14-spring-batch

Утилита пакетной миграции данных из реляционного хранилища (H2 / JPA) в NoSQL
(MongoDB) на основе **Spring Batch**.

## Доменная модель

Та же, что в ДЗ JPA / MongoDB: `Author`, `Genre`, `Book`, `Comment`.

* источник — реляционные сущности `ru.otus.hw.relational.models` (H2, заполняется
  из `schema.sql` / `data.sql`);
* приёмник — документы `ru.otus.hw.mongo.models` (встроенная MongoDB).

## Как сохраняются связи

Числовой `id` каждой сущности при миграции сохраняется в виде строки
(`String.valueOf(id)`), а ссылки (`@DocumentReference`) book → author / genres и
comment → book переносятся по этим же id. Поэтому job мигрирует данные строго
в порядке зависимостей:

```
cleanMigrationStep  -> authorMigrationStep -> genreMigrationStep
                    -> bookMigrationStep   -> commentMigrationStep
```

Каждый шаг — это `JpaCursorItemReader` (чтение из H2) + `ItemProcessor`
(конвертация сущности в документ) + `MongoItemWriter` (запись в коллекцию).
`JpaCursorItemReader` держит один `EntityManager` открытым на время шага, что
позволяет дочитывать ленивые связи (`book.author`, `book.genres`, `comment.book`)
прямо в процессоре.

## Запуск

Job не запускается автоматически (`spring.batch.job.enabled=false`) — управление
через Spring Shell:

| Команда (ключи)            | Описание                                                        |
|----------------------------|-----------------------------------------------------------------|
| `migrate`, `m`             | запустить миграцию; при падении — **рестарт** того же instance  |
| `migrate-fresh`, `mf`      | запустить миграцию как новый instance (с нуля)                  |
| `migrated`, `stats`        | показать число документов в коллекциях MongoDB                  |

### Рестарт (опциональное задание)

`migrate` запускает job с постоянными (пустыми) параметрами. Если предыдущий
запуск завершился со статусом `FAILED`, повторный вызов `migrate` **возобновит**
job с упавшего шага (механизм рестарта Spring Batch). Если job уже `COMPLETED`,
команда сообщит об этом — для полного повторного прогона используйте
`migrate-fresh`.

## Сборка

```powershell
mvn verify
```

Интеграционный тест `MigrationJobTest` запускает job на встроенных H2 и MongoDB и
проверяет, что все сущности перенесены и связи сохранены.
