/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.policy.key;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class KeyProviderDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( KeyProviderDiscovery.class );

  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( KeyProvider.class.isAssignableFrom( candidate ) &&
        Modifier.isPublic( candidate.getModifiers( ) ) &&
        !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        LOG.debug( "Registering policy condition key provider " + candidate.getCanonicalName( ) );
        if ( !Keys.registerKeyProvider( KeyProvider.class.cast( candidate.newInstance( ) ) ) ) {
          LOG.error( "Registration conflict for policy condition key provider " + candidate.getCanonicalName( ) );
        }
        return true;
      } catch ( final Exception e ) {
        LOG.error( "Error registering policy condition key provider: " + candidate, e );
      }
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
}
