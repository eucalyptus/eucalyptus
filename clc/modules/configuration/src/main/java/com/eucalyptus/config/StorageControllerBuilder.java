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

package com.eucalyptus.config;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;


@ComponentPart( Storage.class )
@Handles( { RegisterStorageControllerType.class, DeregisterStorageControllerType.class, DescribeStorageControllersType.class, ModifyStorageControllerAttributeType.class } )
public class StorageControllerBuilder extends AbstractServiceBuilder<StorageControllerConfiguration> {
  private static Logger LOG = Logger.getLogger( StorageControllerBuilder.class );

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Storage.class );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( ) {
    return new StorageControllerConfiguration( );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( String partition, String name, String host, Integer port ) {
    return new StorageControllerConfiguration( partition, name, host, port );
  }
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      final Partition part = Partitions.lookup( this.newInstance( partition, name, host, port ) );
      part.syncKeysToDisk( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused storage controller registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
    }
    return super.checkAdd( partition, name, host, port );
  }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( config.isVmLocal( ) ) {
      java.lang.System.setProperty( "euca.storage.name", config.getName( ) );
      LOG.info( LogUtil.subheader( "Setting euca.storage.name=" + config.getName( ) + " for: " + LogUtil.dumpObject( config ) ) );
    }
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
