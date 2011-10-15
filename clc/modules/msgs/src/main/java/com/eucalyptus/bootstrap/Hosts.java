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

package com.eucalyptus.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.blocks.ReplicatedHashMap;
import com.eucalyptus.bootstrap.Host.DbFilter;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "bootstrap.hosts", description = "Properties controlling the handling of remote host bootstrapping" )
public class Hosts {
  @ConfigurableField( description = "Timeout for state transfers (in msec).", readonly = true )
  public static final Long                        STATE_TRANSFER_TIMEOUT = 10000L;
  private static final Logger                     LOG                    = Logger.getLogger( Hosts.class );
  private static ReplicatedHashMap<Address, Host> hostMap;
  private static Host                             localHost;
  
  enum HostBootstrapEventListener implements EventListener<Hertz> {
    INSTANCE;
    
    @Override
    public void fireEvent( Hertz event ) {
      if ( Hosts.localHost.isDirty( ) ) {
        LOG.info( "Updating local host information: " + Hosts.localHost );
        hostMap.replace( Hosts.localHost.getGroupsId( ), localHost );
      }
    }
  }
  
  enum HostMapStateListener implements ReplicatedHashMap.Notification<Address, Host> {
    INSTANCE;
    
    private String printMap( ) {
      return "\n" + Joiner.on( "\n=> " ).join( hostMap.values( ) );
    }
    
    @Override
    public void contentsCleared( ) {
      LOG.info( "Hosts.contentsCleared(): " + printMap( ) );
    }
    
    @Override
    public void contentsSet( Map<Address, Host> arg0 ) {
      LOG.info( "Hosts.contentsSet(): " + printMap( ) );
    }
    
    @Override
    public void entryRemoved( Address arg0 ) {
      LOG.info( "Hosts.entryRemoved(): " + arg0 );
      LOG.info( "Hosts.entryRemoved(): " + printMap( ) );
    }
    
    @Override
    public void entrySet( Address arg0, Host arg1 ) {
      LOG.info( "Hosts.entryAdded(): " + arg0 + " => " + arg1 );
      LOG.info( "Hosts.entryAdded(): " + printMap( ) );
    }
    
    @Override
    public void viewChange( View arg0, Vector<Address> arg1, Vector<Address> arg2 ) {
      LOG.info( "Hosts.viewChange(): new view => " + Joiner.on( ", " ).join( arg0.getMembers( ) ) );
      LOG.info( "Hosts.viewChange(): joined   => " + Joiner.on( ", " ).join( arg1 ) );
      LOG.info( "Hosts.viewChange(): parted   => " + Joiner.on( ", " ).join( arg2 ) );
    }
    
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteConfiguration )
  public static class HostMembershipBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      try {
        HostManager.getInstance( );
        LOG.info( "Started membership channel " + HostManager.getMembershipGroupName( ) );
        hostMap = new ReplicatedHashMap<Address, Host>( HostManager.getInstance( ).getMembershipChannel( ) );
        hostMap.setDeadlockDetection( true );
        hostMap.setBlockingUpdates( true );
        hostMap.addNotifier( HostMapStateListener.INSTANCE );
        hostMap.start( STATE_TRANSFER_TIMEOUT );
        localHost = new Host( HostManager.getInstance( ).getMembershipChannel( ).getAddress( ) );
        LOG.info( "Setup localhost state: " + localHost );
        hostMap.put( localHost.getGroupsId( ), localHost );
        Listeners.register( HostBootstrapEventListener.INSTANCE );
        LOG.info( "Added localhost to system state: " + localHost );
        LOG.info( "System view:\n" + Joiner.on( "\n=> " ).join( hostMap.values( ) ) );
        if ( !BootstrapArgs.isCloudController( ) ) {
          while ( Hosts.listDatabases( ).isEmpty( ) ) {
            TimeUnit.SECONDS.sleep( 5 );
            LOG.info( "Waiting for system view with database..." );
          }
        } else {
          //TODO:GRZE:handle check and merge of db here!!!!
        }
        LOG.info( "Membership address for localhost: " + Hosts.localHost( ) );
        return true;
      } catch ( final Exception ex ) {
        LOG.fatal( ex, ex );
        BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
        return false;
      }
    }
  }
  
  public static List<Host> list( ) {
    Predicate<Host> trueFilter = Predicates.alwaysTrue( );
    return Hosts.list( trueFilter );
  }
  
  public static List<Host> list( Predicate<Host> filter ) {
    return Lists.newArrayList( Iterables.filter( hostMap.values( ), filter ) );
  }
  
  public static List<Host> listDatabases( ) {
    return Lists.newArrayList( Iterables.filter( Hosts.list( ), DbFilter.INSTANCE ) );
  }
  
  public static boolean contains( Address jgroupsId ) {
    return hostMap.containsKey( jgroupsId );
  }
  
  public static Host lookup( Address jgroupsId ) {
    Host h = hostMap.get( jgroupsId );
    if ( h == null ) {
      throw new NoSuchElementException( "Failed to lookup host for jgroups address: " + jgroupsId );
    } else {
      LOG.debug( "Current host info: " + h );
      return h;
    }
  }
  
  public static Host localHost( ) {
    return localHost;
  }
  
}
