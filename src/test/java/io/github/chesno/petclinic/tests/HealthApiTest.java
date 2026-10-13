package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.model.HealthResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.github.chesno.petclinic.assertions.ApiAssertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("PetClinic REST") @Feature("Health") @Owner("Андрей Чесно")
class HealthApiTest extends BaseApiTest {
    @Test
    @DisplayName("Health: HTTP 200 and status UP")
    void applicationIsHealthy() {
        var response = client.health();
        status(response, 200);
        // Actuator may use its vendor JSON media type; health is defined by the assignment, not openapi.yml.
        assertThat(response.contentType()).containsIgnoringCase("json");
        assertThat(response.bodyAs(HealthResponse.class).status()).isEqualTo("UP");
    }
}
