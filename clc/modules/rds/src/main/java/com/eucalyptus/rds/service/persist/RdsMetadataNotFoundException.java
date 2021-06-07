/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.persist;

/**
 *
 */
public class RdsMetadataNotFoundException extends RdsMetadataException {
  private static final long serialVersionUID = 1L;

  public RdsMetadataNotFoundException( final String message ) {
    super( message );
  }

  public RdsMetadataNotFoundException( final String message, final Throwable cause ) {
    super( message, cause );
  }
}
