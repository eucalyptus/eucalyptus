/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal;

/**
 * Internal metadata access exception.
 */
public class ComputeMetadataException extends Exception {
  private static final long serialVersionUID = 1L;

  public ComputeMetadataException( final String message ) {
    super( message );
  }

  public ComputeMetadataException( final String message, final Throwable cause ) {
    super( message, cause );
  }
}
