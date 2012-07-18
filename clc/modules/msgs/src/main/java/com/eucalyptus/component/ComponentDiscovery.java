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

package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;

public class ComponentDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( ComponentDiscovery.class );
  
  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( ComponentId.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
         && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
      try {
        EventRecord.here( ComponentDiscovery.class, EventType.BOOTSTRAP_INIT_COMPONENT, candidate.getCanonicalName( ) ).info( );
        final Class<? extends ComponentId> idClass = candidate;
        ComponentIds.lookup( idClass );
      } catch ( final Throwable ex ) {
        LOG.error( ex, ex );
        LOG.info( "Error occurred while trying to register ComponentId of type: " + ex.getMessage( ), ex );
      }
      return true;
    } else {
      return false;
    }
  }
  
  @Override
  public Double getPriority( ) {
    return -1.0d;
  }
  
}
