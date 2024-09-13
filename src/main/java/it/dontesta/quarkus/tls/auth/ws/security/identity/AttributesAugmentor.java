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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

/**
 * Augments a SecurityIdentity with attributes extracted from a certificate extension.
 *
 * <p>This class implements the {@link SecurityIdentityAugmentor} interface to extract attributes
 * from a certificate extension and add them to the attributes of the SecurityIdentity.
 * The attributes are extracted from the certificate extension with the OID {@link #OID_DEVICE_ID}.
 * The extension value is expected to be an ASN.1 UTF8String with the format
 * "DeviceId=deviceId".
 *
 * @author Antonio Musarra
 * @see SecurityIdentityAugmentor
 * @see CertificateUtil
 * @see CertificateCredential
 * @see X509Certificate
 * @see QuarkusSecurityIdentity
 */
@ApplicationScoped
public class AttributesAugmentor implements SecurityIdentityAugmentor {

  @Inject
  public AttributesAugmentor(Logger log) {
    this.log = log;
  }

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity,
                                       AuthenticationRequestContext context) {

    log.debug(
        "Augmenting SecurityIdentity with attributes extracted from extension with OID: "
        + OID_DEVICE_ID);

    return Uni.createFrom().item(build(identity));
  }

  /**
   * Extracts the attributes from the certificate extension.
   *
   * <p>The attributes are extracted from the certificate extension with the OID {@link #OID_DEVICE_ID}.
   * The extension value is expected to be an ASN.1 UTF8String with the format
   * "DeviceId=deviceId".
   * </p>
   *
   * @param certificate the certificate from which to extract the attributes
   * @return a map of attributes extracted from the certificate extension
   */
  protected Map<String, String> extractAttributesFromCertificate(X509Certificate certificate) {
    Map<String, String> attributes = new HashMap<>();

    try {
      byte[] deviceIdFromCert = certificate.getExtensionValue(OID_DEVICE_ID);

      if (deviceIdFromCert != null) {
        String deviceId = CertificateUtil.decodeExtensionValue(deviceIdFromCert);
        log.debug("Decoded Device ID from certificate: " + deviceId);

        if (deviceId != null) {
          // Remove the prefix "DeviceId="
          deviceId = deviceId.replace(OID_DEVICE_ID_PREFIX, "");
          attributes.put("deviceId", deviceId);
        }

      }
    } catch (Exception ex) {
      log.error("Occurred an error during attributes extraction from certificate", ex);

      throw new SecurityException(
          "Occurred an error during attributes extraction from certificate");
    }

    return attributes;
  }

  private Supplier<SecurityIdentity> build(SecurityIdentity identity) {
    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);

    CertificateCredential certificate = identity.getCredential(CertificateCredential.class);

    if (certificate != null) {
      Map<String, String> attributes =
          extractAttributesFromCertificate(certificate.getCertificate());
      attributes.forEach(builder::addAttribute);
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
  public static final String OID_DEVICE_ID = "1.3.6.1.4.1.99999.2";

  public static final String OID_DEVICE_ID_PREFIX = "DeviceId=";

  private final Logger log;
}

