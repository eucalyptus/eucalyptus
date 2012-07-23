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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

public class ComponentRegistrationHandler {
  private static Logger LOG = Logger.getLogger( ComponentRegistrationHandler.class );
  
  public static boolean register( final ComponentId compId, String partitionName, String name, String hostName, Integer port ) throws ServiceRegistrationException {
    if ( !compId.isRegisterable( ) ) {
      throw new ServiceRegistrationException( "Failed to register component: " + compId.getFullName( )
                                              + " does not support registration." );
    }
    final ServiceBuilder builder = ServiceBuilders.lookup( compId );
    String partition = partitionName;
    
    if ( !compId.isPartitioned( ) ) {
      partition = name;
    } else if ( !compId.isPartitioned( ) && compId.isCloudLocal( ) ) {
      partition = Components.lookup( Eucalyptus.class ).getComponentId( ).name( );
    } else if ( partition == null ) {
      LOG.error( "BUG: Provided partition is null.  Using the service name as the partition name for the time being." );
      partition = name;
    }
    InetAddress addr;
    try {
      addr = InetAddress.getByName( hostName );
    } catch ( UnknownHostException ex1 ) {
      LOG.error( "Inavlid hostname: " + hostName
                 + " failure: "
                 + ex1.getMessage( ), ex1 );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": registration failed because the hostname "
                                              + hostName
                                              + " is invalid: "
                                              + ex1.getMessage( ), ex1 );
    }
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( )
              + " for: "
              + partition
              + "."
              + name
              + "@"
              + hostName
              + ":"
              + port );
    if ( !builder.checkAdd( partition, name, hostName, port ) ) {
      LOG.info( "Returning existing registration information for: "
                + partition
                + "."
                + name
                + "@"
                + hostName
                + ":"
                + port );
      return true;
    }
    
    try {
      final ServiceConfiguration newComponent = builder.newInstance( partition, name, hostName, port );
      if ( newComponent.getComponentId( ).isPartitioned( ) ) {
        Partition part = Partitions.lookup( newComponent );
        part.syncKeysToDisk( );
        Partition p = Partitions.lookup( newComponent );
        Logs.extreme( ).info( p.getCertificate( ) );
        Logs.extreme( ).info( p.getNodeCertificate( ) );
      }
      ServiceConfigurations.store( newComponent );
      try {
        Components.lookup( newComponent ).setup( newComponent );
        Future<ServiceConfiguration> res = Topology.start( newComponent );
        Futures.makeListenable( res ).addListener( new Runnable( ) {
          
          @Override
          public void run( ) {
            try {
              Topology.enable( newComponent );
            } catch ( Exception ex ) {
              LOG.info( builder.getClass( ).getSimpleName( ) + ": enable failed because of: "
                        + ex.getMessage( ) );
            }
          }
        }, MoreExecutors.sameThreadExecutor( ) );
      } catch ( Exception ex ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": load failed because of: "
                  + ex.getMessage( ) );
      }
      return true;
    } catch ( Exception e ) {
      e = Exceptions.filterStackTrace( e );
      LOG.info( builder.getClass( ).getSimpleName( ) + ": registration failed because of: "
                + e.getMessage( ) );
      LOG.error( e, e );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": registration failed with message: "
                                              + e.getMessage( ), e );
    }
  }
  
  public static boolean deregister( final ComponentId compId, String name ) throws ServiceRegistrationException, EucalyptusCloudException {
    final ServiceBuilder<?> builder = ServiceBuilders.lookup( compId );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    boolean proceedOnError = false;
    try {
      if ( !checkRemove( builder, name ) ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
        throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove returned false.  "
                                                +
                                                "It is unsafe to currently deregister, please check the logs for additional information." );
      }
    } catch ( final NoSuchElementException ex ) {
      LOG.info( "Silently proceeding with deregister for non-existant configuration" );
      proceedOnError = true;
    } catch ( Exception e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove failed with message: "
                                              + e.getMessage( ), e );
    }
    try {
      ServiceConfiguration conf;
      try {
        conf = ServiceConfigurations.lookupByName( compId.getClass( ), name );
      } catch ( NoSuchElementException ex1 ) {
        conf = Components.lookup( compId.getClass( ) ).lookup( name );
      }
      try {
        Topology.destroy( conf );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).debug( ex, ex );
      }
      try {
        ServiceConfigurations.remove( conf );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).debug( ex, ex );
      }
    } catch ( Exception e ) {
      if ( proceedOnError ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": deregistration error, but proceeding since config has been removed: " + e.getMessage( ) );
        return true;
      } else {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": deregistration failed because of" + e.getMessage( ) );
        throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": deregistration failed because of: " + e.getMessage( ), e );
      }
    }
    return true;
  }
  
  /**
   * @param builder
   * @param partition
   * @param name
   * @return
   */
  @SuppressWarnings( "rawtypes" )
  private static boolean checkRemove( ServiceBuilder builder, String name ) {
    try {
      ServiceConfiguration conf = builder.newInstance( );
      conf.setName( name );
      ServiceConfigurations.lookup( conf );
      return true;
    } catch ( final NoSuchElementException ex ) {
      throw ex;
    } catch ( PersistenceException e ) {
      throw Exceptions.toUndeclared( e );
    } catch ( Exception e ) {
      Logs.extreme( ).error( e, e );
      return true;
    }
  }
  
}
