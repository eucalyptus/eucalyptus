/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;


public class Loadbalancingv2MetadataNotFoundException extends Loadbalancingv2MetadataException {

  private static final long serialVersionUID = 1L;

  public Loadbalancingv2MetadataNotFoundException(final String message) {
    super(message);
  }

  public Loadbalancingv2MetadataNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }
}