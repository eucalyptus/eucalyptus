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
import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
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
import org.logicalcobwebs.proxool.ProxoolFacade;
import com.eucalyptus.bootstrap.Host.DbFilter;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceTransitions;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
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
  public static final Long                       STATE_TRANSFER_TIMEOUT = 10000L;
  private static final Logger                    LOG                    = Logger.getLogger( Hosts.class );
  private static ReplicatedHashMap<String, Host> hostMap;
  
  enum ShouldInitialize implements Predicate<ServiceConfiguration> {
    INSTANCE;
    @Override
    public boolean apply( ServiceConfiguration input ) {
      return !BootstrapArgs.isCloudController( ) && Internets.testLocal( input.getInetAddress( ) );
    }
  }
  
  enum HostBootstrapEventListener implements EventListener<Hertz> {
    INSTANCE;
    
    @Override
    public void fireEvent( Hertz event ) {
      Host maybeDirty = Hosts.localHost( ).checkDirty( );
      if ( Hosts.localHost( ).getTimestamp( ).before( new Date( maybeDirty.getLastTime( ) ) ) ) {
        hostMap.replace( maybeDirty.getDisplayName( ), maybeDirty );
        LOG.info( "Updated local host information: " + localHost( ) );
      }
    }
  }
  
  enum HostMapStateListener implements ReplicatedHashMap.Notification<String, Host> {
    INSTANCE;
    
    private String printMap( ) {
      return "\n" + Joiner.on( "\n=> " ).join( hostMap.values( ) );
    }
    
    @Override
    public void contentsCleared( ) {
      LOG.info( "Hosts.contentsCleared(): " + printMap( ) );
    }
    
    @Override
    public void contentsSet( Map<String, Host> arg0 ) {
      LOG.info( "Hosts.contentsSet(): " + printMap( ) );
    }
    
    @Override
    public void entryRemoved( String arg0 ) {
      LOG.info( "Hosts.entryRemoved(): " + arg0 );
      LOG.info( "Hosts.entryRemoved(): " + hostMap.keySet( ) );
    }
    
    @Override
    public void entrySet( String arg0, Host arg1 ) {
      LOG.info( "Hosts.entryAdded(): " + arg0 + " => " + arg1 );
      LOG.info( "Hosts.entryAdded(): " + hostMap.keySet( ) );
      if ( !arg1.isLocalHost( ) ) {
        setup( Empyrean.class, arg1.getBindAddress( ) );
        if ( arg1.hasDatabase( ) ) {
          setup( Eucalyptus.class, arg1.getBindAddress( ) );
        } else {
          try {
            ServiceConfiguration maybeConfig = ServiceConfigurations.lookupByHost( Eucalyptus.class, arg1.getBindAddress( ).getCanonicalHostName( ) );
            if ( ShouldInitialize.INSTANCE.apply( maybeConfig ) ) {
              arg1.markDatabase( );
              hostMap.replace( arg1.getDisplayName( ), arg1 );
              LOG.info( "Hosts.entryAdded(): Marked as database => " + arg1 );
            }
          } catch ( Exception ex ) {
            LOG.error( ex , ex );
          }
        }
      } else if ( arg1.hasDatabase( ) && !BootstrapArgs.isCloudController( ) ) {
        try {
          Bootstrap.initializeSystem( );
          System.exit( 123 );
        } catch ( Exception ex ) {
          System.exit( 123 );
        }
      }
    }
    
    @Override
    public void viewChange( View currentView, Vector<Address> joinMembers, Vector<Address> partMembers ) {
      LOG.trace( "Hosts.viewChange(): new view => " + Joiner.on( ", " ).join( currentView.getMembers( ) ) );
      if ( !joinMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): joined   => " + Joiner.on( ", " ).join( joinMembers ) );
      if ( !partMembers.isEmpty( ) ) LOG.info( "Hosts.viewChange(): parted   => " + Joiner.on( ", " ).join( partMembers ) );
      for ( Host h : Hosts.list( ) ) {
        if ( Iterables.contains( partMembers, h.getGroupsId( ) ) ) {
          hostMap.remove( h.getDisplayName( ) );
          teardown( Empyrean.class, h.getBindAddress( ) );
          if ( h.hasDatabase( ) ) {
            teardown( Eucalyptus.class, h.getBindAddress( ) );
          }
          LOG.info( "Hosts.viewChange(): -> removed  => " + h );
        }
      }
    }
    
  }
  
  public static Address getLocalGroupAddress( ) {
    return HostManager.getInstance( ).getMembershipChannel( ).getAddress( );
  }
  
  private static boolean setup( Class<? extends ComponentId> compClass, InetAddress addr ) {
    if ( !Internets.testLocal( addr ) && !Internets.testReachability( addr ) ) {
      LOG.warn( "Failed to reach host for cloud controller: " + addr );
      return false;
    } else {
      try {
        setupServiceState( compClass, addr );
        for ( ComponentId c : Iterables.filter( ComponentIds.list( ), ComponentIds.lookup( compClass ).isRelated( ) ) ) {
          try {
            setupServiceState( c.getClass( ), addr );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      }
      return true;
    }
  }
  
  private static boolean teardown( Class<? extends ComponentId> compClass, InetAddress addr ) {
    if ( !Internets.testLocal( addr ) && !Internets.testReachability( addr ) ) {
      LOG.warn( "Failed to reach host for cloud controller: " + addr );
      return false;
    } else {
      try {
        for ( ComponentId c : Iterables.filter( ComponentIds.list( ), ComponentIds.lookup( compClass ).isRelated( ) ) ) {
          try {
            final ServiceConfiguration dependsConfig = ServiceConfigurations.lookupByName( compClass, addr.getHostAddress( ) );
            Topology.stop( dependsConfig );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      }
      return true;
    }
  }
  
  private static void setupServiceState( Class<? extends ComponentId> compClass, InetAddress addr ) throws ServiceRegistrationException, ExecutionException {
    try {
      Component comp = Components.lookup( compClass );
      ServiceConfiguration config = ( Internets.testLocal( addr ) )
        ? comp.initRemoteService( addr )
        : comp.initRemoteService( addr );//TODO:GRZE:REVIEW: use of initRemote
      if ( Component.State.INITIALIZED.ordinal( ) >= config.lookupState( ).ordinal( ) ) {
        ServiceTransitions.pathTo( config, Component.State.DISABLED ).get( );
      }
      Topology.enable( config );
    } catch ( InterruptedException ex ) {
      Thread.currentThread( ).interrupt( );
    }
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
        hostMap = new ReplicatedHashMap<String, Host>( HostManager.getInstance( ).getMembershipChannel( ) );
        hostMap.setDeadlockDetection( true );
        hostMap.setBlockingUpdates( true );
        hostMap.addNotifier( HostMapStateListener.INSTANCE );
        hostMap.start( STATE_TRANSFER_TIMEOUT );
        Listeners.register( HostBootstrapEventListener.INSTANCE );
        Host local = new Host( );
        hostMap.put( local.getDisplayName( ), local );
        LOG.info( "Added localhost to system state: " + localHost( ) );
        LOG.info( "System view:\n" + HostMapStateListener.INSTANCE.printMap( ) );
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
  
  public static Host localHost( ) {
    return hostMap.get( Internets.localHostIdentifier( ) );
  }
  
}
