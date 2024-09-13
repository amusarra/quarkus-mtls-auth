/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.security.identity;

import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import it.dontesta.quarkus.tls.auth.ws.utils.CertificateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Augments a SecurityIdentity with roles extracted from a certificate extension.
 *
 * <p>This class implements the {@link SecurityIdentityAugmentor} interface to extract roles from a
 * certificate extension and add them to the roles of the SecurityIdentity.
 * The roles are extracted from the certificate extension with the OID {@link #OID_ROLES}.
 * The extension value is expected to be an ASN.1 UTF8String with the format
 * "Role=role1,role2,role3".
 *
 * @author Antonio Musarra
 * @see SecurityIdentityAugmentor
 * @see CertificateUtil
 * @see CertificateCredential
 * @see X509Certificate
 * @see QuarkusSecurityIdentity
 */
@ApplicationScoped
public class RolesAugmentor implements SecurityIdentityAugmentor {

  @Inject
  public RolesAugmentor(Logger log) {
    this.log = log;
  }

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity,
                                       AuthenticationRequestContext context) {

    log.debug(
        "Augmenting SecurityIdentity with roles extracted from certificate with OID: "
        + OID_ROLES);

    return Uni.createFrom().item(build(identity));
  }

  /**
   * Extracts roles from the given X509 certificate.
   *
   * <p>This method retrieves the extension value from the certificate using the specified OID,
   * decodes the extension value, and extracts the roles encoded within it.
   *
   * @param certificate the X509 certificate from which to extract roles
   * @return a set of roles extracted from the certificate
   */
  protected Set<String> extractRolesFromCertificate(X509Certificate certificate) {
    Set<String> roles = new HashSet<>();

    try {
      // Retrieve the extension value from the certificate
      byte[] roleOidBytesFromCert = certificate.getExtensionValue(OID_ROLES);

      if (roleOidBytesFromCert != null) {
        // Decode the extension value
        String decodedRoles = CertificateUtil.decodeExtensionValue(roleOidBytesFromCert);

        if (decodedRoles != null) {
          log.debug("Decoded roles from certificate: " + decodedRoles);

          // Verify that decodedRoles matches the expected pattern
          if (decodedRoles.matches("^Role=([A-Za-z]+(?:,[A-Za-z]+)*+)$")) {
            // Add the roles to the set
            roles.addAll(Arrays.stream(decodedRoles.split("=")[1].split(","))
                .map(String::trim)
                .collect(Collectors.toSet()));
          } else {
            log.warn("Decoded roles do not match the expected pattern: %s".formatted(decodedRoles));

            throw new SecurityException(
                "Decoded roles do not match the expected pattern: %s".formatted(decodedRoles));
          }
        }
      }
    } catch (Exception ex) {
      log.error("Occurred an error during roles extraction from certificate", ex);

      throw new SecurityException(
          "Occurred an error during roles extraction from certificate");
    }

    return roles;
  }

  /**
   * Builds a new SecurityIdentity by copying the principal, attributes, credentials, and roles
   * from the original identity and adding roles extracted from the certificate.
   *
   * @param identity the original SecurityIdentity
   * @return a Supplier that provides the new SecurityIdentity
   */
  private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
    // create a new builder and copy principal, attributes, credentials and roles
    // from the original identity
    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

    CertificateCredential certificate = identity.getCredential(CertificateCredential.class);

    if (certificate != null) {
      builder.addRoles(extractRolesFromCertificate(certificate.getCertificate()));
    }

    return builder::build;
  }

  /**
   * OID for extracting roles from the certificate.
   *
   * <p>The roles are encoded as an ASN.1 UTF8String extension with the following format:
   * <pre>
   *     1.3.6.1.4.1.12345.1 = ASN1:UTF8String:Role=${ext_cert_role}
   *   </pre>
   *
   * <p>The value of the extension is a comma-separated list of roles.
   * For example, the extension value "Role=role1,role2,role3" would result in the roles "role1",
   * "role2", and "role3" being extracted.
   *
   * <p>You can see the custom extensions in the ssl_extensions.cnf file
   * located in the src/main/shell/certs-manager directory.
   */
  public static final String OID_ROLES = "1.3.6.1.4.1.99999.1";

  private final Logger log;
}