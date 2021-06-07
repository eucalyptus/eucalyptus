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

import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Faults.CheckException;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {
  @Override
  public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {}

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException, CheckException {}

  private static Logger LOG = Logger.getLogger( AbstractServiceBuilder.class );

  @Override
  public boolean checkAdd(
      final String partition,
      final String name,
      final String host,
      final Integer port
  ) throws ServiceRegistrationException {
    try {
      if ( !Internets.testGoodAddress( host ) ) {
        throw new EucalyptusCloudException( "Components cannot be registered using local, link-local, or multicast addresses." );
      }
    } catch (final EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    } catch ( final Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    ServiceConfiguration existingName = null;
    try {
      existingName = ServiceConfigurations.lookupByName( this.getComponentId( ).getClass( ), name );
    } catch ( final NoSuchElementException e ) {
      LOG.trace( "Failed to find existing component registration for name: " + name );
    } catch ( final PersistenceException e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    final Partition partitionAnnotation = Ats.from( this.getComponentId( ) ).get( Partition.class );
    boolean manyToOne = partitionAnnotation != null && partitionAnnotation.manyToOne( );
    ServiceConfiguration existingHost = null;
    try {
      existingHost = ServiceConfigurations.lookupByHost( this.getComponentId( ).getClass( ), host );
    } catch ( final NoSuchElementException e ) {
      LOG.trace( "Failed to find existing component registration for host: " + name );
    } catch ( final PersistenceException e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    /**
     * @grze check here if we have an existing identical registration and return false in this case
     *       -- caller should handle it differently than an exception
     */
    if ( existingName != null && existingHost != null ) {
      ServiceConfiguration maybeIdenticalConfig = existingName;
      if ( existingName.equals( existingHost )
           && maybeIdenticalConfig.getName( ).equals( name )
           && (!maybeIdenticalConfig.getComponentId( ).isPartitioned( ) ||
               maybeIdenticalConfig.getPartition( ).equals( partition ) )
           && maybeIdenticalConfig.getHostName( ).equals( host )
           && maybeIdenticalConfig.getPort( ).equals( port ) ) {
        return false;
      }
    }
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
    } else {
      if ( ServiceConfigurations.listPartition( this.getComponentId( ).getClass( ), partition ).size( ) >= 1 ) {
        throw new ServiceRegistrationException( "Unable to register more than one service in a partition for component type: "
            + this.getComponentId( ).getName( ) );
      }
    }
    if ( existingName == null && ( existingHost == null || manyToOne ) ) {
      return true;
    } else if ( existingName != null ) {
      throw new ServiceRegistrationException( "Component with name=" + name + " already exists with host=" + existingName.getHostName( ) );
    } else if ( existingHost != null ) {
      throw new ServiceRegistrationException( "Component with host=" + host + " already exists with name=" + existingHost.getName( ) );
    } else {
      throw new ServiceRegistrationException( "BUG: This is a logical impossibility." );
    }
  }

  @Override
  public boolean checkUpdate( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      final ServiceConfiguration configuration =
          ServiceConfigurations.lookupByName( this.getComponentId( ).getClass( ), name );

      if ( !configuration.getPort( ).equals( port ) ||
           !configuration.getHostName( ).equals( host ) ) {
        // validate update request
        if ( !Internets.testGoodAddress( host ) ) {
          throw new ServiceRegistrationException( "Components cannot use local, link-local, or multicast addresses." );
        }

        if ( configuration.getComponentId( ).isPartitioned( ) &&
            configuration.getPartition( ) != null &&
            !configuration.getPartition( ).equals( partition ) ) {
          throw new ServiceRegistrationException( "Partition update not supported, please re-register component." );
        }

        return true;
      }
    } catch ( final ServiceRegistrationException e ) {
      throw e;
    } catch ( final NoSuchElementException ex1 ) {
      LOG.trace( "Failed to find existing component registration for name: " + name );
    } catch ( final Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    return false;
  }

}
