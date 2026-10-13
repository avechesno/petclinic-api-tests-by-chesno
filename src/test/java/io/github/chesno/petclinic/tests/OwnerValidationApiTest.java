package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.data.OwnerData;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static io.github.chesno.petclinic.assertions.ApiAssertions.*;
import static io.github.chesno.petclinic.assertions.OwnerAssertions.validationErrorIdentifies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Epic("API приложения PetClinic") @Feature("Owner API: валидация данных") @Owner("Андрей Чесно")
class OwnerValidationApiTest extends BaseApiTest {
    @DisplayName("Owner: создание с невалидными данными возвращает HTTP 400 и не создаёт запись")
    @ParameterizedTest(name = "[{index}] {displayName}; {0}")
    @MethodSource("invalidOwners")
    void rejectsInvalidOwner(String scenario, OwnerRequest.Field field, String invalidValue) {
        OwnerRequest request = OwnerData.uniqueOwner().with(field, invalidValue);
        var response = steps.createOwner(request);
        assertAll("Ошибка валидации и отсутствие созданного owner",
                () -> Allure.step("Проверить HTTP 400 и Content-Type application/problem+json", () -> {
                    status(response, 400);
                    problemJson(response);
                }),
                () -> Allure.step("Проверить типизированное тело ошибки и указание невалидного поля", () ->
                        validationErrorIdentifies(problemBody(response, 400), field)),
                () -> Allure.step("Убедиться, что owner с невалидными данными не сохранён", () ->
                        assertThat(steps.findCreatedOwners(request)).as("Отсутствие owner после отклонённого запроса").isEmpty()));
    }

    static Stream<Arguments> invalidOwners() {
        Stream<Arguments> required = Arrays.stream(OwnerRequest.Field.values()).flatMap(field -> Stream.of(
                Arguments.of("поле " + field.jsonName() + " — пустая строка", field, ""),
                Arguments.of("поле " + field.jsonName() + " имеет значение null", field, null)));
        Stream<Arguments> constraints = Stream.of(
                Arguments.of("длина firstName превышает 30 символов", OwnerRequest.Field.FIRST_NAME, "A".repeat(31)),
                Arguments.of("длина lastName превышает 30 символов", OwnerRequest.Field.LAST_NAME, "A".repeat(31)),
                Arguments.of("длина address превышает 255 символов", OwnerRequest.Field.ADDRESS, "A".repeat(256)),
                Arguments.of("длина city превышает 80 символов", OwnerRequest.Field.CITY, "A".repeat(81)),
                Arguments.of("длина telephone превышает 20 символов", OwnerRequest.Field.TELEPHONE, "1".repeat(21)),
                Arguments.of("поле telephone содержит буквы", OwnerRequest.Field.TELEPHONE, "123ABC"),
                Arguments.of("поле firstName содержит цифры", OwnerRequest.Field.FIRST_NAME, "Andrey123"),
                Arguments.of("поле lastName содержит цифры", OwnerRequest.Field.LAST_NAME, "Chesno123"));
        return Stream.concat(required, constraints);
    }
}
