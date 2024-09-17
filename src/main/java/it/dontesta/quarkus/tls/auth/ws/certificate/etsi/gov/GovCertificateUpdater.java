/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.certificate.etsi.gov;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Update the certificates from the URL <a href="https://eidas.agid.gov.it/TL/TSL-IT.xml">TSL-IT</a>.
 * The certificates are used to validate the identity of a person or organization.
 * The certificates are recognized at the national level.
 *
 * @author Antonio Musarra
 */
@ApplicationScoped
public class GovCertificateUpdater {

  @ConfigProperty(name = "gov.trust.certs.url")
  String certsUrl;

  @Inject
  public GovCertificateUpdater(GovCertificateParser certParser, Logger log) {
    this.certParser = certParser;
    this.log = log;
  }

  /**
   * This scheduled method downloads the certificates from the URL
   * <a href="https://eidas.agid.gov.it/TL/TSL-IT.xml">TSL-IT.xml</a> and uses the
   * {@link GovCertificateParser} to parse and save the certificates (single PEM files)
   * and save them as a PEM bundle.
   *
   * @see GovCertificateParser#parseAndSaveCerts(String)
   * @see GovCertificateParser#saveCertificatesAsPem(Path, Path)
   */
  @Scheduled(every = "{gov.trust.certs.tsl.update.period}", delayed = "{gov.trust.certs.tsl.update.initial.delay}")
  public void updateCertificates() {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(certsUrl))
        .build();

    HttpResponse<String> response = null;

    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        // Parse and save the certificates
        certParser.parseAndSaveCerts(response.body());

        // Save the certificates as a PEM bundle
        certParser.saveCertificatesAsPem(Path.of(certParser.getOutputPath()),
            Path.of(certParser.getOutputPathPemBundle()));
      } else {
        log.warn("Failed to update certificates. Status code: %d".formatted(response.statusCode()));
      }
    } catch (IOException | InterruptedException e) {
      log.error("Error updating certificates: %s".formatted(e.getMessage()));
    }
  }

  private final Logger log;

  private final GovCertificateParser certParser;
}
