package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.client.PetclinicClient;
import io.github.chesno.petclinic.config.TestConfiguration;
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
    protected OwnerTracker owners;

    @BeforeEach
    void startOwnershipJournal() { owners = new OwnerTracker(client); }

    @AfterEach
    void cleanupOwnedData() {
        // JUnit preserves the test failure AND any teardown failure; no swallowing, no early return on failure.
        if (owners != null) Allure.step("Cleanup: remove only test-owned data and verify GET 404", owners::cleanup);
    }
}
