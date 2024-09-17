/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.certificate.etsi.gov;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parse the XML content containing the certificates in Base64 format and save them as PEM files.
 * The XML content is fetched from the URL <a href="https://eidas.agid.gov.it/TL/TSL-IT.xml">TSL-IT</a>.
 * The certificates are extracted from the XML content and saved as PEM files.
 * The certificates are used to validate the identity of a person or organization.
 * The certificates are recognized at the national level.
 *
 * @author Antonio Musarra
 * @see <a href="https://eidas.agid.gov.it/TL/TSL-IT.xml">https://eidas.agid.gov.it/TL/TSL-IT.xml</a>
 * @see <a href="https://www.etsi.org/deliver/etsi_ts/119600_119699/119612/02.02.01_60/ts_119612v020201p.pdf">ETSI TS 119 612</a>
 */
@Singleton
public class GovCertificateParser {

  @ConfigProperty(name = "gov.trust.certs.pem.bundle.output.path")
  String outputPathPemBundle;

  @ConfigProperty(name = "gov.trust.certs.pem.bundle.file.name")
  String outputPemBundleFileName;

  /**
   * Init the outputPath for the PEM files. The outputPath is created as a temporary directory.
   *
   * @param log the logger.
   */
  @Inject
  public GovCertificateParser(Logger log) {
    this.log = log;
    try {
      outputPath = Files.createTempDirectory("gov-trust-certs");
      log.info("Gov Trust Certificates will be saved to: %s".formatted(outputPath.toString()));
    } catch (IOException e) {
      log.error("Could not create temp directory for certificates", e);
    }
  }

  /**
   * Get the output path for the PEM bundle.
   *
   * @return the output path for the PEM bundle.
   * @see #outputPathPemBundle
   */
  public String getOutputPathPemBundle() {
    return outputPathPemBundle;
  }

  /**
   * Get the output PEM bundle file name.
   *
   * @return the output PEM bundle file name.
   * @see #outputPemBundleFileName
   */
  public String getOutputPemBundleFileName() {
    return outputPemBundleFileName;
  }

  /**
   * Get the output path for the certificates.
   *
   * @return the output path for the certificates.
   * @see #outputPath
   */
  public String getOutputPath() {
    return outputPath.toString();
  }

  /**
   * Parse the XML content and save the certificates as PEM files.
   * The certificates are extracted from the XML content and saved as PEM files
   * in the output path specified in the configuration (@see #outputPath).
   *
   * @param xmlContent the XML content containing the certificates in Base64 format.
   */
  public void parseAndSaveCerts(String xmlContent) {
    try {
      // Clean the output path before parsing and saving the certificates
      cleanOutputPath();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

      DocumentBuilder builder = factory.newDocumentBuilder();
      InputStream xmlStream = new ByteArrayInputStream(xmlContent.getBytes());
      Document doc = builder.parse(xmlStream);
      doc.getDocumentElement().normalize();

      XPathFactory xPathFactory = XPathFactory.newInstance();
      XPath xPath = xPathFactory.newXPath();

      // XPath expression to get all ServiceInformation elements with the specified attributes
      // (ServiceTypeIdentifier and ServiceStatus). The ServiceTypeIdentifier
      // http://uri.etsi.org/TrstSvc/Svctype/IdV corresponds to the eIDAS IdV (Identity Validation
      // Service) service using to validate the identity of a person or organization.
      // The ServiceStatus in the TrustServiceStatusList (TSL) are defined in the ETSI TS 119 612.
      // The value http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel means
      // that the service is recognized at the national level.
      String expression =
          "//ServiceInformation[ServiceTypeIdentifier='http://uri.etsi.org/TrstSvc/Svctype/IdV' and ServiceStatus='http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel']";

      NodeList serviceTypeNodes =
          (NodeList) xPath.evaluate(expression, doc, XPathConstants.NODESET);

      // Iterate over the ServiceInformation elements and extract the X509Certificate elements
      // containing the certificates in Base64 format and save them as PEM files.
      IntStream.range(0, serviceTypeNodes.getLength())
          .mapToObj(serviceTypeNodes::item)
          .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
          .map(GovCertificateParser::apply)
          .map(serviceInfoElement -> serviceInfoElement.getElementsByTagName("X509Certificate")
              .item(0))
          .filter(certNode -> certNode != null && certNode.getNodeType() == Node.ELEMENT_NODE)
          .forEach(certNode -> saveCertificateAsPem(certNode.getTextContent().trim(), null));
    } catch (Exception e) {
      log.error("Error parsing XML", e);
    }
  }

