package com.eucalyptus.component;
/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

import java.net.URI;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.config.LocalConfiguration;
import com.eucalyptus.config.RemoteConfiguration;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.NetworkUtil;

public abstract class DatabaseServiceBuilder<T extends ServiceConfiguration> extends AbstractServiceBuilder<T> {
  private static Logger LOG = Logger.getLogger( DatabaseServiceBuilder.class );
  protected abstract T newInstance( );
  
  protected abstract T newInstance( String name, String host, Integer port );
  
  @Override
  public List<T> list( ) throws ServiceRegistrationException {
    return ServiceConfigurations.getInstance( ).list( this.newInstance( ) );
  }
  
  @Override
  public T lookupByName( String name ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setName( name );
    return ( T ) ServiceConfigurations.getInstance( ).lookup( conf );
  }
  
  @Override
  public T lookupByHost( String hostName ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setHostName( hostName );
    return ( T ) ServiceConfigurations.getInstance( ).lookup( conf );
  }
  
  @Override
  public Boolean checkAdd( String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      if ( !NetworkUtil.testGoodAddress( host ) ) {
        throw new EucalyptusCloudException( "Components cannot be registered using local, link-local, or multicast addresses." );
      } else if ( NetworkUtil.testLocal( host ) && !this.getComponent( ).isLocal( ) ) {
        throw new EucalyptusCloudException( "You do not have a local " + this.newInstance( ).getClass( ).getSimpleName( ).replaceAll( "Configuration", "" )
                                            + " enabled (or it is not installed)." );
      }
    } catch ( EucalyptusCloudException e ) {
      throw new ServiceRegistrationException( e.getMessage( ), e );
    } catch ( Exception e ) {
      throw new ServiceRegistrationException( "Service registration failed: " + e.getMessage( ), e );
    }
    ServiceConfiguration existingName = null;
    try {
      existingName = this.lookupByName( name );
    } catch ( ServiceRegistrationException ex1 ) {
      LOG.trace( "Failed to find existing component registration for name: " + name );
    }
    ServiceConfiguration existingHost = null;
    try {
      existingHost = this.lookupByHost( host );
    } catch ( ServiceRegistrationException ex1 ) {
      LOG.trace( "Failed to find existing component registration for host: " + host );
    }
    if( existingName != null && existingHost != null ) {
      return false;
    } else if ( existingName == null && existingHost == null ) {
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
  public T add( String name, String host, Integer port ) throws ServiceRegistrationException {
    T config = this.newInstance( name, host, port );
    ServiceConfigurations.getInstance( ).store( config );
    return config;
  }

  @Override
  public ServiceConfiguration toConfiguration( URI uri ) throws ServiceRegistrationException {
    try {
      if( "vm".equals( uri.getScheme( ) ) || NetworkUtil.testLocal( uri.getHost( ) ) ) {
        return new LocalConfiguration( this.getComponent( ).getPeer( ), uri );      
      } else {
        return new RemoteConfiguration( this.getComponent( ).getPeer( ), uri );
      }
    } catch ( Exception e ) {
      return new LocalConfiguration( this.getComponent( ).getPeer( ), uri );
    }
  }

  @Override
  public T remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    T removeConf = this.lookupByName( config.getName( ) );
    ServiceConfigurations.getInstance( ).remove( removeConf );
    return removeConf;
  }
  
}
