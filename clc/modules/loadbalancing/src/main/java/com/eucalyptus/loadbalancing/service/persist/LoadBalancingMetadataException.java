/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;


public class LoadBalancingMetadataException extends Exception {

  private static final long serialVersionUID = 1L;

  public LoadBalancingMetadataException(final String message) {
    super(message);
  }

  public LoadBalancingMetadataException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
