/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist;


public class LoadBalancingMetadataNotFoundException extends LoadBalancingMetadataException {
  private static final long serialVersionUID = 1L;

  public LoadBalancingMetadataNotFoundException(final String message) {
    super(message);
  }

  public LoadBalancingMetadataNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
