/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;


public class Loadbalancingv2MetadataException extends Exception {

  private static final long serialVersionUID = 1L;

  public Loadbalancingv2MetadataException(final String message) {
    super(message);
  }

  public Loadbalancingv2MetadataException(final String message, final Throwable cause) {
    super(message, cause);
  }
}