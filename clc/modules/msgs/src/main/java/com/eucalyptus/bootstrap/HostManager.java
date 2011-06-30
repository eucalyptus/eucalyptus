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

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicMarkableReference;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedMembershipListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.PhysicalAddress;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Host;
import com.eucalyptus.component.Hosts;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.Internets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class HostManager {
  private static Logger         LOG = Logger.getLogger( HostManager.class );
  private final JChannel        membershipChannel;
  private final PhysicalAddress physicalAddress;
  private final String          membershipGroupName;
  private final CurrentView     view;
  private HostStateListener     stateListener;
  private static HostManager    singleton;
  private final Predicate<Host> dbFilter;
  
  private HostManager( ) {
    this.view = new CurrentView( );
    this.membershipChannel = HostManager.buildChannel( );
    this.stateListener = Bootstrap.isCloudController( )
      ? new CloudControllerHostStateHandler( )
      : new RemoteHostStateListener( );
    this.membershipChannel.setReceiver( this.stateListener );
    //TODO:GRZE:set socket factory for crypto
    this.membershipGroupName = SystemIds.membershipGroupName( );
    try {
      LOG.info( "Starting membership channel... " );
      this.membershipChannel.connect( this.membershipGroupName );
      this.physicalAddress = ( PhysicalAddress ) this.membershipChannel.downcall( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS,
                                                                                                         this.membershipChannel.getAddress( ) ) );
      LOG.info( "Started membership channel: " + this.membershipGroupName );
      this.dbFilter = new Predicate<Host>( ) {
        
        @Override
        public boolean apply( Host arg0 ) {
          return arg0.hasDatabase( ) || arg0.getGroupsId( ).equals( HostManager.this.membershipChannel.getLocalAddress( ) );
        }
      };
      ListenerRegistry.getInstance( ).register( Hertz.class, this.stateListener );
    } catch ( ChannelException ex ) {
      LOG.fatal( ex, ex );
      throw BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
    }
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
  
  abstract class HostStateListener implements Receiver, ExtendedMembershipListener, EventListener {
    
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
    public abstract void receive( Message msg );
    
    @Override
    public abstract void fireEvent( Event event );
    
    @Override
    public final void viewAccepted( View new_view ) {
      HostManager.this.view.viewAccepted( new_view );//this seems dumb at first glance, but the state changing mechanism needs to be separate from the state itself -- they hae diffferent life cycles.
    }
    
    protected boolean setupRemoteCloudServices( InetAddress addr ) {
      if ( !Internets.testReachability( addr ) || Internets.testLocal( addr ) ) {
        return false;
      } else {
        try {
          for ( ComponentId compId : ComponentIds.list( ) ) {//TODO:GRZE:URGENT THIS LIES
            if ( compId.isCloudLocal( ) ) {
              try {
                Component comp = Components.lookup( compId );
                ServiceConfiguration config = comp.initRemoteService( addr );
                if( Component.State.INITIALIZED.ordinal( ) >= config.lookupState( ).ordinal( ) ) {
                  comp.loadService( config ).get( );
                }
              } catch ( Exception ex ) {
                LOG.error( ex, ex );
              }
            }
            for ( Bootstrap.Stage stage : Bootstrap.Stage.values( ) ) {
              stage.updateBootstrapDependencies( );
            }
            HostManager.this.view.markReady( );
          }
        } catch ( RuntimeException ex ) {
          LOG.error( ex, ex );
          throw ex;
        }
        return true;
      }
    }
    
  }
  
  private class RemoteHostStateListener extends HostStateListener {
    @Override
    public void receive( Message msg ) {
      LOG.debug( msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
      if ( !Bootstrap.isFinished( ) ) {
        List<Host> dbHosts = ( List<Host> ) msg.getObject( );
        for ( Host dbHost : dbHosts ) {
          this.setupRemoteCloudServices( dbHost.getHostAddress( ) );
        }
      }//TODO:GRZE: this need to be /more/ dynamic
    }
    
    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof Hertz && ( ( Hertz ) event ).isAsserted( 10 ) && HostManager.this.membershipChannel.isConnected( ) ) {
        LOG.debug( "Sending state info: \n\t" + Hosts.localHost( ) );
        try {
          HostManager.this.membershipChannel.send( new Message( null, null, Lists.newArrayList( Hosts.localHost( ) ) ) );
        } catch ( ChannelNotConnectedException ex ) {
          LOG.error( ex, ex );
        } catch ( ChannelClosedException ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
  }
  
  private class CloudControllerHostStateHandler extends HostStateListener {
    
    @Override
    public void receive( Message msg ) {
      List<Host> dbHosts = ( List<Host> ) msg.getObject( );
      for ( Host dbHost : dbHosts ) {
        this.setupRemoteCloudServices( dbHost.getHostAddress( ) );
      }
    }
    
    @Override
    public void fireEvent( Event event ) {
      if ( event instanceof Hertz && ( ( Hertz ) event ).isAsserted( 10 ) && HostManager.this.membershipChannel.isConnected( ) ) {
        HostManager.this.view.sendState( );
      }
    }
  }
  
  class CurrentView {
    private final AtomicMarkableReference<View> currentView = new AtomicMarkableReference<View>( null, true );
    
    public View getCurrentView( ) {
      boolean[] holder = new boolean[1];
      View view = currentView.get( holder );
      if ( holder[0] ) {
        return null;
      } else {
        return view;
      }
    }
    
    private boolean setView( View oldView, View newView ) {
      return this.currentView.compareAndSet( oldView, newView, true, newView.getMembers( ).size( ) == 1 && Bootstrap.isCloudController( ) );//handle the bootstrap case correctly
    }
    
    public void viewAccepted( View newView ) {
      if ( this.setView( null, newView ) ) {
        LOG.info( "Receiving initial view..." );
      } else if ( this.setView( this.currentView.getReference( ), newView ) ) {
        LOG.info( "Receiving view.  Still waiting for database..." );
      } else {
        LOG.info( "Receiving view.  Still waiting for database..." );
        this.currentView.set( newView, true );
      }
      LOG.info( "-> view: " + this.currentView.getReference( ) );
      LOG.info( "-> mark: " + this.currentView.isMarked( ) );
    }
    
    public Boolean isReady( ) {
      return !this.currentView.isMarked( );
    }
    
    void markReady( ) {
      this.currentView.set( this.currentView.getReference( ), false );
    }
    
    public void sendState( ) {
      final Iterable<Host> dbs = Iterables.filter( Hosts.list( ), HostManager.this.dbFilter );
      LOG.debug( "Sending state info: \n" + Joiner.on( "\n -> " ).join( dbs ) );
      try {
        HostManager.this.membershipChannel.send( new Message( null, null, Lists.newArrayList( dbs ) ) );
      } catch ( ChannelNotConnectedException ex ) {
        LOG.error( ex, ex );
      } catch ( ChannelClosedException ex ) {
        LOG.error( ex, ex );
      }
    }
  }
  
  public static String getMembershipGroupName( ) {
    return HostManager.getInstance( ).membershipGroupName;
  }
  
  private static JChannel buildChannel( ) {
    try {
      final JChannel channel = new JChannel( false );
      channel.setName( Internets.localhostIdentifier( ) );
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
  
}
