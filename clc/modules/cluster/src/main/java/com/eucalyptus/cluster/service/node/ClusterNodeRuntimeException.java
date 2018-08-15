/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
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
