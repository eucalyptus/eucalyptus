/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
