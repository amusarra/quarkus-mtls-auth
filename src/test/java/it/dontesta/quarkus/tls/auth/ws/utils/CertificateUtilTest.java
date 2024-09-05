package it.dontesta.quarkus.tls.auth.ws.utils;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import it.dontesta.quarkus.tls.auth.ws.exception.CertificateConversionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

@QuarkusTest
class CertificateUtilTest {

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
    tempFile.deleteOnExit();

    CertificateConversionException exception =
        assertThrows(CertificateConversionException.class, () -> {
          CertificateUtil.writePemToFile(mockCert, tempFile.getAbsolutePath());
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
  void getKeySize_rsaKey_returnsKeySize() {
    RSAPublicKey mockRsaKey = mock(RSAPublicKey.class);
    when(mockRsaKey.getModulus()).thenReturn(new BigInteger("123456789"));
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getPublicKey()).thenReturn(mockRsaKey);

    int keySize = CertificateUtil.getKeySize(mockCert);

    assertEquals(27, keySize); // 123456789 in binary is 27 bits long
  }

  @Test
  void getKeySize_dsaKey_returnsKeySize() {
    DSAPublicKey mockDsaKey = mock(DSAPublicKey.class);
    DSAParams mockDsaParams = mock(DSAParams.class);
    when(mockDsaParams.getP()).thenReturn(new BigInteger("123456789"));
    when(mockDsaKey.getParams()).thenReturn(mockDsaParams);
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getPublicKey()).thenReturn(mockDsaKey);

    int keySize = CertificateUtil.getKeySize(mockCert);

    assertEquals(27, keySize); // 123456789 in binary is 27 bits long
  }

  @Test
  void getKeySize_ecKey_returnsKeySize() {
    ECPublicKey mockEcKey = mock(ECPublicKey.class);
    ECParameterSpec mockEcParams = mock(ECParameterSpec.class);
    EllipticCurve mockCurve = mock(EllipticCurve.class);
    when(mockCurve.getField()).thenReturn(new ECFieldFp(new BigInteger("123456789")));
    when(mockEcParams.getCurve()).thenReturn(mockCurve);
    when(mockEcKey.getParams()).thenReturn(mockEcParams);
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getPublicKey()).thenReturn(mockEcKey);

    int keySize = CertificateUtil.getKeySize(mockCert);

    assertEquals(27, keySize); // 123456789 in binary is 27 bits long
  }

  @Test
  void getKeySize_unknownKey_returnsNegativeOne() {
    PublicKey mockUnknownKey = mock(PublicKey.class);
    X509Certificate mockCert = mock(X509Certificate.class);
    when(mockCert.getPublicKey()).thenReturn(mockUnknownKey);

    int keySize = CertificateUtil.getKeySize(mockCert);

    assertEquals(-1, keySize);
  }
}