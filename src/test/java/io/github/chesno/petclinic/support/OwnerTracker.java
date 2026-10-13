package io.github.chesno.petclinic.support;

import io.github.chesno.petclinic.client.ApiResponse;
import io.github.chesno.petclinic.client.PetclinicClient;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.github.chesno.petclinic.model.OwnerResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.opentest4j.MultipleFailuresError;
import static org.assertj.core.api.Assertions.assertThat;

/** Журнал данных отдельного теста. Удаляются только записи, принадлежность которых текущему тесту подтверждена. */
public final class OwnerTracker {
    private final PetclinicClient client;
    private final List<Entry> entries = new ArrayList<>();

    public OwnerTracker(PetclinicClient client) { this.client = client; }

    public ApiResponse create(OwnerRequest request) {
        Entry entry = new Entry(request);
        // Регистрируем попытку до HTTP-запроса: после сохранения на сервере возможны таймаут или некорректный ответ.
        entries.add(entry);
        ApiResponse response = client.createOwner(request);
        entry.id = recoverId(response);
        return response;
    }

    public ApiResponse update(int id, OwnerRequest request) {
        entries.stream().filter(entry -> Integer.valueOf(id).equals(entry.id))
                .forEach(entry -> entry.versions.add(request));
        return client.updateOwner(id, request);
    }

    public List<OwnerResponse> findCreated(OwnerRequest request) {
        ApiResponse response = client.findOwners(request.lastName());
        if (response.status() == 404) return List.of();
        assertThat(response.status()).as("Получение списка owners для поиска тестовых записей").isEqualTo(200);
        return Arrays.stream(response.bodyAs(OwnerResponse[].class))
                .filter(owner -> ownedBy(owner, request)).toList();
    }

    public void cleanup() {
        List<Throwable> failures = new ArrayList<>();
        for (Entry entry : entries) {
            Set<Integer> ids = new LinkedHashSet<>();
            try {
                if (entry.id != null) ids.add(entry.id);
                else findCreated(entry.versions.get(0)).forEach(owner -> ids.add(owner.id()));
            } catch (Exception | AssertionError error) {
                failures.add(new AssertionError("Не удалось найти созданного owner; данные для поиска: " + entry.versions.get(0), error));
                continue;
            }
            for (Integer id : ids) {
                try {
                    deleteAndVerify(id, entry);
                } catch (Exception | AssertionError error) {
                    failures.add(new AssertionError("Не удалось очистить данные owner id=" + id, error));
                }
            }
        }
        entries.clear();
        if (!failures.isEmpty()) throw aggregate("Ошибки очистки тестовых данных", failures);
    }

    private void deleteAndVerify(Integer id, Entry entry) {
        assertThat(id).as("Восстановленный owner ID для очистки").isNotNull().isNotNegative();
        ApiResponse before = client.getOwner(id);
        if (before.status() == 404) return; // Запись уже удалена сценарием; её отсутствие подтверждено через GET.
        assertThat(before.status()).as("Получение owner перед очисткой").isEqualTo(200);
        OwnerResponse owner = before.bodyAs(OwnerResponse.class);
        assertThat(entry.versions.stream().anyMatch(request -> ownedBy(owner, request)))
                .as("Удаление owner %s запрещено без уникального маркера текущего теста", id).isTrue();

        // Очистка — защитный механизм, а не проверка контракта. В сценарии ожидается строго HTTP 200.
        // Даже при нарушении контракта DELETE отдельно проверяем фактическое отсутствие записи.
        List<Throwable> failures = new ArrayList<>();
        try {
            ApiResponse deleted = client.deleteOwner(id);
            assertThat(deleted.status()).as("Результат DELETE при служебной очистке").isIn(200, 204, 404);
        } catch (Exception | AssertionError error) { failures.add(error); }
        try {
            assertThat(client.getOwner(id).status()).as("GET после очистки должен подтвердить отсутствие записи").isEqualTo(404);
        } catch (Exception | AssertionError error) { failures.add(error); }
        if (!failures.isEmpty()) throw aggregate("Проверка удаления owner " + id + " через DELETE и GET", failures);
    }

    private static MultipleFailuresError aggregate(String heading, List<Throwable> failures) {
        MultipleFailuresError error = new MultipleFailuresError(heading, failures);
        // Сохраняем вложенные причины в трассировке стека и Allure, а не только в getFailures().
        failures.forEach(error::addSuppressed);
        return error;
    }

    private static boolean ownedBy(OwnerResponse owner, OwnerRequest request) {
        if (owner == null) return false;
        boolean sameName = request.lastName() != null && request.lastName().startsWith("Qa")
                && request.lastName().length() >= 20 && request.lastName().equals(owner.lastName());
        boolean sameAddress = request.address() != null && request.address().startsWith("QA-")
                && request.address().length() >= 30 && request.address().equals(owner.address());
        return sameName || sameAddress;
    }

    private static Integer recoverId(ApiResponse response) {
        // Восстановление ID не подтверждает успешность операции и не скрывает исходную ошибку контракта или разбора.
        try {
            CreatedIdentity identity = response.bodyAs(CreatedIdentity.class);
            if (identity != null && identity.id() != null && identity.id() >= 0) return identity.id();
        } catch (Exception | AssertionError ignored) { /* Пробуем Location, затем уникальный маркер при очистке. */ }
        try {
            String path = URI.create(response.location()).getPath();
            if (path != null && path.matches(".*/api/owners/[0-9]+/?")) {
                String[] parts = path.split("/");
                return Integer.valueOf(parts[parts.length - 1]);
            }
        } catch (Exception ignored) { /* Без ID очистка найдёт запись по уникальному маркеру через типизированный ответ. */ }
        return null;
    }

    public record CreatedIdentity(Integer id) { }
    private static final class Entry {
        private Integer id;
        private final List<OwnerRequest> versions = new ArrayList<>();
        private Entry(OwnerRequest request) { versions.add(request); }
    }
}
