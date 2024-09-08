/*
 * Copyright (c) 2024 Antonio Musarra's Blog.
 * SPDX-License-Identifier: MIT
 */

package it.dontesta.quarkus.tls.auth.ws.error.mapper;

/**
 * The ErrorResponse record represents the error response returned by the SecurityExceptionMapper.
 *
 * <p>This record is used to encapsulate the error response returned by the SecurityExceptionMapper.
 *
 * @author Antonio Musarra
 */
public record SecurityErrorResponse(int statusCode, String message) {
}