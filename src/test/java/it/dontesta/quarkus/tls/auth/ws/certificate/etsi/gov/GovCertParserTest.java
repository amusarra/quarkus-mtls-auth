/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.certificate.etsi.gov;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@QuarkusTest
class GovCertParserTest {

  @Mock
  Logger log;

  @InjectMocks
  GovCertParser govCertParser;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void parseValidCertificate() {
    String validXml = "<Certificates><X509Certificate>MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...</X509Certificate></Certificates>";
    govCertParser.parseAndSaveCerts(validXml);
    // Add assertions to verify the certificate was parsed and saved correctly
  }

  @Test
  void parseInvalidCertificate() {
    String invalidXml = "<Certificates><X509Certificate>INVALID_CERTIFICATE</X509Certificate></Certificates>";
    govCertParser.parseAndSaveCerts(invalidXml);
    // Add assertions to verify the error handling for invalid certificate
  }

  @Test
  void parseCertificateWithMissingFields() {
    String missingFieldsXml = "<Certificates></Certificates>";
    govCertParser.parseAndSaveCerts(missingFieldsXml);
    // Add assertions to verify the handling of missing fields
  }

  @Test
  void parseCertificateWithInvalidSignature() {
    String invalidSignatureXml = "<Certificates><X509Certificate>MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...INVALID</X509Certificate></Certificates>";
    govCertParser.parseAndSaveCerts(invalidSignatureXml);
    // Add assertions to verify the handling of invalid signature
  }

  @Test
  void parseCertificateWithExpiredDate() {
    String expiredDateXml = "<Certificates><X509Certificate>MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...EXPIRED</X509Certificate></Certificates>";
    govCertParser.parseAndSaveCerts(expiredDateXml);
    // Add assertions to verify the handling of expired certificate
  }
}