  /**
   * Save the certificates as PEM bundle.
   *
   * @param inputDirectory the input directory containing the certificates in PEM format.
   * @param outputPath     the output path for the PEM bundle.
   */
  public void saveCertificatesAsPem(Path inputDirectory, Path outputPath) {
    try {
      List<PemObject> pemObjects = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDirectory, "*.pem")) {
        for (Path entry : stream) {
          try (PemReader pemReader = new PemReader(Files.newBufferedReader(entry))) {
            PemObject pemObject = pemReader.readPemObject();
            pemObjects.add(pemObject);
          }
        }
      }

      Path certPath = outputPath.resolve(getOutputPemBundleFileName());

      try (PemWriter pemWriter = new PemWriter(Files.newBufferedWriter(certPath))) {
        for (PemObject pemObject : pemObjects) {
          pemWriter.writeObject(pemObject);
        }
        log.info("Saved certificate bundle to {%s}".formatted(certPath));
      }

    } catch (IOException e) {
      log.error("Error saving certificate bundle as PEM", e);
    }
  }

  /**
   * Clean the output path.
   */
  public void cleanOutputPath() {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputPath)) {
      for (Path entry : stream) {
        Files.delete(entry);
      }
      log.debug("Cleaned output path: %s".formatted(outputPath.toString()));
    } catch (IOException e) {
      log.error("Error cleaning output path", e);
    }
  }

  /**
   * Save the certificate as PEM file.
   *
   * @param certBase64        the certificate in Base64 format.
   * @param outputPemFilePath the output path for the PEM file.
   */
  private void saveCertificateAsPem(String certBase64, String outputPemFilePath) {
    try {
      byte[] decodedCert = Base64.getDecoder().decode(certBase64);
      PemObject pemObject = new PemObject("CERTIFICATE", decodedCert);

      // Verify the certificate is not expired or not yet valid
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
      X509Certificate certificate =
          (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedCert));

      // Throws CertificateExpiredException if the certificate is expired
      certificate.checkValidity();

      String hash = String.format("%064x",
          new BigInteger(1,
              MessageDigest.getInstance("SHA-256").digest(certBase64.getBytes())));

      Path certPath =
          outputPemFilePath != null ? Path.of(outputPemFilePath).resolve(hash + ".pem") :
              outputPath.resolve(hash + ".pem");

      try (PemWriter pemWriter = new PemWriter(Files.newBufferedWriter(certPath))) {
        pemWriter.writeObject(pemObject);
        log.debug("Saved certificate to {%s}".formatted(certPath));
      }

    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
      try {
        byte[] decodedCert = Base64.getDecoder().decode(certBase64);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate =
            (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(decodedCert));

        // Extract subject
        String subject = certificate.getSubjectX500Principal().getName();

        // Calculate fingerprint
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] certBytes = certificate.getEncoded();
        byte[] fingerprintBytes = md.digest(certBytes);
        StringBuilder fingerprint = new StringBuilder();
        for (byte b : fingerprintBytes) {
          fingerprint.append(String.format("%02X:", b));
        }
        if (fingerprint.length() > 0) {
          fingerprint.setLength(fingerprint.length() - 1); // Remove trailing colon
        }

        log.warn("Certificate is expired or not yet valid. Subject: {%s}, Fingerprint: {%s}"
            .formatted(subject, fingerprint.toString()));
      } catch (CertificateException | NoSuchAlgorithmException ex) {
        log.error("Error processing expired certificate", ex);
      }
    } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
      log.error("Error saving certificate as PEM", e);
    }
  }

  /**
   * Apply a cast to the Node to Element.
   *
   * @param node the Node to cast to Element.
   * @return the Element.
   */
  private static Element apply(Node node) {
    return (Element) node;
  }

  private final Logger log;
  private Path outputPath;
}
