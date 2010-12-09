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

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Authorizations;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.component.ResourceAllocate;
import com.eucalyptus.component.ResourceAllocationException;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.cloud.VmAllocationInfo;

public class CreateVmInstances {
  
  public VmAllocationInfo allocate( final VmAllocationInfo vmAllocInfo ) throws EucalyptusCloudException {
    try {
      int quantity = getVmAllocationNumber( vmAllocInfo );
      // Allocate VmType instances
      Authorizations.allocatePrivileged( VmType.class, vmAllocInfo.getVmTypeInfo( ).getName( ), quantity, null /* allocator. Abstract allocation. */ );
      // Allocate vm instances
      Authorizations.allocatePrivileged( VmInstance.class, "", quantity, new ResourceAllocate<VmInstance>( ) {

        @Override
        public void allocate( ) throws ResourceAllocationException {
          String reservationId = VmInstances.getId( vmAllocInfo.getReservationIndex( ), 0 ).replaceAll( "i-", "r-" );
          int vmIndex = 1; /*<--- this corresponds to the first instance id CANT COLLIDE WITH RSVID             */
          for ( ResourceToken token : vmAllocInfo.getAllocationTokens( ) ) {
            if( Clusters.getInstance( ).hasNetworking( ) ) {
              for ( Integer networkIndex : token.getPrimaryNetwork( ).getIndexes( ) ) {
                VmInstance vmInst = getVmInstance( vmAllocInfo, reservationId, token, vmIndex++, networkIndex );
                VmInstances.getInstance( ).register( vmInst );
                token.getInstanceIds( ).add( vmInst.getInstanceId( ) );
              }
            } else {
              for ( int i = 0; i < token.getAmount( ); i++ ) {
                VmInstance vmInst = getVmInstance( vmAllocInfo, reservationId, token, vmIndex++, -1 );
                VmInstances.getInstance( ).register( vmInst );
                token.getInstanceIds( ).add( vmInst.getInstanceId( ) );
              }
            }
          }
          vmAllocInfo.setReservationId( reservationId );
        }
        
      });
      return vmAllocInfo;
    } catch ( AuthException e ) {
      throw new EucalyptusCloudException( "Resource allocation is denied due to quota", e );
    } catch ( ResourceAllocationException e ) {
      throw new EucalyptusCloudException( e );
    }
  }

  private int getVmAllocationNumber( VmAllocationInfo vmAllocInfo ) {
    int vmNum = 0;
    for ( ResourceToken token : vmAllocInfo.getAllocationTokens( ) ) {
      if( Clusters.getInstance( ).hasNetworking( ) ) {
        vmNum += token.getPrimaryNetwork( ).getIndexes( ).size( );
      } else {
        vmNum += token.getAmount( );
      }
    }
    return vmNum;
  }
  
  private VmInstance getVmInstance( VmAllocationInfo vmAllocInfo, String reservationId, ResourceToken token, Integer index, Integer networkIndex ) {
    VmInstance vmInst = new VmInstance( reservationId,
                                        index - 1,
                                        VmInstances.getId( vmAllocInfo.getReservationIndex(), index ),
                                        vmAllocInfo.getRequest().getUserId(),
                                        token.getCluster(),
                                        vmAllocInfo.getUserData(),
                                        vmAllocInfo.getKeyInfo(),
                                        vmAllocInfo.getVmTypeInfo(),
                                        vmAllocInfo.getPlatform( ),
                                        vmAllocInfo.getNetworks(),
                                        networkIndex.toString( ) );
    return vmInst;
  }
}
