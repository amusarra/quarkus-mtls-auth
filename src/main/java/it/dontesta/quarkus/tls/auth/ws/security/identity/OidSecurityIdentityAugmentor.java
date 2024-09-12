/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.security.identity;

import io.quarkus.security.credential.CertificateCredential;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.smallrye.mutiny.Uni;
import it.dontesta.quarkus.tls.auth.ws.utils.CertificateUtil;
import it.dontesta.quarkus.tls.auth.ws.utils.DeviceIdUtil;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.cert.X509Certificate;

/**
 * Augments a SecurityIdentity to verify the DeviceId extracted from a certificate extension.
 *
 * <p>This class implements the {@link SecurityIdentityAugmentor} interface to extract the DeviceId
 * from a certificate extension and verify it. The DeviceId is extracted from the certificate
 * extension with the OID {@link AttributesAugmentor#OID_DEVICE_ID}. The extension value is expected
 * to be an ASN.1 UTF8String with the format "DeviceId=deviceId".
 *
 * <p>The DeviceId is verified by decoding the Base64 encoded string, splitting it into its
 * components, regenerating the HMAC from the combined string, and comparing the provided HMAC with
 * the calculated HMAC.
 *
 * <p>If the DeviceId is valid, the SecurityIdentity is augmented with the DeviceId attribute.
 * Otherwise, the request is failed with a {@link SecurityException}.
 *
 * @author Antonio Musarra
 * @see SecurityIdentityAugmentor
 * @see CertificateUtil
 * @see CertificateCredential
 * @see X509Certificate
 * @see DeviceIdUtil
 * @see SecurityIdentity
 * @see AuthenticationRequestContext
 */
@ApplicationScoped
public class OidSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

  @Override
  public Uni<SecurityIdentity> augment(SecurityIdentity identity,
                                       AuthenticationRequestContext context) {
    CertificateCredential clientCert = identity.getCredential(CertificateCredential.class);

    if (clientCert != null) {
      X509Certificate cert = clientCert.getCertificate();
      byte[] oidValueFromCert = cert.getExtensionValue(AttributesAugmentor.OID_DEVICE_ID);

      if (oidValueFromCert == null) {
        throw new SecurityException(
            "Invalid certificate OID { %s } missing for DeviceId.".formatted(
                AttributesAugmentor.OID_DEVICE_ID));
      }

      String oidValue = CertificateUtil.decodeExtensionValue(oidValueFromCert)
          .replace(AttributesAugmentor.OID_DEVICE_ID_PREFIX, "").trim();

      // Verify that the DeviceId is valid
      if (!DeviceIdUtil.verifyDeviceId(oidValue)) {
        throw new SecurityException(
            "Invalid certificate OID value { %s } or OID { %s } missing for DeviceId.".formatted(
                oidValue, AttributesAugmentor.OID_DEVICE_ID));
      }
    } else {
      throw new SecurityException("Client certificate not found.");
    }

    return Uni.createFrom().item(identity);
  }

  @Override
  public int priority() {
    return 10;
  }
}