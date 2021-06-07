/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.exceptions.s3;

/**
 *
 */
public class AccessDeniedOnCreateException extends AccessDeniedException {
  public AccessDeniedOnCreateException( final String resource ) {
    super( resource, "Access denied due to permissions or limit exceeded" );
  }
}
