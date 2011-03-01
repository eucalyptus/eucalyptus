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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Database;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Hmacs;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.NetworkUtil;
import com.google.common.base.Join;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteConfiguration )
public class MembershipBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( MembershipBootstrapper.class );
  private JChannel      membershipChannel;
  private String        membershipGroupName;
  
  @Override
  public boolean load( ) throws Exception {
    try {
      this.membershipGroupName = "Eucalyptus-" + Hmacs.generateSystemSignature( );
      this.membershipChannel = MembershipManager.buildChannel( );
      final boolean[] done = { false };
      final ReentrantLock lock = new ReentrantLock( );
      final Condition isReady = lock.newCondition( );
      this.membershipChannel.setReceiver( new ReceiverAdapter( ) {
        public void viewAccepted( View newView ) {
          if ( Components.lookup( Eucalyptus.class ).isLocal( ) ) {
            LOG.info( "view: " + newView.printDetails( ) );
            for ( Address addr : newView.getMembers( ) ) {
              if ( !MembershipBootstrapper.this.membershipChannel.getAddress( ).equals( addr ) ) {
                try {
                  LOG.info( "Sending to address=" + addr + " of type=" + addr.getClass( ) );
                  MembershipBootstrapper.this.membershipChannel.send( new Message( addr, null, Join.join( ":", NetworkUtil.getAllAddresses( ) ) ) );
                } catch ( ChannelNotConnectedException ex ) {
                  LOG.error( ex, ex );
                } catch ( ChannelClosedException ex ) {
                  LOG.error( ex, ex );
                } catch ( SocketException ex ) {
                  LOG.error( ex, ex );
                }
              }
            }
          } else {
            LOG.info( "view: " + newView );
          }
        }
        
        public void receive( Message msg ) {
          LOG.info( msg.getObject( ) + " [" + msg.getSrc( ) + "]" );
          if ( !Components.lookup( Eucalyptus.class ).isLocal( ) ) {
            String[] dbAddrs = ( ( String ) msg.getObject( ) ).split( ":" );
            for ( String maybeDbAddr : dbAddrs ) {
              try {
                if ( NetworkUtil.testReachability( maybeDbAddr ) ) {
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
            lock.lock( );
            try {
              done[0] = true;
              isReady.signalAll( );
            } finally {
              lock.unlock( );
            }
          }
        }
      } );
      lock.lock( );
      try {
        this.membershipChannel.connect( this.membershipGroupName );
        LOG.info( "Started membership channel " + this.membershipGroupName );
        if ( !Components.lookup( Eucalyptus.class ).isLocal( ) ) {
          LOG.warn( "Blocking the bootstrap thread for testing." );
          if ( !done[0] ) {
            isReady.await( );
          }
        }
      } finally {
        lock.unlock( );
      }
      return true;
    } catch ( Exception ex ) {
      LOG.fatal( ex, ex );
      BootstrapException.throwFatal( "Failed to connect membership channel because of " + ex.getMessage( ), ex );
      return false;
    }
  }
  
  @Override
  public boolean start( ) throws Exception {
    return true;
  }
  
  @Override
  public boolean enable( ) throws Exception {
    return false;
  }
  
  @Override
  public boolean stop( ) throws Exception {
    return false;
  }
  
  @Override
  public void destroy( ) throws Exception {}
  
  @Override
  public boolean disable( ) throws Exception {
    return false;
  }
  
  @Override
  public boolean check( ) throws Exception {
    return false;
  }
  
}
