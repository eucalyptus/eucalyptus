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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.ExtendedMembershipListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.jgroups.stack.ProtocolStack;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Hmacs;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Internets;
import com.google.common.base.Join;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicMarkableReference;

public class HostManager implements Receiver, ExtendedMembershipListener {
  private static Logger                 LOG         = Logger.getLogger( HostManager.class );
  private final JChannel                membershipChannel;
  private final String                  membershipGroupName;
  private final boolean[]               done        = { false };
  private final ReentrantLock           lock        = new ReentrantLock( );
  private final Condition               isReady     = lock.newCondition( );
  private final AtomicMarkableReference currentView = new AtomicMarkableReference( null, false );
  private static HostManager      singleton;
  
  private HostManager( ) {
    this.membershipChannel = HostManager.buildChannel( );
    this.membershipChannel.setReceiver( this );
    this.membershipGroupName = Eucalyptus.class.getSimpleName( ) + "-" + Hmacs.generateSystemToken( Eucalyptus.class.getSimpleName( ).getBytes( ) );
    try {
      this.membershipChannel.connect( this.membershipGroupName );
      LOG.info( "Started membership channel " + this.membershipGroupName );
    } catch ( ChannelException ex ) {
      LOG.fatal( ex, ex );
      BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
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
          return singleton = new HostManager( );
        }
      }
    }
  }
  
  public static String getMembershipGroupName( ) {
    return HostManager.getInstance( ).membershipGroupName;
  }
  
  private static JChannel buildChannel( ) {
    final JChannel channel = new JChannel( false );
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
    LOG.info( msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
    lock.lock( );
    try {
      String[] dbAddrs = ( ( String ) msg.getObject( ) ).split( ":" );
      for ( String maybeDbAddr : dbAddrs ) {
        try {
          if ( Internets.testReachability( maybeDbAddr ) ) {
            String host = maybeDbAddr;
            for ( Component c : Components.list( ) ) {
              if ( c.getComponentId( ).isCloudLocal( ) ) {
                URI uri = c.getUri( host, c.getComponentId( ).getPort( ) );
                ServiceBuilder builder = c.getBuilder( );
                ServiceConfiguration config = builder.toConfiguration( uri );
                c.loadService( config );
              }
            }
            for ( Bootstrap.Stage stage : Bootstrap.Stage.values( ) ) {
              stage.updateBootstrapDependencies( );
            }
            break;
          }
        } catch ( ServiceRegistrationException ex ) {
          LOG.error( ex, ex );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      done[0] = true;
      isReady.signalAll( );
    } finally {
      lock.unlock( );
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
    LOG.info( "view: " + newView );
    if ( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
      Threads.lookup( Empyrean.class, HostMembershipBootstrapper.class ).submit( new Runnable( ) {
        
        @Override
        public void run( ) {
          for ( final Address addr : newView.getMembers( ) ) {
            if ( !HostManager.this.membershipChannel.getAddress( ).equals( addr ) ) {
              LOG.info( "Sending to address=" + addr + " of type=" + addr.getClass( ) );
              try {
                HostManager.this.membershipChannel.send( new Message( addr, null, Join.join( ":", Internets.getAllAddresses( ) ) ) );
              } catch ( ChannelNotConnectedException ex ) {
                LOG.error( ex, ex );
              } catch ( ChannelClosedException ex ) {
                LOG.error( ex, ex );
              } catch ( SocketException ex ) {
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
    this.currentView.attemptMark( this.currentView.getReference( ), true );
  }
  
  @Override
  public void unblock( ) {
    this.currentView.attemptMark( this.currentView.getReference( ), false );
  }
  
}
