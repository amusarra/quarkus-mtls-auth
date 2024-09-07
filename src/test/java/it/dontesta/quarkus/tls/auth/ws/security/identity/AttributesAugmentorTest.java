/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.security.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AttributesAugmentorTest {

  private AttributesAugmentor augmentor;

  @BeforeEach
  void setUp() {
    Logger mockLogger = mock(Logger.class);
    augmentor = new AttributesAugmentor(mockLogger);
  }

  @Test
  void testAugment() {
    SecurityIdentity mockIdentity = mock(SecurityIdentity.class);
    CertificateCredential mockCredential = mock(CertificateCredential.class);
    X509Certificate mockCertificate = mock(X509Certificate.class);
    Principal mockPrincipal = mock(Principal.class);

    when(mockIdentity.getCredential(CertificateCredential.class)).thenReturn(mockCredential);
    when(mockCredential.getCertificate()).thenReturn(mockCertificate);
    when(mockCertificate.getExtensionValue(AttributesAugmentor.OID_DEVICE_ID))
        .thenReturn(new byte[]{0x04, 0x0B, 0x0C, 0x09, 'D', 'e', 'v', 'i', 'c', 'e', 'I', 'd', '=', '1', '2', '3'});
    when(mockIdentity.getPrincipal()).thenReturn(mockPrincipal);

    Uni<SecurityIdentity> result = augmentor.augment(mockIdentity, null);
    SecurityIdentity augmentedIdentity = result.await().indefinitely();

    Map<String, Object> attributes = augmentedIdentity.getAttributes();
    assertEquals("", attributes.get("deviceId"));
  }

  @Test
  void testExtractAttributesFromCertificate() {
    X509Certificate mockCertificate = mock(X509Certificate.class);
    when(mockCertificate.getExtensionValue(AttributesAugmentor.OID_DEVICE_ID))
        .thenReturn(new byte[]{0x04, 0x0B, 0x0C, 0x09, 'D', 'e', 'v', 'i', 'c', 'e', 'I', 'd', '=', '1', '2', '3'});

    Map<String, String> attributes = augmentor.extractAttributesFromCertificate(mockCertificate);
    assertEquals("", attributes.get("deviceId"));
  }
}