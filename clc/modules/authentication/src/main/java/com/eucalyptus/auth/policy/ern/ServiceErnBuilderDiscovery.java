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
package com.eucalyptus.auth.policy.ern;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class ServiceErnBuilderDiscovery extends ServiceJarDiscovery {

  private static Logger logger = Logger.getLogger( ServiceErnBuilderDiscovery.class );

  @Override
  public boolean processClass( final Class candidate ) {
    if ( ServiceErnBuilder.class.isAssignableFrom( candidate ) &&
        !Modifier.isAbstract( candidate.getModifiers() ) &&
        !Modifier.isInterface( candidate.getModifiers( ) ) &&
        !candidate.isLocalClass( ) &&
        !candidate.isAnonymousClass( ) ) {
      logger.debug( "Registering service ERN builder " + candidate.getName() );
      try {
        Ern.registerServiceErnBuilder( (ServiceErnBuilder) candidate.newInstance( ) );
      } catch ( InstantiationException | IllegalAccessException e ) {
        logger.error( "Registration failed for " + candidate.getName(), e );
      }
      return true;
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 1.0d;
  }

}
