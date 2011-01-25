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

package com.eucalyptus.ws;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.DescribeServicesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeServicesType;
import edu.ucsb.eucalyptus.msgs.DisableServiceResponseType;
import edu.ucsb.eucalyptus.msgs.DisableServiceType;
import edu.ucsb.eucalyptus.msgs.EnableServiceResponseType;
import edu.ucsb.eucalyptus.msgs.EnableServiceType;
import edu.ucsb.eucalyptus.msgs.ServiceId;
import edu.ucsb.eucalyptus.msgs.ServiceStatusType;
import edu.ucsb.eucalyptus.msgs.StartServiceResponseType;
import edu.ucsb.eucalyptus.msgs.StartServiceType;
import edu.ucsb.eucalyptus.msgs.StopServiceResponseType;
import edu.ucsb.eucalyptus.msgs.StopServiceType;

public class EmpyreanService {
  private static Logger LOG = Logger.getLogger( EmpyreanService.class );
  public StartServiceResponseType startService( StartServiceType request ) {
    StartServiceResponseType reply = request.getReply( );
    return reply;
  }
  public StopServiceResponseType stopService( StopServiceType request ) {
    StopServiceResponseType reply = request.getReply( );
    return reply;
  }
  public EnableServiceResponseType enableService( EnableServiceType request ) {
    EnableServiceResponseType reply = request.getReply( );
    return reply;
  }
  public DisableServiceResponseType disableService( DisableServiceType request ) {
    DisableServiceResponseType reply = request.getReply( );
    for( ServiceId serviceId : request.getServiceIds( ) ) {
      try {
        Component c = Components.lookup( serviceId.getType( ) );
        for( Service service : c.getServices( ) ) {
          String partition = service.getServiceConfiguration( ).getPartition( );
          String name = service.getServiceConfiguration( ).getName( );
          if( partition.equals( serviceId.getPartition( ) ) && name.equals( serviceId.getName( ) ) ) {
            if( Component.State.ENABLED.equals( service.getState( ) ) ) {
              try {
                c.disableService( service.getServiceConfiguration( ) );
                reply.getServiceIds( ).add( serviceId );
              } catch ( ServiceRegistrationException ex ) {
                LOG.error( "DISABLE'ing service failed: " + ex.getMessage( ), ex );
              }
            } else {
              LOG.error( "Attempt to DISABLE a service which is not currently ENABLED: " + service.toString( ) );
            }
          }
        }
      } catch ( NoSuchElementException ex ) {
        Exceptions.trace( "Failed to lookup component of type: " + serviceId.getType( ), ex );
      }
    }
    return reply;
  }
  public DescribeServicesResponseType describeService( DescribeServicesType request ) {
    final DescribeServicesResponseType reply = request.getReply( );
    for( Component comp : Components.list( ) ) {
      if( comp.isRunningLocally( ) ) {
        final Service localService = comp.getLocalService( );
        reply.getServiceStatus( ).add( new ServiceStatusType( ) {{
          setServiceId( localService.getServiceId( ) );
          setLocalEpoch( reply.getBaseEpoch( ) );
          setLocalState( localService.getState( ).toString( ) );
          getDetails( ).addAll( localService.getDetails( ) );
        }} );
      }
    }
    return reply;
  }

}
