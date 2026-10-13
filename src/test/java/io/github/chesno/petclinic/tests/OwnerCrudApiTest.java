package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.data.OwnerData;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.github.chesno.petclinic.assertions.ApiAssertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@Epic("PetClinic REST") @Feature("Owner lifecycle") @Owner("Андрей Чесно")
class OwnerCrudApiTest extends BaseApiTest {
    @Test
    @DisplayName("Owner CRUD: create, read, update, read, delete, verify absence")
    void ownerLifecycleMatchesContractAndPersistsData() {
        OwnerRequest initial = OwnerData.uniqueOwner();
        int id = Allure.step("POST owner: 201 and every sent field returned", () ->
                owner(owners.create(initial), 201, initial, null).id());

        Allure.step("GET created owner: persisted data equals POST request", () ->
                owner(client.getOwner(id), 200, initial, id));

        OwnerRequest updated = OwnerData.updatedOwner(initial);
        var updateResponse = Allure.step("PUT changed fields", () -> owners.update(id, updated));

        // Independent checks all run even on a contract mismatch (e.g. PUT returns 204 instead of 200).
        assertAll("Update and delete contract, persisted state and final absence",
                () -> Allure.step("PUT response: OpenAPI requires 200 and Owner", () ->
                        owner(updateResponse, 200, updated, id)),
                () -> Allure.step("GET updated owner: every changed field is saved", () ->
                        owner(client.getOwner(id), 200, updated, id)),
                () -> {
                    var deleteResponse = Allure.step("DELETE owner", () -> client.deleteOwner(id));
                    assertAll("Deletion response and actual absence",
                            () -> Allure.step("DELETE response: OpenAPI requires 200 and Owner", () ->
                                    owner(deleteResponse, 200, updated, id)),
                            () -> Allure.step("GET after DELETE: 404 and ProblemDetail required by OpenAPI", () ->
                                    problem(client.getOwner(id), 404)));
                });
    }
}
