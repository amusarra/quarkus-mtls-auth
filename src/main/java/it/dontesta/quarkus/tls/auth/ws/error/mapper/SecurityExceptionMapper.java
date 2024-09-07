/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.error.mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * The ErrorResponse class represents the error response returned by the SecurityExceptionMapper.
 *
 * <p>This class is used to encapsulate the error response returned by the SecurityExceptionMapper.
 *
 * @author Antonio Musarra
 */
@Provider
public class SecurityExceptionMapper implements ExceptionMapper<SecurityException> {

  @Override
  public Response toResponse(SecurityException exception) {
    SecurityErrorResponse errorResponse = new SecurityErrorResponse(
        Response.Status.UNAUTHORIZED.getStatusCode(),
        exception.getMessage()
    );

    return Response.status(Response.Status.UNAUTHORIZED)
        .entity(errorResponse)
        .build();
  }
}