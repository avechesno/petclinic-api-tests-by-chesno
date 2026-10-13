package io.github.chesno.petclinic.tests;

import io.github.chesno.petclinic.data.OwnerData;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.github.chesno.petclinic.assertions.ApiAssertions.emptyBody;
import static io.github.chesno.petclinic.assertions.OwnerAssertions.owner;
import static org.junit.jupiter.api.Assertions.assertAll;

@Epic("API приложения PetClinic") @Feature("Owner API: полный CRUD") @Owner("Андрей Чесно")
class OwnerCrudApiTest extends BaseApiTest {
    @Test
    @DisplayName("Owner CRUD: создание, получение, обновление и удаление")
    void ownerLifecyclePersistsAndRemovesData() {
        OwnerRequest initial = OwnerData.uniqueOwner();
        int id = owner(steps.createOwner(initial), 201, initial, null).id();

        owner(steps.getOwner(id), 200, initial, id);

        OwnerRequest updated = OwnerData.updatedOwner(initial);
        var updateResponse = steps.updateOwner(id, updated);

        // PUT и DELETE не возвращают тело; итог каждой операции проверяется отдельным GET.
        assertAll("Owner CRUD: HTTP-коды, сохранение изменений и отсутствие удалённой записи",
                () -> emptyBody(updateResponse, 204),
                () -> owner(steps.getOwner(id), 200, updated, id),
                () -> {
                    var deleteResponse = steps.deleteOwner(id);
                    assertAll("HTTP 204 после DELETE и фактическое отсутствие записи",
                            () -> emptyBody(deleteResponse, 204),
                            () -> emptyBody(steps.getOwner(id), 404));
                });
    }
}
