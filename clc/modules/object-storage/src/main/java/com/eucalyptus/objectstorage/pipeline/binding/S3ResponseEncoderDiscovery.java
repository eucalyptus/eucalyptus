/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.objectstorage.pipeline.binding;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class S3ResponseEncoderDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( S3ResponseEncoderDiscovery.class );

  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( S3ResponseEncoder.class.isAssignableFrom( candidate ) &&
        Modifier.isPublic( candidate.getModifiers( ) ) &&
        !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
      try {
        LOG.debug( "Registering s3 response encoder " + candidate.getCanonicalName( ) );
        S3ResponseEncoders.register( S3ResponseEncoder.class.cast( candidate.newInstance( ) ) );
        return true;
      } catch ( final Exception e ) {
        LOG.error( "Error registering s3 response encoder: " + candidate, e );
      }
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
}