package io.github.chesno.petclinic.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chesno.petclinic.config.ApiProperties;
import io.github.chesno.petclinic.model.OwnerRequest;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PetclinicClient {
    private static final Logger LOG = LoggerFactory.getLogger(PetclinicClient.class);
    private final ApiProperties properties;
    private final ObjectMapper mapper;

    public PetclinicClient(ApiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    private RequestSpecification request() {
        // Per-request specifications: no mutable global RestAssured.baseURI/authentication/filters.
        return RestAssured.given().baseUri(properties.baseUrl()).accept(ContentType.JSON)
                .config(RestAssuredConfig.config()
                        .httpClient(HttpClientConfig.httpClientConfig()
                                .setParam("http.connection.timeout", properties.connectTimeoutMs())
                                .setParam("http.socket.timeout", properties.readTimeoutMs()))
                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                                .jackson2ObjectMapperFactory((type, charset) -> mapper)))
                .redirects().follow(false)
                .filter(new AllureRestAssured())
                .filter((req, res, context) -> {
                    var response = context.next(req, res);
                    LOG.info("{} {} -> {}", req.getMethod(), req.getURI(), response.statusCode());
                    return response;
                });
    }

    public ApiResponse health() {
        return new ApiResponse(request().get("/actuator/health"), mapper);
    }
    public ApiResponse createOwner(OwnerRequest owner) {
        return new ApiResponse(request().contentType(ContentType.JSON).body(owner).post("/api/owners"), mapper);
    }
    public ApiResponse getOwner(int id) {
        return new ApiResponse(request().get("/api/owners/{id}", id), mapper);
    }
    public ApiResponse updateOwner(int id, OwnerRequest owner) {
        return new ApiResponse(request().contentType(ContentType.JSON).body(owner).put("/api/owners/{id}", id), mapper);
    }
    public ApiResponse deleteOwner(int id) {
        return new ApiResponse(request().delete("/api/owners/{id}", id), mapper);
    }
    public ApiResponse findOwners(String lastName) {
        var request = request();
        if (lastName != null && !lastName.isBlank()) {
            request.queryParam("lastName", lastName);
        }
        return new ApiResponse(request.get("/api/owners"), mapper);
    }
}
