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
package com.eucalyptus.portal.provider;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.portal.common.provider.TagProvider;

/**
 *
 */
public class TagProviderDiscovery extends ServiceJarDiscovery {

  private static final Logger LOG = Logger.getLogger( TagProviderDiscovery.class );

  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( TagProvider.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        final TagProvider provider = TagProvider.class.cast( candidate.newInstance( ) );
        LOG.info( "Registering tag provider for " + provider.getVendor( ) );
        if ( !TagProviders.register( provider ) ) {
          LOG.warn( "Duplicate tag provider for " + provider.getVendor( ) );
        }
      } catch( final Exception e ) {
        LOG.error( "Error registering tag provider " + candidate, e );
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
