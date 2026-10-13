package io.github.chesno.petclinic.model;

import java.time.LocalDate;
import java.util.List;

public record OwnerResponse(Integer id, String firstName, String lastName, String address,
                            String city, String telephone, List<PetResponse> pets) {
    public OwnerRequest fields() {
        return new OwnerRequest(firstName, lastName, address, city, telephone);
    }

    public record PetResponse(Integer id, String name, LocalDate birthDate, PetType type,
                              Integer ownerId, List<VisitResponse> visits) { }
    public record PetType(Integer id, String name) { }
    public record VisitResponse(Integer id, LocalDate date, String description) { }
}
