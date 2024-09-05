package it.dontesta.quarkus.tls.auth.ws.utils;

import it.dontesta.quarkus.tls.auth.ws.exception.CertificateConversionException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Utility class for converting X509Certificates to different formats.
 *
 * @author Antonio Musarra
 */
public class CertificateUtil {

  /**
   * Converts an X509Certificate object to a Base64 encoded string.
   *
   * @param cert The X509Certificate to convert.
   * @return The Base64 encoded certificate as a String.
   * @throws CertificateConversionException If an error occurs during conversion.
   */
  public static String convertToBase64(X509Certificate cert) throws CertificateConversionException {
    try {
      byte[] encodedCert = cert.getEncoded();
      return Base64.getEncoder().encodeToString(encodedCert);
    } catch (Exception e) {
      throw new CertificateConversionException("Failed to convert X509Certificate to Base64 format",
          e);
    }
  }

  /**
   * Converts an X509Certificate object to PEM format.
   *
   * @param cert The X509Certificate to convert.
   * @return The PEM formatted certificate as a String.
   * @throws CertificateConversionException If an error occurs during conversion.
   */
  public static String convertToPem(X509Certificate cert) throws CertificateConversionException {
    try {
      // Encode the byte array to Base64
      String base64Cert = convertToBase64(cert);

      // Get the system-dependent line separator
      String lineSeparator = System.lineSeparator();

      // Create a StringBuilder to construct the PEM formatted certificate
      StringBuilder pemCert = new StringBuilder();
      pemCert.append("-----BEGIN CERTIFICATE-----").append(lineSeparator);

      // Split the Base64 encoded string into lines of 64 characters for readability
      // (common in PEM files)
      int index = 0;
      while (index < base64Cert.length()) {
        pemCert.append(base64Cert, index, Math.min(index + 64, base64Cert.length()))
            .append(lineSeparator);
        index += 64;
      }

      pemCert.append("-----END CERTIFICATE-----").append(lineSeparator);

      // Return the PEM formatted certificate
      return pemCert.toString();
    } catch (Exception e) {
      throw new CertificateConversionException("Failed to convert X509Certificate to PEM format",
          e);
    }
  }

  /**
   * Gets the key size of the public key in the X509Certificate.
   *
   * @param cert The X509Certificate.
   * @return The key size in bits or -1 if the algorithm is not supported.
   */
  public static int getKeySize(X509Certificate cert) {
    PublicKey publicKey = cert.getPublicKey();

    return switch (publicKey) {
      case RSAPublicKey rsaPublicKey ->
        // For RSA keys
          rsaPublicKey.getModulus().bitLength();
      case DSAPublicKey dsaPublicKey ->
        // For DSA keys
          dsaPublicKey.getParams().getP().bitLength();
      case ECPublicKey ecPublicKey ->
        // For EC keys (Elliptic Curve)
          ecPublicKey.getParams().getCurve().getField().getFieldSize();
      case null, default ->
        // Unsupported or unknown key type
          -1;
    };
  }

  /**
   * Writes a PEM formatted certificate to a file.
   *
   * @param cert     The X509Certificate to write.
   * @param filePath The file path where the PEM certificate will be written.
   * @throws CertificateConversionException If an error occurs during conversion or writing.
   */
  public static void writePemToFile(X509Certificate cert, String filePath)
      throws CertificateConversionException {
    try (FileWriter writer = new FileWriter(filePath)) {
      String pemCert = convertToPem(cert);
      writer.write(pemCert);
    } catch (IOException e) {
      throw new CertificateConversionException("Failed to write PEM certificate to file", e);
    }
  }

  /**
   * Private constructor to prevent instantiation.
   *
   * @see <a href="https://rules.sonarsource.com/java/RSPEC-1118">SonarLint java:S1118 rule</a>
   */
  private CertificateUtil() {
    throw new IllegalStateException("Utility class");
  }
}