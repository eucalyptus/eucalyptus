/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
