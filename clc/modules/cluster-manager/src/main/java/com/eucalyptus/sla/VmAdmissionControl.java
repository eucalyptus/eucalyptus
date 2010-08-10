/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.sla;

import java.util.List;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.records.EventType;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;
import com.eucalyptus.records.EventRecord;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class VmAdmissionControl {
  
  private static Logger LOG = Logger.getLogger( VmAdmissionControl.class );
  
  public VmAllocationInfo verify( RunInstancesType request ) throws EucalyptusCloudException {
    //:: encapsulate the request into a VmAllocationInfo object and forward it on :://
    VmAllocationInfo vmAllocInfo = new VmAllocationInfo( request );
    if( vmAllocInfo.getRequest( ).getInstanceType( ) == null || "".equals( vmAllocInfo.getRequest( ).getInstanceType( ) )) {
      vmAllocInfo.getRequest( ).setInstanceType( VmInstance.DEFAULT_TYPE );
    }
    vmAllocInfo.setReservationIndex( Counters.getIdBlock( request.getMaxCount( ) ) );
    
    byte[] userData = new byte[0];
    if ( vmAllocInfo.getRequest( ).getUserData( ) != null ) {
      try {
        userData = Base64.decode( vmAllocInfo.getRequest( ).getUserData( ) );
      } catch ( Exception e ) {
      }
    }
    vmAllocInfo.setUserData( userData );
    vmAllocInfo.getRequest( ).setUserData( new String( Base64.encode( userData ) ) );
    return vmAllocInfo;
  }
  
  public VmAllocationInfo evaluate( VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    List<ResourceAllocator> pending = Lists.newArrayList( );
    pending.add( new NodeResourceAllocator() );
    if( Clusters.getInstance( ).hasNetworking( ) ) {
      pending.add( new AddressAllocator() );
      pending.add( new PrivateNetworkAllocator( ) );
      pending.add( new SubnetIndexAllocator( ) );
    }
    List<ResourceAllocator> finished = Lists.newArrayList( );
    
    for( ResourceAllocator allocator : pending ) {
      try {
        allocator.allocate( vmAllocInfo );
        finished.add( allocator );
      } catch (ScriptExecutionFailedException e) {
        if( e.getCause() != null ) {
          throw new EucalyptusCloudException( e.getCause( ).getMessage( ), e.getCause( ) );
        } else {
          throw new EucalyptusCloudException( e.getMessage( ), e );
        }
      } catch ( Throwable e ) {
        LOG.debug( e, e );
        try {
          allocator.fail( vmAllocInfo, e );
        } catch ( Throwable e1 ) {
          LOG.debug( e1, e1 );
        }
        for( ResourceAllocator rollback : Iterables.reverse( finished ) ) {
          try {
            rollback.fail( vmAllocInfo, e );
          } catch ( Throwable e1 ) {
            LOG.debug( e1, e1 );
          }
        }
        throw new EucalyptusCloudException( e.getMessage( ), e );
      }
    }
    EventRecord.here( this.getClass(), EventType.VM_RESERVED, LogUtil.dumpObject( vmAllocInfo ) ).trace( );
    return vmAllocInfo;
  }
  
}
