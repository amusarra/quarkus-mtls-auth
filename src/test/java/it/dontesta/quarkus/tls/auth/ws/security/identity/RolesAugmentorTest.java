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
import java.util.Set;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RolesAugmentorTest {

  private RolesAugmentor augmentor;
  private Logger mockLogger;

  @BeforeEach
  void setUp() {
    mockLogger = mock(Logger.class);
    augmentor = new RolesAugmentor(mockLogger);
  }

  @Test
  void testAugment() {
    SecurityIdentity mockIdentity = mock(SecurityIdentity.class);
    CertificateCredential mockCredential = mock(CertificateCredential.class);
    X509Certificate mockCertificate = mock(X509Certificate.class);
    Principal mockPrincipal = mock(Principal.class);

    when(mockIdentity.getCredential(CertificateCredential.class)).thenReturn(mockCredential);
    when(mockCredential.getCertificate()).thenReturn(mockCertificate);
    when(mockCertificate.getExtensionValue(RolesAugmentor.OID_ROLES))
        .thenReturn(new byte[]{0x04, 0x0B, 0x0C, 0x09, 'R', 'o', 'l', 'e', '=', 'r', 'o', 'l', 'e', '1', ',', 'r', 'o', 'l', 'e', '2'});
    when(mockIdentity.getPrincipal()).thenReturn(mockPrincipal);

    Uni<SecurityIdentity> result = augmentor.augment(mockIdentity, null);
    SecurityIdentity augmentedIdentity = result.await().indefinitely();

    Set<String> roles = augmentedIdentity.getRoles();
    assertEquals(Set.of("role"), roles);
  }

  @Test
  void testExtractRolesFromCertificate() {
    X509Certificate mockCertificate = mock(X509Certificate.class);
    when(mockCertificate.getExtensionValue(RolesAugmentor.OID_ROLES))
        .thenReturn(new byte[]{0x04, 0x0B, 0x0C, 0x09, 'R', 'o', 'l', 'e', '=', 'r', 'o', 'l', 'e', '1', ',', 'r', 'o', 'l', 'e', '2'});

    Set<String> roles = augmentor.extractRolesFromCertificate(mockCertificate);
    assertEquals(Set.of("role"), roles);
  }
}