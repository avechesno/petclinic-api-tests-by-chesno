package io.github.chesno.petclinic.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

public record ApiResponse(Response raw, ObjectMapper mapper) {
    public int status() { return raw.statusCode(); }
    public String contentType() { return raw.contentType(); }
    public String location() { return raw.header("Location"); }
    public String body() { return raw.asString(); }

    public <T> T bodyAs(Class<T> type) {
        try {
            return mapper.readValue(raw.asString(), type);
        } catch (JsonProcessingException exception) {
            throw new AssertionError("Не удалось преобразовать ответ HTTP " + status() + " в " + type.getSimpleName()
                    + "; исходный HTTP-ответ приложен к отчёту Allure", exception);
        }
    }
}
