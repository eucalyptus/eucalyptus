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
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;

public abstract class AbstractServiceBuilder<T extends ServiceConfiguration> implements ServiceBuilder<T> {
  private static Logger LOG = Logger.getLogger( AbstractServiceBuilder.class );
  
  @Override
  public Boolean checkRemove( String partition, String name ) throws ServiceRegistrationException {
    try {
      this.lookupByName( name );
      return true;
    } catch ( ServiceRegistrationException e ) {
      throw e;
    } catch ( Throwable e ) {
      LOG.error( e, e );
      return false;
    }
  }
  
  @Override
  public List<T> list( ) throws ServiceRegistrationException {
    return ServiceConfigurations.list( this.newInstance( ) );
  }
  
  @Override
  public T lookup( String partition, String name ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setName( name );
    conf.setPartition( partition );
    return ( T ) ServiceConfigurations.lookup( conf );
  }
  
  @Override
  public T lookupByName( String name ) throws ServiceRegistrationException {//TODO:GRZE:RELEASE fix the name uniqueness checking here.
    T conf = this.newInstance( );
    conf.setName( name );
    return ( T ) ServiceConfigurations.lookup( conf );
  }
  
  @Override
  public T lookupByHost( String hostName ) throws ServiceRegistrationException {
    T conf = this.newInstance( );
    conf.setHostName( hostName );
    return ( T ) ServiceConfigurations.lookup( conf );
  }
  
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
    if ( existingName != null && existingHost != null ) {
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
  public T add( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    T config = this.newInstance( partition, name, host, port );
    return ( T ) ServiceConfigurations.store( config );
  }
  
  @Override
  public T remove( ServiceConfiguration config ) throws ServiceRegistrationException {
    T removeConf = this.lookupByName( config.getName( ) );
    return ( T ) ServiceConfigurations.remove( removeConf );
  }
  
}
