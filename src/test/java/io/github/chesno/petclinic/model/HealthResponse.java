package io.github.chesno.petclinic.model;

import java.util.List;

public record HealthResponse(String status, List<String> groups) { }
