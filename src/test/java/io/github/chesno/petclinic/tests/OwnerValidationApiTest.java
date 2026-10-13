package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.data.OwnerData;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static io.github.chesno.petclinic.assertions.ApiAssertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Epic("PetClinic REST") @Feature("Owner validation") @Owner("Андрей Чесно")
class OwnerValidationApiTest extends BaseApiTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidOwners")
    void rejectsInvalidOwner(String scenario, OwnerRequest.Field field, String invalidValue) {
        OwnerRequest request = OwnerData.uniqueOwner().with(field, invalidValue);
        var response = Allure.step("POST invalid owner: " + scenario, () -> owners.create(request));
        assertAll("Validation error and no persisted owner",
                () -> Allure.step("HTTP status and Content-Type follow OpenAPI", () -> {
                    status(response, 400);
                    json(response);
                }),
                () -> Allure.step("Typed error body identifies the rejected field", () -> {
                    var error = problemBody(response, 400);
                    assertThat(error.schemaValidationErrors()).as("validation errors").isNotEmpty();
                    assertThat(error.schemaValidationErrors()).allSatisfy(message ->
                            assertThat(message.message()).as("validation message").isNotBlank());
                    // OpenAPI guarantees message, not an additional 'field' extension or exact localized wording.
                    assertThat(error.schemaValidationErrors()).as("error identifies rejected field %s", field.jsonName())
                            .anySatisfy(message -> assertThat(message.message()).contains(field.jsonName()));
                }),
                () -> Allure.step("Rejected owner was not persisted", () ->
                        assertThat(owners.findCreated(request)).as("invalid owner must not be persisted").isEmpty()));
    }

    static Stream<Arguments> invalidOwners() {
        Stream<Arguments> required = Arrays.stream(OwnerRequest.Field.values()).flatMap(field -> Stream.of(
                Arguments.of(field.jsonName() + " is empty", field, ""),
                Arguments.of(field.jsonName() + " is null", field, null)));
        Stream<Arguments> constraints = Stream.of(
                Arguments.of("firstName exceeds 30 characters", OwnerRequest.Field.FIRST_NAME, "A".repeat(31)),
                Arguments.of("lastName exceeds 30 characters", OwnerRequest.Field.LAST_NAME, "A".repeat(31)),
                Arguments.of("address exceeds 255 characters", OwnerRequest.Field.ADDRESS, "A".repeat(256)),
                Arguments.of("city exceeds 80 characters", OwnerRequest.Field.CITY, "A".repeat(81)),
                Arguments.of("telephone exceeds 20 characters", OwnerRequest.Field.TELEPHONE, "1".repeat(21)),
                Arguments.of("telephone contains letters", OwnerRequest.Field.TELEPHONE, "123ABC"),
                Arguments.of("firstName contains digits", OwnerRequest.Field.FIRST_NAME, "Andrey123"),
                Arguments.of("lastName contains digits", OwnerRequest.Field.LAST_NAME, "Chesno123"));
        return Stream.concat(required, constraints);
    }
}
