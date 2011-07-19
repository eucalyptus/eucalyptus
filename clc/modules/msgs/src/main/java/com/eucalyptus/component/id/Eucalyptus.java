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

package com.eucalyptus.component.id;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.HostManager;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.Internets;

public class Eucalyptus extends ComponentId.Unpartioned {
  public static final Eucalyptus INSTANCE = new Eucalyptus( );                   //NOTE: this has a silly name because it is temporary.  do not use it as an example of good form for component ids.
  private static Logger          LOG      = Logger.getLogger( Eucalyptus.class );
  
  @Override
  public String getLocalEndpointName( ) {
    return "vm://EucalyptusRequestQueue";
  }
  
  @Override
  public Boolean hasCredentials( ) {
    return true;
  }
  
  @Override
  public boolean isUserService( ) {
    return true;
  }
  
  public static boolean setupServiceDependencies( InetAddress addr ) {
    if ( !Internets.testLocal( addr ) && !Internets.testReachability( addr ) ) {
      LOG.warn( "Failed to reach host for cloud controller: " + addr );
      return false;
    } else {
      try {
        setupServiceState( addr, Eucalyptus.INSTANCE );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return false;
      }
      for ( ComponentId compId : ComponentIds.list( ) ) {//TODO:GRZE:URGENT THIS LIES
        try {
          if ( compId.isCloudLocal( ) && !compId.isRegisterable( ) ) {
            setupServiceState( addr, compId );
          }
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      return true;
    }
    
  }
  
  private static void setupServiceState( InetAddress addr, ComponentId compId ) throws ServiceRegistrationException, ExecutionException {
    try {
      Component comp = Components.lookup( compId );
      ServiceConfiguration config = ( Internets.testLocal( addr ) )
        ? comp.initRemoteService( addr )
        : comp.initRemoteService( addr );//TODO:GRZE:REVIEW: use of initRemote
      if ( Component.State.INITIALIZED.ordinal( ) >= config.lookupState( ).ordinal( ) ) {
        comp.loadService( config ).get( );
      }
      Topology.enable( config );
    } catch ( InterruptedException ex ) {
      Thread.currentThread( ).interrupt( );
    }
  }
}
