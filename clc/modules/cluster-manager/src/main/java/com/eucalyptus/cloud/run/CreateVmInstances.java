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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cloud.run;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.context.Context;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.vm.VmTypes;
import edu.ucsb.eucalyptus.cloud.ResourceToken;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class CreateVmInstances {
  private static Logger LOG = Logger.getLogger( CreateVmInstances.class );
  
  public Allocation allocate( final Allocation allocInfo ) throws EucalyptusCloudException, MetadataException {
    long quantity = getVmAllocationNumber( allocInfo );
    Context ctx = allocInfo.getContext( );
    User requestUser = ctx.getUser( );
    UserFullName userFullName = ctx.getUserFullName( );
    String action = PolicySpec.requestToAction( allocInfo.getRequest( ) );
    String vmType = allocInfo.getVmType( ).getName( );
    // Allocate VmType instances
    if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_VMTYPE, vmType, action, requestUser, 1L ) ) {
      throw new EucalyptusCloudException( "Quota exceeded in allocating vm type " + vmType + " for " + requestUser.getName( ) );
    }
    // Allocate vm instances
    if ( !Permissions.canAllocate( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_INSTANCE, "", action, requestUser, quantity ) ) {
      throw new EucalyptusCloudException( "Quota exceeded in allocating " + quantity + " vm instances for " + requestUser.getName( ) );
    }
    String reservationId = VmInstances.getId( allocInfo.getReservationIndex( ), -1 ).replaceAll( "i-", "r-" );
    int vmIndex = 0; /*<--- this corresponds to the first instance id CANT COLLIDE WITH RSVID             */
    for ( ResourceToken token : allocInfo.getAllocationTokens( ) ) {
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        for ( Integer networkIndex : token.getPrimaryNetwork( ).getIndexes( ) ) {
          VmInstance vmInst = getVmInstance( userFullName, allocInfo, reservationId, token, vmIndex++, networkIndex );
          VmInstances.getInstance( ).register( vmInst );
          token.getInstanceIds( ).add( vmInst.getInstanceId( ) );
        }
      } else {
        for ( int i = 0; i < token.getAmount( ); i++ ) {
          VmInstance vmInst = getVmInstance( userFullName, allocInfo, reservationId, token, vmIndex++, -1 );
          VmInstances.getInstance( ).register( vmInst );
          token.getInstanceIds( ).add( vmInst.getInstanceId( ) );
        }
      }
    }
    return allocInfo;
  }
  
  private int getVmAllocationNumber( Allocation allocInfo ) {
    int vmNum = 0;
    for ( ResourceToken token : allocInfo.getAllocationTokens( ) ) {
      if ( Clusters.getInstance( ).hasNetworking( ) ) {
        vmNum += token.getPrimaryNetwork( ).getIndexes( ).size( );
      } else {
        vmNum += token.getAmount( );
      }
    }
    return vmNum;
  }
  
  private VmInstance getVmInstance( UserFullName userFullName, Allocation allocInfo, String reservationId, ResourceToken token, Integer index, Integer networkIndex ) {
    VmTypeInfo vbr = null;//TODO:GRZE:this is crap.
    try {
      vbr = VmTypes.asVmTypeInfo( allocInfo.getVmType( ), allocInfo.getBootSet( ).getMachine( ) );
    } catch ( MetadataException ex ) {
      LOG.error( ex , ex );
    }
    VmInstance vmInst = new VmInstance( userFullName,  VmInstances.getId( allocInfo.getReservationIndex( ), index ), token.getInstanceUuids( ).get( index ), reservationId, 
                                        index, token.getCluster( ),
                                        allocInfo.getUserData( ),
                                        vbr,
                                        allocInfo.getSshKeyPair( ),
                                        allocInfo.getVmType( ),
                                        allocInfo.getBootSet( ).getMachine( ).getPlatform( ).name( ),
                                        allocInfo.getNetworks( ),
                                        networkIndex.toString( ) );
    return vmInst;
  }
}
