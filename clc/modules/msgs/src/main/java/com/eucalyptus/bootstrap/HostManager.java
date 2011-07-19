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

package com.eucalyptus.bootstrap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedMembershipListener;
import org.jgroups.Global;
import org.jgroups.Header;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.bootstrap.HostManager.HostStateListener;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Host;
import com.eucalyptus.component.Hosts;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Logs;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class HostManager {
  private static Logger              LOG                   = Logger.getLogger( HostManager.class );
  private final JChannel             membershipChannel;
  private final PhysicalAddress      physicalAddress;
  private final String               membershipGroupName;
  final CurrentView                  view;
  private HostStateListener          stateListener;
  private static HostManager         singleton;
  private static final AtomicInteger epochSeen             = new AtomicInteger( 0 );
  private static final long          HOST_ADVERTISE_REMOTE = 15;
  private static final long          HOST_ADVERTISE_CLOUD  = 8;
  
  private HostManager( ) {
    this.view = new CurrentView( );
    this.membershipChannel = HostManager.buildChannel( );
    HostStateListener listener = BootstrapArgs.isCloudController( )
      ? new CloudControllerHostStateHandler( )
      : new RemoteHostStateListener( );
    this.membershipChannel.setReceiver( listener );
    //TODO:GRZE:set socket factory for crypto
    this.membershipGroupName = SystemIds.membershipGroupName( );
    try {
      LOG.info( "Starting membership channel... " );
      this.membershipChannel.connect( this.membershipGroupName );
      this.setStateListener( listener );
      Protocols.registerHeader( EpochHeader.class );
      this.physicalAddress = ( PhysicalAddress ) this.membershipChannel.downcall( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS,
                                                                                                         this.membershipChannel.getAddress( ) ) );
      LOG.info( "Started membership channel: " + this.membershipGroupName );
    } catch ( ChannelException ex ) {
      LOG.fatal( ex, ex );
      throw BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
    }
  }
  
  private void setStateListener( HostStateListener stateListener ) {
    if ( this.stateListener != null && this.stateListener != stateListener ) {//yes i mean reference equality
      ListenerRegistry.getInstance( ).deregister( Hertz.class, HostManager.this.stateListener );
    }
    this.stateListener = stateListener;
    ListenerRegistry.getInstance( ).register( Hertz.class, HostManager.this.stateListener );
  }
  
  public static int getMaxSeenEpoch( ) {
    return HostManager.epochSeen.get( );
  }
  
  public static HostManager getInstance( ) {
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
  
  public static View getCurrentView( ) {
    return HostManager.getInstance( ).view.getCurrentView( );
  }
  
  public static Boolean isReady( ) {
    return HostManager.getInstance( ).view.isReady( );
  }
  
  enum InitState {
    PENDING, WORKING, FINISHED
  };
  
  abstract class HostStateListener implements Receiver, ExtendedMembershipListener, EventListener {
    private AtomicReference<InitState> initializing = new AtomicReference<InitState>( BootstrapArgs.isCloudController( )
                                                      ? InitState.FINISHED
                                                      : InitState.PENDING );
    
    @Override
    public final byte[] getState( ) {
      return null;
    }
    
    @Override
    public final void setState( byte[] state ) {}
    
    @Override
    public final void suspect( Address suspected_mbr ) {
      LOG.debug( suspected_mbr );
    }
    
    @Override
    public final void block( ) {
      LOG.debug( this.getClass( ) + ".block()" );
    }
    
    @Override
    public final void unblock( ) {
      LOG.debug( this.getClass( ) + ".unblock()" );
    }
    
    @Override
    public void receive( Message msg ) {
      if ( Hosts.localHost( ).getGroupsId( ).equals( msg.getSrc( ) ) ) {
        return;
      } else {
        try {
          this.onMessage( msg );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
    private void onMessage( Message msg ) {
      EpochHeader epochHeader = ( EpochHeader ) msg.getHeader( Protocols.lookupRegisteredId( EpochHeader.class ) );
      Integer senderEpoch = epochHeader.getValue( );
      int myEpoch = HostManager.epochSeen.get( );
      if ( myEpoch < senderEpoch ) {
        HostManager.epochSeen.compareAndSet( myEpoch, senderEpoch );
      }
      switch ( this.initializing.get( ) ) {
        case PENDING:
          if ( msg.getObject( ) instanceof InitRequest ) {
            if ( this.initializing.compareAndSet( InitState.PENDING, InitState.WORKING ) ) {
              LOG.debug( "Received initialize message: " + msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
              try {
                this.initialize( msg.getObject( ) instanceof Initialize );
              } finally {
                this.initializing.set( InitState.FINISHED );
              }
            } else {
              LOG.debug( "Ignoring request arriving while currently working on initializing system state: " + msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
            }
          } else if ( !BootstrapArgs.isCloudController( ) ) {
            HostManager.send( null, Lists.newArrayList( Hosts.localHost( ) ) );
          }
          break;
        case WORKING:
          LOG.debug( "Ignoring request arriving while currently working on initializing system state: " + msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
          break;
        case FINISHED:
          if ( msg.getObject( ) instanceof List ) {
            LOG.debug( "Received updated host information: " + msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
            this.receive( ( List<Host> ) msg.getObject( ) );
          } else {
            LOG.debug( "Received unknown message type: " + msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
          }
          break;
      }
    }
    
    public abstract void receive( List<Host> hostsState );
    
    public abstract void initialize( boolean doInit );
    
    @Override
    public abstract void fireEvent( Event event );
    
    @Override
    public final void viewAccepted( View newView ) {
      HostManager.this.view.viewAccepted( newView );
      /**
       * this seems dumb at first glance, but the state changing mechanism needs to be separate from
       * the state itself --
       * they have different life cycles.
       **/
    }
    
  }
  
  private class RemoteHostStateListener extends HostStateListener {
    
    /**
     * 
     */
    public RemoteHostStateListener( ) {}
    
    public void initialize( boolean doInit ) {
      if ( doInit ) {
        LOG.info( "Performing first-time system init." );
        try {
          Bootstrap.initializeSystem( );
          System.exit( 123 );
        } catch ( Throwable ex ) {
          LOG.error( ex, ex );
          System.exit( 123 );
        }
      } else {
      }
    }
    
    @Override
    public void receive( List<Host> hosts ) {
      if ( !Bootstrap.isFinished( ) ) {
        for ( Host host : hosts ) {
          if ( host.hasDatabase( ) && Eucalyptus.setupServiceDependencies( host.getBindAddress( ) ) ) {
            HostManager.this.view.markReady( );
          }
        }//TODO:GRZE: this need to be /more/ dynamic
        for ( Host host : hosts ) {
          Hosts.update( host );
        }
      } else {
        for ( Host host : hosts ) {
          Hosts.update( host );
        }
      }
      
    }
    
    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof Hertz && ( ( Hertz ) event ).isAsserted( HOST_ADVERTISE_REMOTE ) ) {
        try {
          HostManager.send( null, Lists.newArrayList( Hosts.localHost( ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
  }
  
  private class CloudControllerHostStateHandler extends HostStateListener {
    
    public CloudControllerHostStateHandler( ) {}
    
    @Override
    public void receive( List<Host> hosts ) {
      Component euca = Components.lookup( Eucalyptus.class );
      if ( !Bootstrap.isFinished( ) ) {
        for ( final Host host : hosts ) {
          Hosts.update( host );
        }
        //NOTE:GRZE: setup any existing remote DBs here
        for ( Host host : Hosts.listRemoteDatabases( ) ) {
          Eucalyptus.setupServiceDependencies( host.getBindAddress( ) );
        }
        HostManager.this.view.markReady( );
        return;
      } else {
        for ( final Host host : hosts ) {
          Hosts.update( host );
          if ( !host.hasBootstrapped( ) && !host.isLocalHost( ) ) {/** trigger startup on remote hosts **/
            try {
              ServiceConfiguration config = euca.getBuilder( ).lookupByHost( host.getBindAddress( ).getHostAddress( ) );
              LOG.debug( "Requesting first time initialization for remote cloud controller: " + host );
              HostManager.send( host.getGroupsId( ), new Initialize( ) );
            } catch ( Exception ex ) {
              Logs.exhaust( ).error( ex );
              LOG.debug( "Requesting remote component startup: " + host );
              HostManager.send( host.getGroupsId( ), new NoInitialize( ) );
            }
          }
        }
      }
    }
    
    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof Hertz && ( ( Hertz ) event ).isAsserted( HOST_ADVERTISE_CLOUD ) && Bootstrap.isFinished( ) ) {
        HostManager.send( null, ( Serializable ) Hosts.listDatabases( ) );
      }
    }
    
    @Override
    public void initialize( boolean doInit ) {}
    
  }
  
  class CurrentView {
    private final AtomicMarkableReference<View> currentView = new AtomicMarkableReference<View>( null, true );
    
    public View getCurrentView( ) {
      boolean[] holder = new boolean[1];
      View view = currentView.get( holder );
//      if ( holder[0] ) {
//        return null;
//      } else {
      return view;
//      }
    }
    
    private boolean setInitialView( View oldView, View newView ) {
      return this.currentView.compareAndSet( oldView, newView, true, !( BootstrapArgs.isCloudController( ) && oldView == null && newView.size( ) == 1 ) );//handle the bootstrap case correctly
    }
    
    public void viewAccepted( View newView ) {
      if ( this.setInitialView( null, newView ) ) {
        LOG.info( "Receiving initial view..." );
      } else if ( !this.isReady( ) ) {
        LOG.info( "Receiving view.  Still waiting for database..." );
        this.setInitialView( this.getCurrentView( ), newView );
      } else {
        this.currentView.set( newView, false );
      }
      LOG.info( "-> view: " + this.currentView.getReference( ) );
      LOG.info( "-> mark: " + this.currentView.isMarked( ) );
      Hosts.change( this.currentView.getReference( ).getMembers( ) );
    }
    
    public Boolean isReady( ) {
      return !this.currentView.isMarked( );
    }
    
    void markReady( ) {
      if ( !this.isReady( ) ) {
        this.currentView.set( this.currentView.getReference( ), false );
      }
    }
    
  }
  
  public static String getMembershipGroupName( ) {
    return HostManager.getInstance( ).membershipGroupName;
  }
  
  private static JChannel buildChannel( ) {
    try {
      final JChannel channel = new JChannel( false );
      channel.setName( Internets.localHostIdentifier( ) );
      ProtocolStack stack = new ProtocolStack( );
      channel.setProtocolStack( stack );
      stack.addProtocols( Protocols.getMembershipProtocolStack( ) );
      stack.init( );
      return channel;
    } catch ( Exception ex ) {
      LOG.fatal( ex, ex );
      throw new RuntimeException( ex );
    }
  }
  
  public JChannel getMembershipChannel( ) {
    return this.membershipChannel;
  }
  
  public PhysicalAddress getPhysicalAddress( ) {
    return this.physicalAddress;
  }
  
  public static Future<?> send( final Address dest, final Serializable msg ) {
    final Message outMsg = new Message( dest, null, msg );
    outMsg.putHeader( Protocols.lookupRegisteredId( EpochHeader.class ), new EpochHeader( Topology.epoch( ) ) );
    StackTraceElement caller = Thread.currentThread( ).getStackTrace( )[1];
    LOG.debug( caller.getClassName( ).replaceAll( "^.*\\.","" ) + "." + caller.getMethodName( ) + ":" + caller.getLineNumber( ) + " sending message to: " + dest + " with payload " + msg.toString( ) + " " + outMsg.getHeaders( ) );
    return Threads.lookup( Empyrean.class, HostManager.class ).limitTo( 8 ).submit( new Runnable( ) {
      
      @Override
      public void run( ) {
        View v = HostManager.getInstance( ).view.getCurrentView( );
        if ( dest == null && ( v == null || v.getMembers( ).size( ) <= 1 ) ) {
          return;
        } else {
          try {
            LOG.trace( "Sending message to: " + dest + " with payload " + msg.toString( ) );
            singleton.membershipChannel.send( outMsg );
          } catch ( ChannelNotConnectedException ex ) {
            LOG.error( ex, ex );
          } catch ( ChannelClosedException ex ) {
            LOG.error( ex, ex );
          } catch ( Exception ex ) {
            LOG.error( ex, ex );
          }
        }
      }
    } );
    
  }
  
  interface InitRequest {}
  
  static class Initialize implements InitRequest, Serializable {}
  
  static class NoInitialize implements InitRequest, Serializable {}
  
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
