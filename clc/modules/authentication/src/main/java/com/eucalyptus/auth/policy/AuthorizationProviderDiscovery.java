/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.policy;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class AuthorizationProviderDiscovery extends ServiceJarDiscovery {

  private static final Logger logger = Logger.getLogger( AuthorizationProviderDiscovery.class );

  @Override
  public Double getPriority() {
    return 1.0d;
  }

  @Override
  public boolean processClass( final Class candidate ) {
    if ( AuthorizationProvider.class.isAssignableFrom( candidate ) &&
        !Modifier.isAbstract( candidate.getModifiers() ) &&
        Modifier.isPublic( candidate.getModifiers( ) ) ) {
      try {
        final AuthorizationProvider provider = (AuthorizationProvider) candidate.newInstance( );
        AuthorizationProviders.register( provider );
      } catch ( InstantiationException | IllegalAccessException e ) {
        logger.error( "Error registering authorization provider class: " + candidate, e );
      }
      return true;
    }
    return false;
  }
}
