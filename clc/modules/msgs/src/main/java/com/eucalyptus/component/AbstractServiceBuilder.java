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

import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId.Partition;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {
  private static Logger LOG = Logger.getLogger( AbstractServiceBuilder.class );
  
  @Override
  public Boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      if ( !Internets.testGoodAddress( host ) ) {
        throw new EucalyptusCloudException( "Components cannot be registered using local, link-local, or multicast addresses." );
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    ServiceConfiguration existingName = null;
    try {
      existingName = ServiceConfigurations.lookupByName( this.getComponentId( ).getClass( ), name );
    } catch ( NoSuchElementException ex1 ) {
      LOG.trace( "Failed to find existing component registration for name: " + name );
    } catch ( PersistenceException ex1 ) {
      LOG.trace( "Failed to find existing component registration for name: " + name );
    }
    Partition partitionAnnotation = Ats.from( this.getComponentId( ) ).get( Partition.class );
    boolean manyToOne = partitionAnnotation != null && partitionAnnotation.manyToOne( );
    ServiceConfiguration existingHost = null;
    if ( !manyToOne ) {
      if ( this.getComponentId( ).isPartitioned( ) ) {
        if ( ServiceConfigurations.listPartition( this.getComponentId( ).getClass( ), partition ).size( ) >= 2 ) {
          throw new ServiceRegistrationException( "Unable to register more than two services in a partition for component type: "
                                                  + this.getComponentId( ).getName( ) );
        }
      } else {
        if ( ServiceConfigurations.list( this.getComponentId( ).getClass( ) ).size( ) >= 2 ) {
          throw new ServiceRegistrationException( "Unable to register more than two services in a partition for component type: "
                                                  + this.getComponentId( ).getName( ) );
        }
      }
      try {
        existingHost = ServiceConfigurations.lookupByHost( this.getComponentId( ).getClass( ), host );
      } catch ( NoSuchElementException ex1 ) {
        LOG.trace( "Failed to find existing component registration for host: " + name );
      } catch ( PersistenceException ex1 ) {
        LOG.trace( "Failed to find existing component registration for host: " + host );
      }
    } else {
      if ( ServiceConfigurations.listPartition( this.getComponentId( ).getClass( ), partition ).size( ) >= 1 ) {
        throw new ServiceRegistrationException( "Unable to register more than one service in a partition for component type: "
                                                + this.getComponentId( ).getName( ) );
      }
    }
    /**
     * @grze check here if we have an existing identical registration and return false in this case
     *       -- caller should handle it differently than an exception
     */
    if ( existingName != null && existingHost != null ) {
      ServiceConfiguration maybeIdenticalConfig = existingName;
      if ( existingName.equals( existingHost )
           && maybeIdenticalConfig.getName( ).equals( name )
           && maybeIdenticalConfig.getPartition( ).equals( partition )
           && maybeIdenticalConfig.getHostName( ).equals( host )
           && maybeIdenticalConfig.getPort( ).equals( port ) ) {
        return false;
      }
    }
    if ( existingName == null && existingHost == null ) {
      return true;
    } else if ( existingName != null ) {
      throw new ServiceRegistrationException( "Component with name=" + name + " already exists with host=" + existingName.getHostName( ) );
    } else if ( existingHost != null ) {
      throw new ServiceRegistrationException( "Component with host=" + host + " already exists with name=" + existingHost.getName( ) );
    } else {
      throw new ServiceRegistrationException( "BUG: This is a logical impossibility." );
    }
  }
  
}
