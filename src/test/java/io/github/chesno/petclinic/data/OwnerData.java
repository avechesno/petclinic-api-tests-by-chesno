package io.github.chesno.petclinic.data;

import io.github.chesno.petclinic.model.OwnerRequest;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class OwnerData {
    private OwnerData() { }

    public static OwnerRequest uniqueOwner() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        StringBuilder name = new StringBuilder("Qa");
        // Имя должно содержать буквы, а не цифры UUID; сохраняем 80 случайных бит при ограничении длины 30 символов.
        for (char digit : token.toCharArray()) {
            name.append((char) ('a' + Character.digit(digit, 16)));
        }
        return new OwnerRequest("Andrey", name.toString(), "QA-" + UUID.randomUUID(), "Madison",
                Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L)));
    }

    public static OwnerRequest updatedOwner(OwnerRequest original) {
        OwnerRequest unique = uniqueOwner();
        String phone = original.telephone().equals("9999999999") ? "8888888888" : "9999999999";
        return new OwnerRequest("Andrew", unique.lastName(), unique.address(), "Boston", phone);
    }
}
