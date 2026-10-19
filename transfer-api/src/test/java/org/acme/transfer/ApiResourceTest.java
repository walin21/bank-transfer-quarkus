package org.acme.transfer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ApiResourceTest {
  @Inject
  @Connector("smallrye-in-memory")
  InMemoryConnector connector;

  private String token;

  @BeforeEach
  void login() {
    token =
        given()
            .contentType("application/json")
            .body(Map.of("email", "customer@test.local", "password", "secret"))
            .when()
            .post("/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .path("accessToken");
  }

  @Test
  void rejectsUnauthenticatedRequestsAndReturnsAccountsWithToken() {
    given().when().get("/api/v1/accounts").then().statusCode(401);

    given()
        .auth()
        .oauth2(token)
        .when()
        .get("/api/v1/accounts")
        .then()
        .statusCode(200)
        .body("", hasSize(1))
        .body("[0].currency", equalTo("PEN"));
  }

  @Test
  void createsBeneficiaryAndPublishesPendingTransfer() {
    String beneficiaryId =
        given()
            .auth()
            .oauth2(token)
            .contentType("application/json")
            .body(
                Map.of(
                    "alias",
                    "Savings",
                    "bankName",
                    "Test Bank",
                    "accountNumber",
                    "999",
                    "currency",
                    "PEN"))
            .when()
            .post("/api/v1/beneficiaries")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    InMemorySink<?> sink = connector.sink("transfer-created");
    given()
        .auth()
        .oauth2(token)
        .contentType("application/json")
        .body(
            Map.of(
                "sourceAccountId",
                "22222222-2222-2222-2222-222222222222",
                "beneficiaryId",
                beneficiaryId,
                "amount",
                25.50))
        .when()
        .post("/api/v1/transfers")
        .then()
        .statusCode(202)
        .body("status", equalTo("PENDING"));

    assertEquals(1, sink.received().size());
  }

  @Test
  void rejectsInvalidTransferPayload() {
    given()
        .auth()
        .oauth2(token)
        .contentType("application/json")
        .body(Map.of("amount", 0))
        .when()
        .post("/api/v1/transfers")
        .then()
        .statusCode(400)
        .body("code", equalTo("VALIDATION_ERROR"));
  }
}
