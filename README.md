# petclinic-api-tests-by-chesno

Автор: **Андрей Чесно**.

Отдельный проект API-автотестов Spring PetClinic REST. Исходники приложения не включены и не изменяются.
Тесты проверяют зафиксированный OpenAPI-контракт, сохранённые данные и отсутствие удалённых записей.

## Стек

Java **17+** (проверка на Temurin 17.0.16), Maven **3.9.11** через Wrapper,
Spring Boot **3.5.7**, JUnit **5** (версия управляется Spring Boot), REST Assured **5.5.6**,
AssertJ, Jackson, Allure Java **2.32.0**. Docker Compose v2 — для отдельного тестового окружения.

Spring Boot используется по назначению: поднимает контекст тестовой инфраструктуры, связывает настройки и API-клиент.
Веб-сервер тестового проекта не запускается. Версия Spring Boot тестов не обязана совпадать с сервером, взаимодействие идёт по HTTP.

## Быстрый запуск на Windows / PowerShell

1. Установите JDK 17 или новее и Git. Для контейнера — Docker Desktop с Linux containers.
2. Склонируйте опубликованный репозиторий:

   ```powershell
   git clone <URL-репозитория>
   cd petclinic-api-tests-by-chesno
   ```

   Если проект уже создан локально:

   ```powershell
   cd C:\Work\petclinic-api-tests-by-chesno
   ```

3. Проверьте Java и Maven:

   ```powershell
   java -version
   .\mvnw.cmd -version
   ```

   Если Wrapper сообщает об отсутствии JAVA_HOME, задайте путь к **своему** JDK без `\bin`:

   ```powershell
   $env:JAVA_HOME = 'C:\путь\к\jdk-17'
   .\mvnw.cmd -version
   ```

   Это настройка текущего терминала. Maven отдельно устанавливать не нужно: Wrapper скачает зафиксированную версию.

4. Если PetClinic ещё не запущен, запустите Docker Desktop, дождитесь готовности движка и выполните:

   ```powershell
   docker compose up -d
   .\scripts\wait-for-petclinic.ps1
   ```

   Приложение: http://localhost:9966/petclinic

   Swagger: http://localhost:9966/petclinic/swagger-ui.html

   При первом запуске потребуется интернет для Docker-образа и Maven-зависимостей. Закрытые сервисы и секреты не нужны.

5. Запустите все тесты:

   ```powershell
   .\mvnw.cmd clean test
   ```

   При установленном Maven работают требуемые заданием команды:

   ```text
   mvn clean test
   mvn clean test -DbaseUrl=http://localhost:9966/petclinic
   ```

   Эквивалент с Wrapper в PowerShell:

   ```powershell
   .\mvnw.cmd clean test "-DbaseUrl=http://localhost:9966/petclinic"
   ```

6. Создайте и откройте Allure-отчёт, даже если тесты нашли дефекты:

   ```powershell
   .\mvnw.cmd allure:report
   .\mvnw.cmd allure:serve
   ```

   Вторая команда запускает локальный просмотр отчёта до Ctrl+C.
   XML/text отчёты: `target/surefire-reports`; вложения запросов/ответов: `target/allure-results`.

7. Если окружение запущено через Compose этого проекта:

   ```powershell
   docker compose down
   ```

   Будет удалён контейнер **этого Compose-проекта** вместе с его временными данными.
   Это не требуется для уже работающего приложения, запущенного отдельно в IDEA.

## Если порт 9966 уже занят

Не останавливайте чужой процесс. Используйте текущий локальный PetClinic либо отдельный порт:

```powershell
$env:PETCLINIC_PORT = '19966'
docker compose up -d
.\scripts\wait-for-petclinic.ps1 -BaseUrl http://localhost:19966/petclinic
.\mvnw.cmd clean test "-DbaseUrl=http://localhost:19966/petclinic"
```

Linux/macOS: `bash mvnw clean test`; параметры Maven те же.

## Запуски из IDEA и отдельные наборы

Откройте `pom.xml` как Maven project, выберите JDK 17+, Maven Wrapper и дождитесь импорта.
Запускайте классы из `src/test/java/.../tests` либо все тесты через Maven.
Для другого URL в JUnit run configuration укажите VM option `-DbaseUrl=...`.

```powershell
# Только самопроверки инфраструктуры, без работающего PetClinic
.\mvnw.cmd clean test -Pframework

# Только API-тесты
.\mvnw.cmd test "-Dgroups=api"

# Только health check
.\mvnw.cmd test "-Dtest=HealthApiTest"
```

