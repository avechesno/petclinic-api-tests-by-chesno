package io.github.chesno.petclinic.model;

public record OwnerRequest(String firstName, String lastName, String address, String city, String telephone) {
    public OwnerRequest with(Field field, String value) {
        return switch (field) {
            case FIRST_NAME -> new OwnerRequest(value, lastName, address, city, telephone);
            case LAST_NAME -> new OwnerRequest(firstName, value, address, city, telephone);
            case ADDRESS -> new OwnerRequest(firstName, lastName, value, city, telephone);
            case CITY -> new OwnerRequest(firstName, lastName, address, value, telephone);
            case TELEPHONE -> new OwnerRequest(firstName, lastName, address, city, value);
        };
    }

    public enum Field {
        FIRST_NAME("firstName"), LAST_NAME("lastName"), ADDRESS("address"), CITY("city"), TELEPHONE("telephone");
        private final String jsonName;
        Field(String jsonName) { this.jsonName = jsonName; }
        public String jsonName() { return jsonName; }
    }
}
