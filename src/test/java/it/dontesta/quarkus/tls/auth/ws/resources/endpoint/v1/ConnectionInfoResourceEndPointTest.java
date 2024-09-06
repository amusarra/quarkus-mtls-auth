package it.dontesta.quarkus.tls.auth.ws.resources.endpoint.v1;

import static io.restassured.RestAssured.given;
import static io.restassured.config.SSLConfig.sslConfig;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.http.ContentType;
import it.dontesta.quarkus.tls.auth.ws.utils.CertificateUtil;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConnectionInfoResourceEndPointTest {

  @ConfigProperty(name = "client.tls.https.key-store-client-1.p12.path")
  String keyStorePath1;

  @ConfigProperty(name = "client.tls.https.key-store-client-1.p12.password")
  String keyStorePassword1;

  @ConfigProperty(name = "client.tls.https.key-store-client-2.p12.path")
  String keyStorePath2;

  @ConfigProperty(name = "client.tls.https.key-store-client-2.p12.password")
  String keyStorePassword2;

  @ConfigProperty(name = "client.tls.https.key-store-client-3.p12.path")
  String keyStorePath3;

  @ConfigProperty(name = "client.tls.https.key-store-client-3.p12.password")
  String keyStorePassword3;

  // Variabile per mantenere il truststore configurato
  private static SSLConfig globalSSLConfig;

  @BeforeAll
  static void setup() throws Exception {
    String[] trustStorePath = ConfigProvider.getConfig()
        .getValue("quarkus.tls.trust-store.pem.certs", String.class).split(",");

    // Carica il trust store della CA in formato PEM
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    trustStore.load(null, null);

    Arrays.stream(trustStorePath).map(String::trim).forEachOrdered(trustStorePemPath -> {
      try (var trustStoreStream = ConnectionInfoResourceEndPointTest.class.getClassLoader()
          .getResourceAsStream(trustStorePemPath)) {
        X509Certificate trustCert = (X509Certificate) cf.generateCertificate(trustStoreStream);
        trustStore.setCertificateEntry(CertificateUtil.getCommonName(trustCert), trustCert);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // Crea una configurazione SSL con solo il truststore
    globalSSLConfig = sslConfig().trustStore(trustStore);

    // Imposta la configurazione globale per RestAssured (solo truststore)
    RestAssured.config = RestAssured.config().sslConfig(globalSSLConfig);

  }

  @Test
  void testGetConnectionInfo() {
    SSLConfig testSSLConfig = globalSSLConfig.with().keyStore(keyStorePath1, keyStorePassword1);

    given()
        .config(RestAssured.config().sslConfig(testSSLConfig))
        .get("/api/v1/connection-info/info")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("isSecure", equalTo(true))
        .body("httpProtocol", equalTo("HTTP_1_1"))
        .body("userAgent", anything())
        .body("protocol", equalTo("TLSv1.3"))
        .body("client.certCommonName", equalTo("D54FF113-A0C4-4A67-A6DE-B8DEE24A5095"));
  }

  @Test
  void testGetUserIdentity() {
    SSLConfig testSSLConfig = globalSSLConfig.with().keyStore(keyStorePath2, keyStorePassword2);

    List<String> roles = Arrays.asList("User", "Administrator", "HorseManager");

    given()
        .config(RestAssured.config().sslConfig(testSSLConfig))
        .when().get("/api/v1/connection-info/user-identity")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("principal", equalTo(
            "CN=C708ECA6-C8F9-464C-A745-3A37FF670DFC,OU=Horse Sporting Club,O=Judio Rocio Corporation,L=Rome,ST=Italy,C=IT"))
        .body("roles", equalTo(roles))
        .body("attributes.deviceId", equalTo("loGdwvje+MLyEJQt"))
        .body("userCN", equalTo("C708ECA6-C8F9-464C-A745-3A37FF670DFC"));
  }

  @Test
  void testGetUserIdentityHttpStatus403() {
    SSLConfig testSSLConfig = globalSSLConfig.with().keyStore(keyStorePath3, keyStorePassword3);

    given()
        .config(RestAssured.config().sslConfig(testSSLConfig))
        .when().get("/api/v1/connection-info/user-identity")
        .then()
        .statusCode(403);
  }
}