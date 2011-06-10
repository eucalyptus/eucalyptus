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

import java.net.InetAddress;
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
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Host;
import com.eucalyptus.component.Hosts;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Internets;

public class HostManager implements Receiver, ExtendedMembershipListener, EventListener {
  private static Logger                       LOG         = Logger.getLogger( HostManager.class );
  private final JChannel                      membershipChannel;
  private final PhysicalAddress               physicalAddress;
  private final String                        membershipGroupName;
  private final AtomicMarkableReference<View> currentView = new AtomicMarkableReference<View>( null, true );
  public static HostManager                   singleton;
  
  private HostManager( ) {
    this.membershipChannel = HostManager.buildChannel( );
    this.membershipChannel.setReceiver( this );
    this.membershipGroupName = SystemIds.membershipGroupName( );//TODO:GRZE:RELEASE make cached
    try {
      LOG.info( "Starting membership channel... " );
      this.membershipChannel.connect( this.membershipGroupName );
      this.physicalAddress = ( PhysicalAddress ) this.membershipChannel.downcall( new org.jgroups.Event( org.jgroups.Event.GET_PHYSICAL_ADDRESS, this.membershipChannel.getAddress( ) ) );
      LOG.info( "Started membership channel: " + this.membershipGroupName );
    } catch ( ChannelException ex ) {
      LOG.fatal( ex, ex );
      throw BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
    }
    ListenerRegistry.getInstance( ).register( ClockTick.class, new HostStateMonitor( ) );
    ListenerRegistry.getInstance( ).register( ClockTick.class, this );
  }
  
  public static View getCurrentView( ) {
    return singleton.currentView.getReference( );
  }
  
  public static Boolean isReady( ) {
    return !singleton.currentView.isMarked( );
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
  
  public static String getMembershipGroupName( ) {
    return HostManager.getInstance( ).membershipGroupName;
  }
  
  private static JChannel buildChannel( ) {
    final JChannel channel = new JChannel( false );
    channel.setName( Internets.localhostIdentifier( ) );
    ProtocolStack stack = new ProtocolStack( );
    channel.setProtocolStack( stack );
    stack.addProtocols( Protocols.getMembershipProtocolStack( ) );
    try {
      stack.init( );
    } catch ( Exception ex ) {
      LOG.fatal( ex, ex );
      System.exit( 1 );
    }
    return channel;
  }
  
  @Override
  public void receive( Message msg ) {
    View view = this.currentView.getReference( );
    if( view != null ) {
      LOG.debug( msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
      Host recvHost = ( Host ) msg.getObject( );
      if ( !Bootstrap.isFinished( ) ) {
        if ( recvHost.hasDatabase( ) && !Bootstrap.isCloudController( ) ) {
          for ( InetAddress addr : recvHost.getHostAddresses( ) ) {
            if ( this.setupCloudLocals( addr ) ) {
              break;
            }
          }
        } else if ( Bootstrap.isCloudController( ) ) {
          this.currentView.set( this.currentView.getReference( ), false );
        }
      }
      LOG.debug( "Received updated host information: " + recvHost );
      Host hostEntry = Hosts.updateHost( view, recvHost );
    }
  }
  
  private boolean setupCloudLocals( InetAddress addr ) {
    if ( !Internets.testReachability( addr ) ) {
      return false;
    } else {
      try {
        for ( Component c : Components.list( ) ) {//TODO:GRZE:URGENT THIS LIES
          if( c.getComponentId( ).isCloudLocal( ) ) {
            try {
              ServiceConfiguration config = c.initRemoteService( addr );
              c.loadService( config );
            } catch ( ServiceRegistrationException ex ) {
              LOG.error( ex );
            }
          }
        }
        for ( Bootstrap.Stage stage : Bootstrap.Stage.values( ) ) {
          stage.updateBootstrapDependencies( );
        }
      } catch( RuntimeException ex ) {
        LOG.error( ex, ex );
        throw ex;
      } finally {
        this.currentView.set( this.currentView.getReference( ), false );
      }
      return true;
    }
  }
  
  @Override
  public byte[] getState( ) {
    return null;
  }
  
  @Override
  public void setState( byte[] state ) {}
  
  @Override
  public void viewAccepted( final View newView ) {
    if ( this.currentView.compareAndSet( null, newView, true, !Bootstrap.isCloudController( ) ) ) {
      LOG.info( "Receiving initial view..." );
    } else if ( this.currentView.compareAndSet( this.currentView.getReference( ), newView, true, true ) ) {
      LOG.info( "Receiving view.  Still waiting for database..." );
    } else {
      this.currentView.set( newView, false );
    }
    LOG.info( "-> view: " + this.currentView.getReference( ) );
    LOG.info( "-> mark: " + this.currentView.isMarked( ) );
    if ( !Bootstrap.isCloudController( ) ) {
      HostManager.this.broadcastAddresses( );
    }
  }
  
  private void broadcastAddresses( ) {
    final View view = this.currentView.getReference( );
    if ( view == null ) {
      return;
    } else {
      Threads.lookup( Empyrean.class, HostMembershipBootstrapper.class ).submit( new Runnable( ) {
        @Override
        public void run( ) {
          
          for ( final Address addr : view.getMembers( ) ) {
            if ( ( HostManager.this.membershipChannel.getAddress( ) != null ) && ( !HostManager.this.membershipChannel.getAddress( ).equals( addr ) ) ) {
              Host localHost = Hosts.localHost( );
              LOG.info( "Broadcasting local address info for viewId=" + view.getViewId( ) + " to: " + addr + " with host info: " + localHost );
              try {
                HostManager.this.membershipChannel.send( new Message( addr, null, localHost ) );
              } catch ( ChannelNotConnectedException ex ) {
                LOG.error( ex, ex );
              } catch ( ChannelClosedException ex ) {
                LOG.error( ex, ex );
              }
            }
          }
        }
      } );
    }
    
  }
  
  @Override
  public void suspect( Address suspected_mbr ) {}
  
  @Override
  public void block( ) {
    LOG.debug( "HostManager: blocked" );
  }
  
  @Override
  public void unblock( ) {
    LOG.debug( "HostManager: unblocked" );
  }
  
  @Override
  public void fireEvent( Event event ) {
    if ( event instanceof ClockTick ) {
      this.broadcastAddresses( );
    }
  }
  
  public JChannel getMembershipChannel( ) {
    return this.membershipChannel;
  }

  public PhysicalAddress getPhysicalAddress( ) {
    return this.physicalAddress;
  }
  
}
