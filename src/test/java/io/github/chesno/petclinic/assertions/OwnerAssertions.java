package io.github.chesno.petclinic.assertions;

import io.github.chesno.petclinic.client.ApiResponse;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.github.chesno.petclinic.model.OwnerResponse;
import io.github.chesno.petclinic.model.ProblemDetailResponse;
import io.qameta.allure.Allure;
import static io.github.chesno.petclinic.assertions.ApiAssertions.json;
import static io.github.chesno.petclinic.assertions.ApiAssertions.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Проверки данных Owner, созданных текущим тестом с пустым полем pets. */
public final class OwnerAssertions {
    private OwnerAssertions() { }

    public static OwnerResponse owner(ApiResponse response, int expectedStatus, OwnerRequest expected, Integer expectedId) {
        return Allure.step("Проверить HTTP " + expectedStatus + ", owner ID, все переданные поля и поле pets", () -> {
            // Проверяем контракт до десериализации: ответ с ошибкой нельзя интерпретировать как Owner.
            status(response, expectedStatus);
            json(response);
            OwnerResponse actual = response.bodyAs(OwnerResponse.class);
            matches(actual, expected, expectedId);
            return actual;
        });
    }

    public static void matches(OwnerResponse actual, OwnerRequest expected, Integer expectedId) {
        assertThat(actual).as("Тело ответа Owner").isNotNull();
        assertAll("Owner ID, все изменяемые поля и поле pets",
                () -> assertThat(actual.id()).as("Присвоенный owner ID").isNotNull().isNotNegative(),
                () -> { if (expectedId != null) assertThat(actual.id()).as("ID запрошенного owner").isEqualTo(expectedId); },
                () -> assertThat(actual.fields()).as("Соответствие данных Owner отправленному запросу")
                        .usingRecursiveComparison().isEqualTo(expected),
                () -> assertThat(actual.pets()).as("Пустое поле pets у созданного тестом owner").isNotNull().isEmpty());
    }

    public static void validationErrorIdentifies(ProblemDetailResponse error, OwnerRequest.Field field) {
        assertThat(error.schemaValidationErrors()).as("Список ошибок валидации").isNotEmpty();
        assertThat(error.schemaValidationErrors()).allSatisfy(message ->
                assertThat(message.message()).as("Сообщение об ошибке валидации").isNotBlank());
        // OpenAPI требует message, но не необязательное поле field или точную локализованную формулировку.
        assertThat(error.schemaValidationErrors()).as("Указание невалидного поля %s в описании ошибки", field.jsonName())
                .anySatisfy(message -> assertThat(message.message())
                        .as("Имя поля в сообщении об ошибке").contains(field.jsonName()));
    }
}
