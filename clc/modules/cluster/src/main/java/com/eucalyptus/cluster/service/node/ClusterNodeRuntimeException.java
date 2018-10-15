/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cluster.service.node;


public class ClusterNodeRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ClusterNodeRuntimeException( final String message ) {
    super( message );
  }

  public ClusterNodeRuntimeException( final String message, final Throwable cause ) {
    super( message, cause );
  }
}
