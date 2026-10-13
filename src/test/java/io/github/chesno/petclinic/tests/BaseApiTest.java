package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.client.PetclinicClient;
import io.github.chesno.petclinic.config.TestConfiguration;
import io.github.chesno.petclinic.steps.OwnerSteps;
import io.github.chesno.petclinic.support.OwnerTracker;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("api")
@SpringBootTest(classes = TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class BaseApiTest {
    @Autowired protected PetclinicClient client;
    private OwnerTracker owners;
    protected OwnerSteps steps;

    @BeforeEach
    void startOwnershipJournal() {
        owners = new OwnerTracker(client);
        steps = new OwnerSteps(client, owners);
    }

    @AfterEach
    void cleanupOwnedData() {
        // JUnit сохраняет и исходное падение теста, и ошибку очистки: исключения не скрываем, очистку не пропускаем.
        if (owners != null) Allure.step("Очистить данные текущего теста и подтвердить отсутствие записей через GET 404", owners::cleanup);
    }
}
