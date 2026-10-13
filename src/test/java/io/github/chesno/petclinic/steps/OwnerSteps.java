package io.github.chesno.petclinic.steps;

import io.github.chesno.petclinic.client.ApiResponse;
import io.github.chesno.petclinic.client.PetclinicClient;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.github.chesno.petclinic.model.OwnerResponse;
import io.github.chesno.petclinic.support.OwnerTracker;
import io.qameta.allure.Allure;
import java.util.List;
import static io.github.chesno.petclinic.assertions.OwnerAssertions.owner;

/** Действия отдельного теста. Изменения регистрируются до проверок и последующих запросов. */
public final class OwnerSteps {
    private final PetclinicClient client;
    private final OwnerTracker tracker;

    public OwnerSteps(PetclinicClient client, OwnerTracker tracker) {
        this.client = client;
        this.tracker = tracker;
    }

    public ApiResponse createOwner(OwnerRequest request) {
        return Allure.step("Owner: создать и зарегистрировать для очистки — POST /api/owners", () -> tracker.create(request));
    }

    public OwnerResponse createVerifiedOwner(OwnerRequest request) {
        return Allure.step("Owner: подготовить для текущего теста и проверить HTTP 201 с переданными данными", () ->
                owner(createOwner(request), 201, request, null));
    }

    public ApiResponse getOwner(int id) {
        return Allure.step("Owner: получить по ID — GET /api/owners/" + id, () -> client.getOwner(id));
    }

    public ApiResponse updateOwner(int id, OwnerRequest request) {
        return Allure.step("Owner: обновить и учесть новые данные для очистки — PUT /api/owners/" + id, () ->
                tracker.update(id, request));
    }

    public ApiResponse deleteOwner(int id) {
        // Сохраняем ID в журнале: очистка проверит отсутствие записи, даже если проверка сценария упадёт.
        return Allure.step("Owner: удалить — DELETE /api/owners/" + id, () -> client.deleteOwner(id));
    }

    public List<OwnerResponse> findCreatedOwners(OwnerRequest request) {
        return Allure.step("Owner: найти записи текущего теста по уникальным маркерам", () -> tracker.findCreated(request));
    }
}
