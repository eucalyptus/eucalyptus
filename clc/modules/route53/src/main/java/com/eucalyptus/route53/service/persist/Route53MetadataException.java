/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist;

/**
 *
 */
public class Route53MetadataException extends Exception {

  private static final long serialVersionUID = 1L;

  public Route53MetadataException(final String message) {
    super(message);
  }

  public Route53MetadataException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
