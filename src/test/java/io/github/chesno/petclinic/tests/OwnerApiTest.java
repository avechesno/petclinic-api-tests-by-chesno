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

@Epic("API приложения PetClinic") @Feature("Owner API: независимые операции") @Owner("Андрей Чесно")
class OwnerApiTest extends BaseApiTest {
    @Test
    @DisplayName("Owner: создание с HTTP 201 и сохранение переданных данных")
    void createsOwnerWithSubmittedData() {
        OwnerRequest request = OwnerData.uniqueOwner();

        var response = steps.createOwner(request);

        int id = owner(response, 201, request, null).id();
        owner(steps.getOwner(id), 200, request, id);
    }

    @Test
    @DisplayName("Owner: получение по ID с HTTP 200 и корректными данными")
    void readsOwnOwnerById() {
        OwnerRequest request = OwnerData.uniqueOwner();
        int id = steps.createVerifiedOwner(request).id();

        var response = steps.getOwner(id);

        owner(response, 200, request, id);
    }

    @Test
    @DisplayName("Owner: обновление с HTTP 204 и сохранение всех изменённых полей")
    void updatesAllOwnerFields() {
        OwnerRequest initial = OwnerData.uniqueOwner();
        int id = steps.createVerifiedOwner(initial).id();
        OwnerRequest updated = OwnerData.updatedOwner(initial);

        var response = steps.updateOwner(id, updated);

        // PUT не возвращает тело, поэтому результат операции проверяется отдельным GET.
        assertAll("HTTP 204 после PUT и сохранение изменённых данных",
                () -> emptyBody(response, 204),
                () -> owner(steps.getOwner(id), 200, updated, id));
    }

    @Test
    @DisplayName("Owner: удаление с HTTP 204, повторный GET — HTTP 404 без тела")
    void deletesOwnOwnerAndVerifiesAbsence() {
        OwnerRequest request = OwnerData.uniqueOwner();
        int id = steps.createVerifiedOwner(request).id();
        owner(steps.getOwner(id), 200, request, id);

        var response = steps.deleteOwner(id);

        assertAll("HTTP 204 после DELETE и фактическое отсутствие записи",
                () -> emptyBody(response, 204),
                () -> emptyBody(steps.getOwner(id), 404));
    }
}
