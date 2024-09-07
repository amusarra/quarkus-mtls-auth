/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.exception;

/**
 * Exception thrown when an error occurs during certificate conversion.
 *
 * @author Antonio Musarra
 */
public class CertificateConversionException extends RuntimeException {

  public CertificateConversionException(String message, Throwable cause) {
    super(message, cause);
  }

  public CertificateConversionException(String message) {
    super(message);
  }
}