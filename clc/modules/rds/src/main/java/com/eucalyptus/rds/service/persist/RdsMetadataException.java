/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

/**
 *
 */
public class RdsMetadataException extends Exception {
  private static final long serialVersionUID = 1L;

  public RdsMetadataException( final String message ) {
    super( message );
  }

  public RdsMetadataException( final String message, final Throwable cause ) {
    super( message, cause );
  }
}
