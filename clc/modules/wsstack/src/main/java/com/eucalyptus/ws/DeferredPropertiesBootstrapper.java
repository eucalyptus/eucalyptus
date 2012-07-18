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

package com.eucalyptus.ws;

import java.util.List;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.MultiDatabasePropertyEntry;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.configurable.SingletonDatabasePropertyEntry;
import com.eucalyptus.configurable.StaticPropertyEntry;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.google.common.collect.Lists;

@Provides( ComponentId.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
public class DeferredPropertiesBootstrapper extends Bootstrapper.Simple {
  private static Logger LOG = Logger.getLogger( DeferredPropertiesBootstrapper.class );
  
  @Override
  public boolean load( ) throws Exception {
    Threads.lookup( Empyrean.class, DeferredPropertiesBootstrapper.class ).submit( new Runnable( ) {
      
      @Override
      public void run( ) {
        if ( Bootstrap.isShuttingDown( ) ) {
          return;
        } else {
          Bootstrap.awaitFinished( );
          List<ConfigurableProperty> staticProps = Lists.newArrayList( );
          for ( Entry<String, ConfigurableProperty> entry : PropertyDirectory.getPendingPropertyEntries( ) ) {
            ConfigurableProperty prop = entry.getValue( );
            if ( prop instanceof StaticPropertyEntry ) {
              staticProps.add( prop );
            } else {
              try {
                ComponentId compId = ComponentIds.lookup( prop.getEntrySetName( ) );
                for ( ServiceConfiguration s : ServiceConfigurations.list( compId.getClass( ) ) ) {
                  if ( compId.name( ).equals( prop.getEntrySetName( ) ) ) {
                    if ( prop instanceof SingletonDatabasePropertyEntry ) {
                      PropertyDirectory.addProperty( prop );
                    } else if ( prop instanceof MultiDatabasePropertyEntry ) {
                      PropertyDirectory.addProperty( ( ( MultiDatabasePropertyEntry ) prop ).getClone( s.getPartition( ) ) );
                    }
                  }
                }
              } catch ( Exception ex ) {
                LOG.error( ex, ex );
              }
            }
          }
          for ( ConfigurableProperty prop : staticProps ) {
            if ( PropertyDirectory.addProperty( prop ) ) {
              try {
                prop.getValue( );
              } catch ( Exception ex ) {
                Logs.extreme( ).error( ex, ex );
              }
            }
          }
        }
      }
    } );
    return true;
  }
}
