package it.dontesta.quarkus.tls.auth.ws.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.quarkus.tls.auth.ws.exception.CertificateConversionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.Base64;
import java.util.stream.Stream;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CertificateUtilTest {

  @ParameterizedTest
  @MethodSource("provideCertificatesForCommonName")
  void getCommonName(X509Certificate cert, String expectedCommonName) {
    String commonName = CertificateUtil.getCommonName(cert);
    assertEquals(expectedCommonName, commonName);
  }

  @ParameterizedTest
  @MethodSource("provideCertificatesForKeySize")
  void getKeySize(X509Certificate cert, int expectedKeySize) {
    int keySize = CertificateUtil.getKeySize(cert);
    assertEquals(expectedKeySize, keySize);
  }

  @Test
  void getCommonName_nullCertificate_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> {
      CertificateUtil.getCommonName(null);
    });
  }

  @Test
  void convertToBase64_validCertificate_returnsBase64String() throws Exception {
    X509Certificate mockCert = mock(X509Certificate.class);
    byte[] encodedCert = "mocked-certificate".getBytes();
    when(mockCert.getEncoded()).thenReturn(encodedCert);

    String base64Cert = CertificateUtil.convertToBase64(mockCert);
    String expectedBase64Cert = Base64.getEncoder().encodeToString(encodedCert);

    assertEquals(expectedBase64Cert, base64Cert);
  }

  @Test
  void convertToBase64_certificateEncodingException_throwsCertificateConversionException()
      throws Exception {
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getEncoded()).thenThrow(new CertificateEncodingException("Encoding error"));

    CertificateConversionException exception =
        assertThrows(CertificateConversionException.class, () -> {
          CertificateUtil.convertToBase64(mockCert);
        });

    assertEquals("Failed to convert X509Certificate to Base64 format", exception.getMessage());
  }

  @Test
  void decodeExtensionValue_validOctetString_returnsDecodedString() {
    byte[] extensionValue =
        new byte[] {0x04, 0x0B, 0x0C, 0x09, 'R', 'o', 'l', 'e', '=', 'U', 's', 'e', 'r'};
    String decodedValue = CertificateUtil.decodeExtensionValue(extensionValue);
    assertEquals("Role=User", decodedValue);
  }

  @Test
  void decodeExtensionValue_invalidOctetString_returnsNull() {
    byte[] extensionValue = new byte[] {0x05, 0x00}; // Invalid ASN.1 structure
    String decodedValue = CertificateUtil.decodeExtensionValue(extensionValue);
    assertNull(decodedValue);
  }

  @Test
  void decodeExtensionValue_emptyOctetString_returnsNull() {
    byte[] extensionValue = new byte[] {};
    String decodedValue = CertificateUtil.decodeExtensionValue(extensionValue);
    assertNull(decodedValue);
  }

  @Test
  void decodeExtensionValue_nullExtensionValue_throwsCertificateConversionException() {
    byte[] invalidAsn1Data = new byte[] {
        0x30, (byte) 0x80, // Start of a SEQUENCE with indefinite length
        0x02, 0x01, 0x01,  // INTEGER 1
        // Missing end-of-contents marker (0x00, 0x00)
    };
    assertThrows(CertificateConversionException.class, () -> {
      CertificateUtil.decodeExtensionValue(invalidAsn1Data);
    });
  }

  @Test
  void writePemToFile_validCertificate_writesPemToFile() throws Exception {
    X509Certificate mockCert = mock(X509Certificate.class);
    byte[] encodedCert = "mocked-certificate".getBytes();
    when(mockCert.getEncoded()).thenReturn(encodedCert);

    File tempFile = File.createTempFile("cert", ".pem");
    tempFile.deleteOnExit();

    CertificateUtil.writePemToFile(mockCert, tempFile.getAbsolutePath());

    String pemContent = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
    String base64Cert = Base64.getEncoder().encodeToString(encodedCert);

    assertTrue(pemContent.contains("-----BEGIN CERTIFICATE-----"));
    assertTrue(pemContent.contains("-----END CERTIFICATE-----"));
    assertTrue(pemContent.contains(base64Cert));
  }

  @Test
  void writePemToFile_certificateEncodingException_throwsCertificateConversionException()
      throws Exception {
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getEncoded()).thenThrow(new CertificateEncodingException("Encoding error"));

    File tempFile = File.createTempFile("cert", ".pem");
    String absolutePath = tempFile.getAbsolutePath();
    tempFile.deleteOnExit();

    CertificateConversionException exception =
        assertThrows(CertificateConversionException.class, () -> {
          CertificateUtil.writePemToFile(mockCert, absolutePath);
        });

    assertEquals("Failed to convert X509Certificate to PEM format", exception.getMessage());
  }

  @Test
  void writePemToFile_ioException_throwsCertificateConversionException() throws Exception {
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getEncoded()).thenReturn("mocked-certificate".getBytes());

    FileWriter mockWriter = mock(FileWriter.class);
    doThrow(new IOException("IO error")).when(mockWriter).write(anyString());

    CertificateConversionException exception =
        assertThrows(CertificateConversionException.class, () -> {
          CertificateUtil.writePemToFile(mockCert, "/invalid/path/cert.pem");
        });

    assertEquals("Failed to write PEM certificate to file", exception.getMessage());
  }

  @Test
  void testCertificateUtilPrivateConstructor() throws Exception {
    Constructor<CertificateUtil> constructor = CertificateUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    InvocationTargetException exception =
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    assertEquals("This is a utility class and cannot be instantiated",
        exception.getTargetException().getMessage());
  }

  private static Stream<Arguments> provideCertificatesForCommonName() {
    X509Certificate certWithCommonName = mock(X509Certificate.class);
    X500Principal principalWithCommonName =
        new X500Principal("CN=John Doe, OU=Test, O=Example, C=US");
    when(certWithCommonName.getSubjectX500Principal()).thenReturn(principalWithCommonName);

    X509Certificate certWithoutCommonName = mock(X509Certificate.class);
    when(certWithoutCommonName.getSubjectX500Principal()).thenReturn(null);

    X509Certificate certWithLowerCaseCn = mock(X509Certificate.class);
    X500Principal principalWithLowerCaseCn =
        new X500Principal("cn=Jane Doe, OU=Test, O=Example, C=US");
    when(certWithLowerCaseCn.getSubjectX500Principal()).thenReturn(principalWithLowerCaseCn);

    X509Certificate certWithMultipleCommonNames = mock(X509Certificate.class);
    X500Principal principalWithMultipleCommonNames =
        new X500Principal("CN=First, CN=Second, OU=Test, O=Example, C=US");
    when(certWithMultipleCommonNames.getSubjectX500Principal()).thenReturn(
        principalWithMultipleCommonNames);

    return Stream.of(
        Arguments.of(certWithCommonName, "John Doe"),
        Arguments.of(certWithoutCommonName, null),
        Arguments.of(certWithLowerCaseCn, "Jane Doe"),
        Arguments.of(certWithMultipleCommonNames, "First")
    );
  }

  private static Stream<Arguments> provideCertificatesForKeySize() {
    RSAPublicKey rsaKey = mock(RSAPublicKey.class);
    when(rsaKey.getModulus()).thenReturn(new BigInteger("123456789"));
    X509Certificate certWithRsaKey = mock(X509Certificate.class);
    when(certWithRsaKey.getPublicKey()).thenReturn(rsaKey);

    DSAPublicKey dsaKey = mock(DSAPublicKey.class);
    DSAParams dsaParams = mock(DSAParams.class);
    when(dsaParams.getP()).thenReturn(new BigInteger("123456789"));
    when(dsaKey.getParams()).thenReturn(dsaParams);
    X509Certificate certWithDsaKey = mock(X509Certificate.class);
    when(certWithDsaKey.getPublicKey()).thenReturn(dsaKey);

    ECPublicKey ecKey = mock(ECPublicKey.class);
    ECParameterSpec ecParams = mock(ECParameterSpec.class);
    EllipticCurve curve = mock(EllipticCurve.class);
    when(curve.getField()).thenReturn(new ECFieldFp(new BigInteger("123456789")));
    when(ecParams.getCurve()).thenReturn(curve);
    when(ecKey.getParams()).thenReturn(ecParams);
    X509Certificate certWithEcKey = mock(X509Certificate.class);
    when(certWithEcKey.getPublicKey()).thenReturn(ecKey);

    PublicKey unknownKey = mock(PublicKey.class);
    X509Certificate certWithUnknownKey = mock(X509Certificate.class);
    when(certWithUnknownKey.getPublicKey()).thenReturn(unknownKey);

    return Stream.of(
        Arguments.of(certWithRsaKey, 27),
        Arguments.of(certWithDsaKey, 27),
        Arguments.of(certWithEcKey, 27),
        Arguments.of(certWithUnknownKey, -1)
    );
  }
}