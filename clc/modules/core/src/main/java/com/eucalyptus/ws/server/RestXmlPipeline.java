/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.ws.server;

import java.util.EnumSet;
import java.util.Set;
import com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import com.eucalyptus.ws.util.HmacUtils.SignatureVersion;
import com.google.common.collect.ImmutableSet;

/**
 *
 */
public abstract class RestXmlPipeline extends RestPipeline {

  protected RestXmlPipeline( final String name,
                             final String servicePathPrefix,
                             final Set<TemporaryKeyType> allowedTemporaryCredentials ) {
    this( name,
        ImmutableSet.of( servicePathPrefix ),
        allowedTemporaryCredentials );
  }

  protected RestXmlPipeline( final String name,
                             final String servicePathPrefix,
                            final Set<TemporaryKeyType> allowedTemporaryCredentials,
                             final Set<SignatureVersion> allowedSignatureVersions ) {
    super( name,
        ImmutableSet.of( servicePathPrefix ),
        allowedTemporaryCredentials,
        allowedSignatureVersions );
  }

  protected RestXmlPipeline( final String name,
                             final Set<String> servicePathPrefixes,
                             final Set<TemporaryKeyType> allowedTemporaryCredentials ) {
    super(
        name,
        servicePathPrefixes,
        allowedTemporaryCredentials,
        EnumSet.allOf( SignatureVersion.class ) );
  }
}
