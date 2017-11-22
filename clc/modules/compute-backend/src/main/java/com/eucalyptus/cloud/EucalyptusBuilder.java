/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
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
public class EucalyptusBuilder extends AbstractServiceBuilder<EucalyptusConfiguration> {
  static Logger               LOG           = Logger.getLogger( EucalyptusBuilder.class );

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
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {
  }
  
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException { }
  
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
	       LOG.fatal( "config.getFullName( )" + " : does not have a running database. Restarting process." );
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
            LOG.fatal( "MULTIPLE CONTROLLERS -- FAIL-STOP FOR UNSUPPORTED TOPOLOGY." );
            LOG.fatal( "MULTIPLE CONTROLLERS -- Shutting down secondary CLC." );
            System.exit( 1 );
          }
          
          if ( Topology.isEnabledLocally( Eucalyptus.class ) ) {
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
