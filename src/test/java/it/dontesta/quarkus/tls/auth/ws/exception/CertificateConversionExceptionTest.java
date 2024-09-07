/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.exception;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CertificateConversionExceptionTest {

  @Test
  void testExceptionWithMessage() {
    String message = "Conversion error";
    CertificateConversionException exception = new CertificateConversionException(message);
    assertEquals(message, exception.getMessage());
  }

  @Test
  void testExceptionWithMessageAndCause() {
    String message = "Conversion error";
    Throwable cause = new Throwable("Root cause");
    CertificateConversionException exception = new CertificateConversionException(message, cause);
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }
}