import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {
    /** Set the base URI for all HTTP requests made with RestAssured */
    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnPatientsWithValidToken() {
        /** Prepare the login payload with user credentials (email and password) */
        String loginPayLoad = """
                {
                    "email": "invalid_user@test.com",
                    "password": "wrongpassword"
                }
                """;

        /** Start building the request. */
        String token = given()
                .contentType("application/json") /** Set request header to specify the body is JSON. */

                .body(loginPayLoad) /** Attach the request body (login JSON). */

                .when() /** Triggers the actual HTTP request. */

                .post("/auth/login") /** Specify the HTTP POST method and target URL. */

                .then() /** Starts validating the response. */

                .statusCode(200) /** Expect the response to have HTTP 200 OK. */

                .extract() /** Start extracting data from the response. */

                .jsonPath() /** Converts the response body to a JSON object. */

                .get("token"); /** Retrieving the "token" field from the JSON. */

        /******************************************************************************/

        /** Send a GET request to /patients with the Authorization header containing the JWT token */
        given()
                .header("Authorization", "Bearer " + token) /** Set the Authorization header with the Bearer token */

                .when() /** Trigger the request */

                .get("/patients") /** Specify the endpoint to call */

                .then() /** Start verifying the response */

                .statusCode(200) /** OK */

                .body("patients", notNullValue()); /** Assert that the "patients" field in the response is not null */
    }
}
