/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal;

/**
 * Internal metadata access exception.
 */
public class ComputeMetadataNotFoundException extends ComputeMetadataException {
  private static final long serialVersionUID = 1L;

  public ComputeMetadataNotFoundException(final String message ) {
    super( message );
  }

  public ComputeMetadataNotFoundException(final String message, final Throwable cause ) {
    super( message, cause );
  }
}
