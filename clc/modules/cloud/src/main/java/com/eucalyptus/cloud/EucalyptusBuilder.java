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

package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.ComponentPart;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.google.common.base.Predicate;

@ComponentPart( Eucalyptus.class )
@Handles( { RegisterEucalyptusType.class,
           DeregisterEucalyptusType.class,
           DescribeEucalyptusType.class,
           ModifyEucalyptusAttributeType.class } )
public class EucalyptusBuilder extends AbstractServiceBuilder<EucalyptusConfiguration> {
  static Logger               LOG           = Logger.getLogger( EucalyptusBuilder.class );
  private static final String jdbcJmxDomain = "net.sf.hajdbc";
  
  @Override
  public EucalyptusConfiguration newInstance( ) {
    return new EucalyptusConfiguration( );
  }
  
  @Override
  public EucalyptusConfiguration newInstance( String partition, String name, String host, Integer port ) {
    try {
      InetAddress.getByName( host );
      return new EucalyptusConfiguration( host, host );
    } catch ( UnknownHostException e ) {
      return new EucalyptusConfiguration( Internets.localHostAddress( ), Internets.localHostAddress( ) );
    }
  }
  
  @Override
  public ComponentId getComponentId( ) {
    return Eucalyptus.INSTANCE;
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( !config.isVmLocal( ) ) {
      for ( Host h : Hosts.list( ) ) {
        if ( h.getHostAddresses( ).contains( config.getInetAddress( ) ) ) {
          EventRecord.here( EucalyptusBuilder.class, EventType.COMPONENT_SERVICE_ENABLED, config.toString( ) ).info( );
          return;
        }
      }
      throw Faults.failure( config, Exceptions.error( "There is no host in the system (yet) for the given cloud controller configuration: "
        + config.getFullName( )
        + ".\nHosts are: "
        + Hosts.list( ) ) );
    } else if ( config.isVmLocal( ) && !Hosts.isCoordinator( ) ) {
      throw Faults.failure( config, Exceptions.error( "This cloud controller "
        + config.getFullName( )
        + " is not currently the coordinator "
        + Hosts.list( ) ) );
    }
  }
  
  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {
    Host coordinator = Hosts.getCoordinator( );
    if ( coordinator == null ) {
      throw Faults.failure( config, Exceptions.error( config.getFullName( ) + ":fireCheck(): failed to lookup coordinator (" + coordinator + ")." ) );
    } else if ( coordinator.isLocalHost( ) ) {
      Check.COORDINATOR.apply( config );
    } else if ( !coordinator.isLocalHost( ) ) {
      Check.SECONDARY.apply( config );
    }
  }
  
  enum Check implements Predicate<ServiceConfiguration> {
    COORDINATOR {
      
      @Override
      public boolean apply( ServiceConfiguration config ) {  
	  if ( !Databases.isRunning( ) ) {
	       LOG.fatal( "config.getFullName( )" + " : does not have a running database. Restarting process to force re-synchronization." );
	       System.exit(123);
	  }
	  return true;
      }
    },
    SECONDARY {
      
      @Override
      public boolean apply( ServiceConfiguration config ) {
        if ( config.isVmLocal( ) ) {  
   
          if ( BootstrapArgs.isCloudController( ) ) {
              if ( !Databases.isRunning( ) ) {
 	           LOG.fatal( "config.getFullName( )" + " : does not have a running database. Restarting process to force re-synchronization." );
 	           System.exit(123);
              }
 	  }
          
          if ( !Databases.isSynchronized( ) ) {
            throw Faults.failure( config,
                                  Exceptions.error( config.getFullName( )
                                    + ":fireCheck(): eucalyptus service " + config.getFullName( ) + " is currently synchronizing: "
                                    + Hosts.getCoordinator( ) ) );
          } else if ( Topology.isEnabledLocally( Eucalyptus.class ) ) {
            throw Faults.failure( config,
                                  Exceptions.error( config.getFullName( )
                                    + ":fireCheck(): eucalyptus service " + config.getFullName( ) + " cant be enabled when it is not the coordinator: "
                                    + Hosts.getCoordinator( ) ) );
          } else {
            LOG.debug( config.getFullName( ) + ":fireCheck() completed with coordinator currently: " + Hosts.getCoordinator( ) );
          }
        }
        return true;
      }
    };
    
    @Override
    public abstract boolean apply( ServiceConfiguration input );
  }
}
