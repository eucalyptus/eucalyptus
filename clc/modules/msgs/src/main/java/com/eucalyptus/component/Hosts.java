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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.View;
import com.eucalyptus.bootstrap.HostManager;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Mbeans;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

public class Hosts {
  private static final Logger                       LOG     = Logger.getLogger( Hosts.class );
  private static final ConcurrentMap<Address, Host> hostMap = new ConcurrentHashMap<Address, Host>( );

  public static <T> Collection<Host> collect( Predicate<Host> function ) {
    return Collections2.filter( hostMap.values( ), function );
  }

  public static <T> Collection<? extends T> collect( Function<Host,? extends T> function ) {
    return Collections2.transform( hostMap.values( ), function );
  }
  
  public static Host getHostByAddress( InetAddress addr ) {
    for ( Host h : hostMap.values( ) ) {
      if ( h.getHostAddresses( ).contains( addr ) ) {
        LOG.debug( "Found host info: addr=" + addr + " host=" + h );
        return h;
      }
    }
    throw Exceptions.debug( new NoSuchElementException( "Failed to lookup host for host address: " + addr ) );
  }
  
  public static Host getHostInstance( Address jgroupsId ) {
    Host h = hostMap.get( jgroupsId );
    if ( h == null ) {
      throw new NoSuchElementException( "Failed to lookup host for jgroups address: " + jgroupsId );
    } else {
      LOG.debug( "Current host info: " + h );
      return h;
    }
  }
  
  public static Host localHost( ) {
    if( !hostMap.containsKey( Hosts.localMembershipAddress( ) ) ) {
      Host temp = new Host( HostManager.getCurrentView( ).getViewId( ) );
      hostMap.putIfAbsent( Hosts.localMembershipAddress( ), temp );
      Host local = hostMap.get( Hosts.localMembershipAddress( ) );
      Mbeans.register( local );
      return local;
    } else {
      return hostMap.get( Hosts.localMembershipAddress( ) );
    }
  }
  
  public static Host updateHost( View currentView, Host updatedHost ) {
    synchronized ( Hosts.class ) {
      List<Address> currentMembers = Lists.newArrayList( currentView.getMembers( ) );
      Host entry = null;
      if ( hostMap.containsKey( updatedHost.getGroupsId( ) ) ) {
        entry = hostMap.get( updatedHost.getGroupsId( ) );
        entry.update( currentView.getViewId( ), updatedHost.hasDatabase( ), updatedHost.getHostAddresses( ) );
      } else {
        Component empyrean = Components.lookup( Empyrean.class );
        ComponentId empyreanId = empyrean.getComponentId( );
        for( InetAddress addr : updatedHost.getHostAddresses( ) ) {
          ServiceConfiguration ephemeralConfig = ServiceConfigurations.createEphemeral( empyrean, addr );
          if( !empyrean.hasService( ephemeralConfig ) ) {
            try {
              empyrean.initRemoteService( addr );
              empyrean.loadService( ephemeralConfig ).get();
              ServiceConfiguration config = empyrean.lookupService( ephemeralConfig ).getServiceConfiguration( );
              entry = new Host( currentView.getViewId( ), updatedHost.getGroupsId( ), updatedHost.hasDatabase( ), updatedHost.getHostAddresses( ), config );
              Mbeans.register( entry );
            } catch ( ServiceRegistrationException ex ) {
              LOG.error( ex , ex );
            } catch ( ExecutionException ex ) {
              LOG.error( ex , ex );
            } catch ( InterruptedException ex ) {
              LOG.error( ex , ex );
            }
          }
        }
        hostMap.put( entry.getGroupsId( ), entry );
      }
      /** determine hosts to remove in this view **/
//      List<Address> removeMembers = Lists.newArrayList( hostMap.keySet( ) );
//      if ( removeMembers.removeAll( currentMembers ) ) {
//        for ( Address addr : removeMembers ) {
//          Host removedHost = hostMap.remove( addr );
//          LOG.info( "Removing host: " + removedHost );
//        }
//      }
      LOG.debug( "Current host entries: " );
      for ( Host host : hostMap.values( ) ) {
        LOG.debug( "-> " + host );
      }
      return entry;
    }
  }
  
  public static Address localMembershipAddress( ) {
    return HostManager.getInstance( ).getMembershipChannel( ).getAddress( );
  }
}
