/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.util.concurrent.Futures;

public class ComponentRegistrationHandler {
  private static Logger LOG = Logger.getLogger( ComponentRegistrationHandler.class );
  
  public static boolean register( final ComponentId compId, String partitionName, String name, String hostName, Integer port ) throws ServiceRegistrationException {
    if ( !compId.isRegisterable( ) ) {
      throw new ServiceRegistrationException( "Failed to register component: " + compId.getFullName( ) + " does not support registration." );
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
      LOG.error( "Inavlid hostname: " + hostName + " failure: " + ex1.getMessage( ), ex1 );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": registration failed because the hostname " + hostName + " is invalid: "
                                              + ex1.getMessage( ), ex1 );
    }
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) + " for: " + partition + "." + name + "@" + hostName + ":" + port );
    if ( !builder.checkAdd( partition, name, hostName, port ) ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": checkAdd failed." );
      return false;
    }
    
    try {
      final ServiceConfiguration newComponent = builder.newInstance( partition, name, hostName, port );
      if ( newComponent.getComponentId( ).isPartitioned( ) ) {
        Partition part = Partitions.lookup( newComponent );
        part.syncKeysToDisk( );
        Partition p = Partitions.lookup( newComponent );
        Logs.exhaust( ).info( p.getCertificate( ) );
        Logs.exhaust( ).info( p.getNodeCertificate( ) );
      }
      ServiceConfigurations.store( newComponent );
      try {
        Components.lookup( newComponent ).loadService( newComponent );
        Topology.enable( newComponent ).get( );
      } catch ( Exception ex ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": enable failed because of: " + ex.getMessage( ) );
      }
      return true;
    } catch ( Exception e ) {
      e = Exceptions.filterStackTrace( e );
      LOG.info( builder.getClass( ).getSimpleName( ) + ": registration failed because of: " + e.getMessage( ) );
      LOG.error( e, e );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": registration failed with message: " + e.getMessage( ), e );
    }
  }
  
  public static boolean deregister( final ComponentId compId, String name ) throws ServiceRegistrationException, EucalyptusCloudException {
    final ServiceBuilder<?> builder = ServiceBuilders.lookup( compId );
    LOG.info( "Using builder: " + builder.getClass( ).getSimpleName( ) );
    try {
      if ( !checkRemove( builder, name ) ) {
        LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
        throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove returned false.  " +
                                                "It is unsafe to currently deregister, please check the logs for additional information." );
      }
    } catch ( Exception e ) {
      LOG.info( builder.getClass( ).getSimpleName( ) + ": checkRemove failed." );
      throw new ServiceRegistrationException( builder.getClass( ).getSimpleName( ) + ": checkRemove failed with message: " + e.getMessage( ), e );
    }
    final ServiceConfiguration conf = ServiceConfigurations.lookupByName( compId.getClass( ), name );
    Topology.destroy( conf );
    ServiceConfigurations.remove( conf );
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
    } catch ( PersistenceException e ) {
      throw Exceptions.toUndeclared( e );
    } catch ( Exception e ) {
      Logs.extreme( ).error( e, e );
      return true;
    }
  }
  
}
