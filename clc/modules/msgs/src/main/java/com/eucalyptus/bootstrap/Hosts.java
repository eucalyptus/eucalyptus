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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelException;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.View;
import org.jgroups.blocks.ReplicatedHashMap;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.bootstrap.Host.DbFilter;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.util.Internets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "bootstrap.hosts", description = "Properties controlling the handling of remote host bootstrapping" )
public class Hosts {
  @ConfigurableField( description = "Timeout for state transfers (in msec).", readonly = true )
  public static final Long                        STATE_TRANSFER_TIMEOUT = 10000L;
  private static final Logger                     LOG                    = Logger.getLogger( Hosts.class );
  private static ReplicatedHashMap<Address, Host> hostMap;
  private static Host                             localHostSingleton;
  
  enum HostBootstrapEventListener implements EventListener<Hertz> {
    INSTANCE;
    
    @Override
    public void fireEvent( Hertz event ) {
      Host maybeDirty = Hosts.localHost( ).checkDirty( );
      if ( Hosts.localHost( ).getTimestamp( ).before( maybeDirty.getTimestamp( ) ) ) {
        Hosts.localHost( maybeDirty );
        LOG.info( "Updating local host information: " + Hosts.localHost( ) );
        hostMap.replace( Hosts.localHost( ).getGroupsId( ), Hosts.localHost( ) );
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
  
  public static Address getLocalGroupAddress( ) {
    return HostManager.getInstance( ).getMembershipChannel( ).getAddress( );
  }
  
  static class HostManager {
    private final JChannel             membershipChannel;
    private final PhysicalAddress      physicalAddress;
    private static HostManager         singleton;
    private static final AtomicInteger epochSeen             = new AtomicInteger( 0 );
    private static final long          HOST_ADVERTISE_REMOTE = 15;
    private static final long          HOST_ADVERTISE_CLOUD  = 8;
    public static short                PROTOCOL_ID           = 513;
    public static short                HEADER_ID             = 1025;
    
    private HostManager( ) {
      this.membershipChannel = HostManager.buildChannel( );
      //TODO:GRZE:set socket factory for crypto
      try {
        LOG.info( "Starting membership channel... " );
        this.membershipChannel.connect( SystemIds.membershipGroupName( ) );
        HostManager.registerHeader( EpochHeader.class );
        this.physicalAddress = ( PhysicalAddress ) this.membershipChannel.downcall( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS,
                                                                                                           this.membershipChannel.getAddress( ) ) );
        LOG.info( "Started membership channel: " + SystemIds.membershipGroupName( ) );
      } catch ( ChannelException ex ) {
        LOG.fatal( ex, ex );
        throw BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
      }
    }
    
    public static short lookupRegisteredId( Class c ) {
      return ClassConfigurator.getMagicNumber( c );
    }
    
    private static synchronized <T extends Header> String registerHeader( Class<T> h ) {
      if ( ClassConfigurator.getMagicNumber( h ) == -1 ) {
        ClassConfigurator.add( ++HEADER_ID, h );
      }
      return "euca-" + ( h.isAnonymousClass( )
        ? h.getSuperclass( ).getSimpleName( ).toLowerCase( )
        : h.getSimpleName( ).toLowerCase( ) ) + "-header";
    }
    
    private static synchronized String registerProtocol( Protocol p ) {
      if ( ClassConfigurator.getProtocolId( p.getClass( ) ) == 0 ) {
        ClassConfigurator.addProtocol( ++PROTOCOL_ID, p.getClass( ) );
      }
      return "euca-" + ( p.getClass( ).isAnonymousClass( )
        ? p.getClass( ).getSuperclass( ).getSimpleName( ).toLowerCase( )
        : p.getClass( ).getSimpleName( ).toLowerCase( ) ) + "-protocol";
    }
    
    private static List<Protocol> getMembershipProtocolStack( ) {
      return Groovyness.run( "setup_membership.groovy" );
    }
    
    private static HostManager getInstance( ) {
      if ( singleton != null ) {
        return singleton;
      } else {
        synchronized ( HostManager.class ) {
          if ( singleton != null ) {
            return singleton;
          } else {
            singleton = new HostManager( );
            return singleton;
          }
        }
      }
    }
    
    private static JChannel buildChannel( ) {
      try {
        final JChannel channel = new JChannel( false );
        channel.setName( Internets.localHostIdentifier( ) );
        ProtocolStack stack = new ProtocolStack( );
        channel.setProtocolStack( stack );
        stack.addProtocols( HostManager.getMembershipProtocolStack( ) );
        stack.init( );
        return channel;
      } catch ( Exception ex ) {
        LOG.fatal( ex, ex );
        throw new RuntimeException( ex );
      }
    }
    
    public static JChannel getMembershipChannel( ) {
      return getInstance( ).membershipChannel;
    }
    
    public static class EpochHeader extends Header {
      private Integer value;
      
      public EpochHeader( ) {
        super( );
      }
      
      public EpochHeader( Integer value ) {
        super( );
        this.value = value;
      }
      
      @Override
      public void writeTo( DataOutputStream out ) throws IOException {
        out.writeInt( this.value );
      }
      
      @Override
      public void readFrom( DataInputStream in ) throws IOException, IllegalAccessException, InstantiationException {
        this.value = in.readInt( );
      }
      
      @Override
      public int size( ) {
        return Global.INT_SIZE;
      }
      
      public Integer getValue( ) {
        return this.value;
      }
    }
    
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.RemoteConfiguration )
  public static class HostMembershipBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      try {
        HostManager.getInstance( );
        LOG.info( "Started membership channel " + SystemIds.membershipGroupName( ) );
        hostMap = new ReplicatedHashMap<Address, Host>( HostManager.getInstance( ).getMembershipChannel( ) );
        hostMap.setDeadlockDetection( true );
        hostMap.setBlockingUpdates( true );
        hostMap.addNotifier( HostMapStateListener.INSTANCE );
        hostMap.start( STATE_TRANSFER_TIMEOUT );
        localHost( new Host( ) );
        LOG.info( "Setup localhost state: " + localHost( ) );
        hostMap.put( localHost( ).getGroupsId( ), localHost( ) );
        Listeners.register( HostBootstrapEventListener.INSTANCE );
        LOG.info( "Added localhost to system state: " + localHost( ) );
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
  
  public static int maxEpoch( ) {
    try {
      return Collections.max( Collections2.transform( hostMap.values( ), new Function<Host, Integer>( ) {
        
        @Override
        public Integer apply( Host arg0 ) {
          return arg0.getEpoch( );
        }
      } ) );
    } catch ( Exception ex ) {
      return 0;
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
  
  private static Host localHost( ) {
    return localHostSingleton;
  }
  
  private static Host localHost( Host newLocalHost ) {
    return localHostSingleton = newLocalHost;
  }
  
}
