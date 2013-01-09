/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tags;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 * Performs discovery and registration of FilterSupport instances.
 */
public class FilterDiscovery extends ServiceJarDiscovery {
  private final Logger logger = Logger.getLogger( FilterDiscovery.class );

  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    boolean accepted = false;
    if ( FilterSupport.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers() ) ) {
      try {
        FilterSupport.registerFilterSupport( (FilterSupport) candidate.newInstance() );
        accepted = true;
      } catch ( Exception e ) {
        logger.error( e, e );
      }
    }
    return accepted;
  }

  @Override
  public Double getPriority() {
    return 0.3d;
  }
}
