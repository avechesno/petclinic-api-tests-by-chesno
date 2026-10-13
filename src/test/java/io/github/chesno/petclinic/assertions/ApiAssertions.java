package io.github.chesno.petclinic.assertions;

import io.github.chesno.petclinic.client.ApiResponse;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.github.chesno.petclinic.model.OwnerResponse;
import io.github.chesno.petclinic.model.ProblemDetailResponse;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class ApiAssertions {
    private ApiAssertions() { }

    public static void status(ApiResponse response, int expected) {
        assertThat(response.status()).as("HTTP status defined by the pinned OpenAPI contract").isEqualTo(expected);
    }

    public static void json(ApiResponse response) {
        assertThat(response.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT))
                .as("Content-Type defined by OpenAPI").isEqualTo("application/json");
    }

    public static OwnerResponse owner(ApiResponse response, int expectedStatus, OwnerRequest expected, Integer expectedId) {
        // Status and media type before deserialization: HTML/error responses cannot masquerade as Owner.
        status(response, expectedStatus);
        json(response);
        OwnerResponse actual = response.bodyAs(OwnerResponse.class);
        assertThat(actual).as("Owner response body").isNotNull();
        assertAll("Owner ID, all editable fields and pets",
                () -> assertThat(actual.id()).as("assigned ID").isNotNull().isNotNegative(),
                () -> { if (expectedId != null) assertThat(actual.id()).isEqualTo(expectedId); },
                () -> assertThat(actual.fields()).as("persisted fields vs sent request").isEqualTo(expected),
                () -> assertThat(actual.pets()).as("new owner's pets").isNotNull().isEmpty());
        return actual;
    }

    public static ProblemDetailResponse problem(ApiResponse response, int expectedStatus) {
        status(response, expectedStatus);
        json(response);
        return problemBody(response, expectedStatus);
    }

    public static ProblemDetailResponse problemBody(ApiResponse response, int expectedStatus) {
        status(response, expectedStatus);
        // Independent body check: callers still assert the exact contract Content-Type separately.
        // A media-type defect must not hide missing/wrong business error fields.
        ProblemDetailResponse actual = response.bodyAs(ProblemDetailResponse.class);
        assertThat(actual).isNotNull();
        assertAll("Required ProblemDetail fields",
                () -> assertThat(actual.status()).isEqualTo(expectedStatus),
                () -> { assertThat(actual.type()).isNotNull(); assertThat(actual.type().isAbsolute()).isTrue(); },
                () -> assertThat(actual.title()).isNotBlank(),
                () -> assertThat(actual.detail()).isNotBlank(),
                () -> assertThat(actual.timestamp()).isNotNull(),
                () -> assertThat(actual.schemaValidationErrors()).isNotNull());
        return actual;
    }
}
