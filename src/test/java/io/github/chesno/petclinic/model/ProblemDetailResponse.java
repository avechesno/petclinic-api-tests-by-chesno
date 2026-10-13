package io.github.chesno.petclinic.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record ProblemDetailResponse(URI type, String title, Integer status, String detail,
                                    Instant timestamp, List<ValidationMessage> schemaValidationErrors) {
    // OpenAPI требует только message; field, rejectedValue и defaultMessage — необязательные расширения.
    public record ValidationMessage(String message, String field, String rejectedValue, String defaultMessage) { }
}
