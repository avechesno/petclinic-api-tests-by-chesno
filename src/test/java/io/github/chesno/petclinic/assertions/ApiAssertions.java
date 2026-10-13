package io.github.chesno.petclinic.assertions;

import io.github.chesno.petclinic.client.ApiResponse;
import io.github.chesno.petclinic.model.ProblemDetailResponse;
import io.qameta.allure.Allure;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class ApiAssertions {
    private ApiAssertions() { }

    public static void status(ApiResponse response, int expected) {
        assertThat(response.status()).as("Ожидаемый HTTP-код ответа").isEqualTo(expected);
    }

    public static void json(ApiResponse response) {
        assertThat(response.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                .as("Тип содержимого JSON-ответа").isEqualTo("application/json");
    }

    public static void problemJson(ApiResponse response) {
        assertThat(response.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                .as("Тип содержимого ответа ProblemDetail").isEqualTo("application/problem+json");
    }

    public static void emptyBody(ApiResponse response, int expectedStatus) {
        Allure.step("Проверить HTTP " + expectedStatus + " и отсутствие тела ответа", () -> {
            status(response, expectedStatus);
            assertThat(response.body()).as("Пустое тело ответа HTTP " + expectedStatus).isBlank();
        });
    }

    public static ProblemDetailResponse problem(ApiResponse response, int expectedStatus) {
        return Allure.step("Проверить HTTP " + expectedStatus + " и типизированное описание ошибки ProblemDetail", () -> {
            status(response, expectedStatus);
            problemJson(response);
            return problemBody(response, expectedStatus);
        });
    }

    public static ProblemDetailResponse problemBody(ApiResponse response, int expectedStatus) {
        status(response, expectedStatus);
        // Тело проверяется независимо; соответствие Content-Type проверяется отдельно.
        // Неверный тип содержимого не должен скрывать отсутствующие или некорректные поля описания ошибки.
        ProblemDetailResponse actual = response.bodyAs(ProblemDetailResponse.class);
        assertThat(actual).as("Тело ответа с описанием ошибки ProblemDetail").isNotNull();
        assertAll("Обязательные поля описания ошибки ProblemDetail",
                () -> assertThat(actual.status()).as("Код ошибки в поле status").isEqualTo(expectedStatus),
                () -> {
                    assertThat(actual.type()).as("URI типа ошибки в поле type").isNotNull();
                    assertThat(actual.type().isAbsolute()).as("Абсолютный URI в поле type").isTrue();
                },
                () -> assertThat(actual.title()).as("Заголовок ошибки в поле title").isNotBlank(),
                () -> assertThat(actual.detail()).as("Описание ошибки в поле detail").isNotBlank(),
                () -> assertThat(actual.timestamp()).as("Время ошибки в поле timestamp").isNotNull(),
                () -> assertThat(actual.schemaValidationErrors()).as("Список ошибок в поле schemaValidationErrors").isNotNull());
        return actual;
    }
}
