/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.error.mapper;

/**
 * The ErrorResponse class represents the error response returned by the SecurityExceptionMapper.
 *
 * <p>This class is used to encapsulate the error response returned by the SecurityExceptionMapper.
 *
 * @author Antonio Musarra
 */
public class SecurityErrorResponse {
  private int statusCode;
  private String message;

  public SecurityErrorResponse(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}