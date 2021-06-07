/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import javax.persistence.PersistenceException;

import com.eucalyptus.component.groups.ServiceGroups;
import com.google.common.base.Predicate;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.JdkFutureAdapters;

public class ComponentRegistrationHandler {
  private static Logger LOG = Logger.getLogger( ComponentRegistrationHandler.class );

  enum HostNamePredicate implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply( String hostName ) {
      InetAddress addr;
      try {
        addr = InetAddress.getByName( hostName );
        return true;
      } catch ( UnknownHostException ex1 ) {
        LOG.error( "Invalid hostname: " + hostName
                   + " failure: "
                   + ex1.getMessage(), ex1 );
        throw Exceptions.toUndeclared( "Registration failed because the hostname "
                                       + hostName
                                       + " is invalid: "
                                       + ex1.getMessage(), ex1 );
      }
    }

    public static Predicate<String> current = INSTANCE;
    public static Predicate<String> get() { return current; };
  }

  public static ServiceConfiguration register( final ComponentId compId,
                                               final String partitionName,
                                               final String name,
                                               final String hostName,
                                               final Integer port ) throws ServiceRegistrationException {
    ServiceConfiguration returnConfig = null;
    if ( !compId.isRegisterable( ) ) {
      throw new ServiceRegistrationException( "Failed to register component: " + compId.getFullName( )
                                              + " does not support registration." );
    }
    final ServiceBuilder builder = ServiceBuilders.lookup( compId );
    String partition = partitionName;

    if ( !ServiceGroups.listMembership( compId ).isEmpty( ) ) {
      partition = partitionName;
    } else if ( !compId.isPartitioned( ) ) {
      partition = name;
    } else if ( !compId.isPartitioned( ) && compId.isCloudLocal( ) ) {
      partition = Components.lookup( Eucalyptus.class ).getComponentId( ).name( );
    } else if ( partition == null ) {
      LOG.error( "BUG: Provided partition is null.  Using the service name as the partition name for the time being." );
      partition = name;
    }
    HostNamePredicate.get().apply( hostName );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( )
              + " for: "
              + partition
              + "."
              + name
              + "@"
              + hostName
              + ":"
              + port );
    if ( builder.checkUpdate( partition, name, hostName, port ) ) {
      final ServiceConfiguration configuration =
          ServiceConfigurations.lookupByName( builder.getComponentId( ).getClass( ), name );
      ServiceConfigurations.update( configuration, hostName, port );
      ServiceConfigurations.store( configuration );
      Components.updateConfiguration( ).apply( configuration );
      LOG.info( String.format( "Updated registration information to: %s.%s@%s:%d"
          , partition
          , name
          , hostName
          , port ) );
      return configuration;
    }
    if ( !builder.checkAdd( partition, name, hostName, port ) ) {
      LOG.info( "Returning existing registration information for: "
                + partition
                + "."
                + name
                + "@"
                + hostName
                + ":"
                + port );
      return ServiceConfigurations.lookupByName( builder.getComponentId( ).getClass( ), name );
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
      returnConfig = ServiceConfigurations.store( newComponent );
      try {
        Components.lookup( newComponent ).setup( newComponent );
        Future<ServiceConfiguration> res = Topology.start( newComponent );
        JdkFutureAdapters.listenInPoolThread( res ).addListener( new Runnable( ) {
        
          @Override
          public void run( ) {
            try {
              Topology.enable( newComponent );
            } catch ( Exception ex ) {
              LOG.info( builder.getClass( ).getSimpleName( ) + ": enable failed because of: "
                        + ex.getMessage( ) );
            }
          }
        }, MoreExecutors.directExecutor( ) );
      } catch ( Exception ex ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": load failed because of: "
                  + ex.getMessage( ) );
      }
      return returnConfig;
    } catch ( Exception e ) {
      e = Exceptions.filterStackTrace( e );
      LOG.info( builder.getClass( ).getSimpleName( ) + ": registration failed because of: "
                + e.getMessage( ) );
      LOG.error( e, e );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": registration failed with message: "
                                              + e.getMessage( ), e );
    }
  }

  public static ServiceConfiguration deregister( final ComponentId compId, String name ) throws ServiceRegistrationException, EucalyptusCloudException {
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
    ServiceConfiguration conf = null;
    try {
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
        return conf;
      } else {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": deregistration failed because of" + e.getMessage( ) );
        throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": deregistration failed because of: " + e.getMessage( ), e );
      }
    }
    return conf;
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
