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

/** Per-test ownership journal. Only unique test-owned records may be removed. */
public final class OwnerTracker {
    private final PetclinicClient client;
    private final List<Entry> entries = new ArrayList<>();

    public OwnerTracker(PetclinicClient client) { this.client = client; }

    public ApiResponse create(OwnerRequest request) {
        Entry entry = new Entry(request);
        // Journal BEFORE the HTTP request: the server may commit and then time out or return a malformed body.
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
        assertThat(response.status()).as("Ownership recovery: list owners").isEqualTo(200);
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
                failures.add(new AssertionError("Unable to recover created owner; marker: " + entry.versions.get(0), error));
                continue;
            }
            for (Integer id : ids) {
                try {
                    deleteAndVerify(id, entry);
                } catch (Exception | AssertionError error) {
                    failures.add(new AssertionError("Cleanup failed for owner id=" + id, error));
                }
            }
        }
        entries.clear();
        if (!failures.isEmpty()) throw aggregate("Test data cleanup failures", failures);
    }

    private void deleteAndVerify(Integer id, Entry entry) {
        assertThat(id).as("recoverable owner ID").isNotNull().isNotNegative();
        ApiResponse before = client.getOwner(id);
        if (before.status() == 404) return; // Already removed by the scenario, verified with GET.
        assertThat(before.status()).as("GET before cleanup").isEqualTo(200);
        OwnerResponse owner = before.bodyAs(OwnerResponse.class);
        assertThat(entry.versions.stream().anyMatch(request -> ownedBy(owner, request)))
                .as("Refuse to delete owner %s without this test's unique marker", id).isTrue();

        // Cleanup is a safety mechanism, NOT a contract test. The scenario asserts exactly 200.
        // Even if DELETE violates the contract, independently check whether data is actually gone.
        List<Throwable> failures = new ArrayList<>();
        try {
            ApiResponse deleted = client.deleteOwner(id);
            assertThat(deleted.status()).as("Cleanup DELETE result").isIn(200, 204, 404);
        } catch (Exception | AssertionError error) { failures.add(error); }
        try {
            assertThat(client.getOwner(id).status()).as("GET after cleanup must prove absence").isEqualTo(404);
        } catch (Exception | AssertionError error) { failures.add(error); }
        if (!failures.isEmpty()) throw aggregate("DELETE/GET verification for " + id, failures);
    }

    private static MultipleFailuresError aggregate(String heading, List<Throwable> failures) {
        MultipleFailuresError error = new MultipleFailuresError(heading, failures);
        // Preserve nested causes in ordinary stack traces and Allure, not only in getFailures().
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
        // ID recovery must not assert success or mask the original contract/deserialization failure.
        try {
            CreatedIdentity identity = response.bodyAs(CreatedIdentity.class);
            if (identity != null && identity.id() != null && identity.id() >= 0) return identity.id();
        } catch (Exception | AssertionError ignored) { /* Try Location, then unique marker during teardown. */ }
        try {
            String path = URI.create(response.location()).getPath();
            if (path != null && path.matches(".*/api/owners/[0-9]+/?")) {
                String[] parts = path.split("/");
                return Integer.valueOf(parts[parts.length - 1]);
            }
        } catch (Exception ignored) { /* No usable ID: teardown performs typed lookup by unique marker. */ }
        return null;
    }

    public record CreatedIdentity(Integer id) { }
    private static final class Entry {
        private Integer id;
        private final List<OwnerRequest> versions = new ArrayList<>();
        private Entry(OwnerRequest request) { versions.add(request); }
    }
}
