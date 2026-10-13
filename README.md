# PetClinic API Tests

Автор: **Андрей Чесно**.

Отдельный проект API-автотестов для Spring PetClinic REST.

## Покрытие

- health check: `GET /actuator/health`, HTTP 200 и `status=UP`;
- полный CRUD для owner: POST -> GET -> PUT -> DELETE -> GET;
- негативное создание owner с проверкой HTTP-кода и тела ошибки валидации.

## Технологии и версии

- Java 17+ (проект проверен на Eclipse Temurin 17.0.16);
- Apache Maven 3.9.11 через Maven Wrapper;
- Spring Boot 3.5.7;
- JUnit 5;
- REST Assured 5.5.6;
- AssertJ;
- Allure 2.32.0.

## Запуск Spring PetClinic REST

Требуется запущенный Docker Desktop.

```powershell
docker compose up -d
```

Проверка готовности приложения:

```powershell
Invoke-RestMethod http://localhost:9966/petclinic/actuator/health
```

Ожидаемый результат: `status=UP`.

- Base URL: `http://localhost:9966/petclinic`
- Swagger UI: `http://localhost:9966/petclinic/swagger-ui.html`

## Запуск тестов

Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

Linux/macOS:

```bash
./mvnw clean test
```

Для запуска с другим адресом приложения передайте `baseUrl`:

```powershell
.\mvnw.cmd clean test "-DbaseUrl=http://localhost:9966/petclinic"
```

## Allure-отчёт

После выполнения тестов:

```powershell
.\mvnw.cmd allure:serve
```

## Остановка приложения

```powershell
docker compose down
```

Проект запускается локально и не требует доступа к закрытым сервисам или секретам.
