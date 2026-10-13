package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.model.HealthResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.github.chesno.petclinic.assertions.ApiAssertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("API приложения PetClinic") @Feature("Проверка доступности сервиса") @Owner("Андрей Чесно")
class HealthApiTest extends BaseApiTest {
    @Test
    @DisplayName("Проверка доступности сервиса: HTTP 200 и статус UP")
    void applicationIsHealthy() {
        var response = client.health();
        status(response, 200);
        // Actuator может возвращать собственный тип JSON; требования к health заданы в задании, а не в openapi.yml.
        assertThat(response.contentType()).as("Ответ о состоянии сервиса представлен в формате JSON")
                .containsIgnoringCase("json");
        assertThat(response.bodyAs(HealthResponse.class).status()).as("Состояние сервиса в поле status").isEqualTo("UP");
    }
}