`-Pframework` не является заменой полному прогону задания. Все API-проверки включены по умолчанию.

## Архитектура и надёжность

Весь код относится к тестовому проекту и находится в `src/test/java/io/github/chesno/petclinic`:

- `config` — Spring Boot context, настройки URL/таймаутов и единый Jackson mapper;
- `client` — HTTP-операции, метаданные ответа, типизированная десериализация и Allure request/response attachments;
- `model` — неизменяемые модели запросов, ответов owner/health/ProblemDetail;
- `data` — уникальные синтетические данные в рамках ограничений OpenAPI;
- `assertions` — проверка кодов/Content-Type и сравнение моделей;
- `support` — отдельный журнал принадлежащих тесту данных и проверяемая очистка;
- `tests` — health, CRUD и 18 параметризованных негативных случаев;
- `framework` — тесты самой инфраструктуры, без сети и без PetClinic.

Нет JSONPath-ассертов, фиксированных ID, зависимостей между тестами, повторных запусков для скрытия падений,
статических изменяемых настроек RestAssured, обращений к БД/приватным сервисам или секретов.
Запросы и ответы прикладываются к Allure; шаги оформлены через `Allure.step`, поэтому AspectJ/javaagent не требуется.

POST и GET сравниваются со всеми пятью отправленными полями. После PUT отдельный GET проверяет сохранение,
после DELETE отдельный GET доказывает отсутствие записи. HTTP-статус проверяется до десериализации.
У негативных ответов точный Content-Type и содержимое DTO проверяются независимо:
неверный media type остаётся дефектом, но не скрывает проверку тела ошибки.
DTO не заменяют проверки: обязательные поля проверяются явно, `Integer` не подменяет отсутствующий ID нулём,
преобразование строки в число Jackson запрещено.

### Очистка

Попытка POST регистрируется **до запроса**, ID — сразу после ответа, до бизнес-ассертов.
При проблеме с телом используется Location; без ID выполняется поиск по уникальным маркерам теста.
Это покрывает и неожиданный успешный ответ в негативном тесте, и таймаут после сохранения на сервере.
Перед удалением сверяется принадлежность записи тесту. Изменённые при PUT маркеры тоже учитываются.

`@AfterEach` выполняет очистку при обычном падении теста; ошибка одной записи не останавливает остальные удаления.
После DELETE проверяется GET 404. Ошибки очистки сохраняются в отчёте вместе с первоначальным падением.
При недоступном сервере, аварийном завершении JVM или потере всех идентификаторов абсолютной гарантии нет:
тест сообщает проблему, а одноразовый контейнер позволяет удалить всё окружение без воздействия на чужую БД.

Тесты изменяют данные, поэтому по умолчанию разрешены только loopback URL.
Для специально выделенного удалённого тестового стенда требуется осознанный `-DallowRemote=true`.
Не направляйте эти тесты в production.

## Контракт, найденные дефекты и результаты

См. [матрицу требований и источник OpenAPI](docs/contract/README.md).
В контракте PUT/DELETE требуют 200, исходники предусматривают 204; тесты не адаптируются к этому молча.
Ошибки соответствия контракту должны давать красную сборку. Известные расхождения не пропускаются.

Результаты фактической проверки проекта записаны в [verification.md](docs/verification.md):
35 проверок, 16 passed и 19 failed по описанным расхождениям контракта. Все 15 самопроверок инфраструктуры прошли;
типизированное тело ошибки успешно проверено во всех 18 негативных случаях.
Это не полностью зелёная сборка: до сдачи следует согласовать найденные дефекты с автором задания.

## CI и публикация

GitHub Actions поднимает отдельный контейнер, запускает **все** тесты, сохраняет Surefire/Allure/логи даже при падении
и останавливает свой контейнер. CI не маскирует известные дефекты контракта.

Репозиторий создан локально. GitHub/GitLab-проект нужно создать в своём аккаунте; URL намеренно не выдуман.
После проверки файлов и настройки своей Git identity:

```text
git add .
git commit -m "Add PetClinic API test project"
git remote add origin <URL-пустого-репозитория>
git push -u origin main
```

Не коммитьте `target`, локальные токены, `.env` и настройки IDE.

## Документация используемых инструментов

- Spring Boot testing: https://docs.spring.io/spring-boot/3.5/reference/testing/spring-boot-applications.html
- Allure REST Assured: https://allurereport.org/docs/restassured/
- Allure JUnit: https://allurereport.org/docs/junit5/
- Maven Wrapper: https://maven.apache.org/tools/wrapper/index.html
