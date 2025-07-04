import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTests {
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004/";
    }

    @Test
    public void shouldReturnOKWithValidToken() {
        // 1. Arrange (set up what tests need to be ready)
        // 2. act (means a code which triggers the things which it's testing
        // 3. assert (comparison with expected result)

        String loginPayLoad = """
                {
                    "email": "testuser@test.com",
                    "password": "password123"
                }
                """;

        Response response = given()
                .contentType("application/json")
                .body(loginPayLoad)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract().response();

        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }
}
