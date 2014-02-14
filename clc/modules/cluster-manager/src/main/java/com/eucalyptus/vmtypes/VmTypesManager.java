/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 *************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.vmtypes;

import javax.annotation.Nullable;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesType;
import com.eucalyptus.compute.common.backend.ModifyInstanceTypeAttributeResponseType;
import com.eucalyptus.compute.common.backend.ModifyInstanceTypeAttributeType;
import com.eucalyptus.compute.common.backend.VmTypeDetails;
import com.eucalyptus.compute.common.backend.VmTypeEphemeralDisk;
import com.eucalyptus.compute.common.backend.VmTypeZoneStatus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.vmtypes.VmTypes.PredefinedTypes;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;

public class VmTypesManager {
  enum VmAvailabilityToZoneStatus implements Function<VmTypeAvailability, VmTypeZoneStatus> {
    INSTANCE;
    
    @Override
    public VmTypeZoneStatus apply( @Nullable final VmTypeAvailability input ) {
      return new VmTypeZoneStatus( ) {
        {
          this.setName( input.getType( ).getName( ) );
          this.setMax( input.getMax( ) );
          this.setAvailable( input.getAvailable( ) );
        }
      };
    }
  }
  
  public DescribeInstanceTypesResponseType DescribeInstanceTypes( final DescribeInstanceTypesType request ) {
    DescribeInstanceTypesResponseType reply = request.getReply( );
    for ( final VmType v : Iterables.filter( VmTypes.list( ), CloudMetadatas.filteringFor( VmType.class ).byId( request.getInstanceTypes( ) ).byPrivileges( ).buildPredicate( ) ) ) {
      VmTypeDetails vmTypeDetails = new VmTypeDetails( ) {
        {
          this.setName( v.getName( ) );
          this.setDisk( v.getDisk( ) );
          this.setCpu( v.getCpu( ) );
          this.setMemory( v.getMemory( ) );
          if ( request.getVerbose( ) ) {
            for ( EphemeralDisk e : v.getEpehemeralDisks( ) ) {
              this.getEphemeralDisk( ).add( new VmTypeEphemeralDisk( e.getDiskName( ), e.getDeviceName( ), e.getSize( ), e.getFormat( ).name( ) ) );
            }
          }
          if ( request.getAvailability( ) ) {
            for ( ServiceConfiguration cc : Topology.enabledServices( ClusterController.class ) ) {
              VmTypeAvailability available = Clusters.lookup( cc ).getNodeState( ).getAvailability( v.getName( ) );
              VmTypeZoneStatus status = VmAvailabilityToZoneStatus.INSTANCE.apply( available );
              status.setZoneName( cc.getPartition( ) );//sucks having to set this here...
              this.getAvailability( ).add( status );
            }
          }
        }
      };
      reply.getInstanceTypeDetails( ).add( vmTypeDetails );
    }
    return reply;
  }
  
  public ModifyInstanceTypeAttributeResponseType modifyVmType( final ModifyInstanceTypeAttributeType request ) throws EucalyptusCloudException {
    final ModifyInstanceTypeAttributeResponseType reply = request.getReply( );
    if ( Contexts.lookup( ).isAdministrator( ) ) {
      final Function<String, VmType> modifyFunc = new Function<String, VmType>( ) {
        @Override
        public VmType apply( String vmTypeName ) {
          try {
            VmType vmType = VmTypes.lookup( vmTypeName );
            if ( request.getReset( ) ) {
              PredefinedTypes defaultVmType = PredefinedTypes.valueOf( vmTypeName.toUpperCase( ).replace( ".", "" ) );
              vmType.setCpu( defaultVmType.getCpu( ) );
              vmType.setDisk( defaultVmType.getDisk( ) );
              vmType.setMemory( defaultVmType.getMemory( ) );
            } else {
              vmType.setCpu( Objects.firstNonNull( request.getCpu( ), vmType.getCpu( ) ) );
              vmType.setDisk( Objects.firstNonNull( request.getDisk( ), vmType.getDisk( ) ) );
              vmType.setMemory( Objects.firstNonNull( request.getMemory( ), vmType.getMemory( ) ) );
            }
            //GRZE:TODO:EUCA-3500 do the appropriate sanity checks here.
            VmTypes.update( vmType );
            return vmType;
          } catch ( NoSuchMetadataException ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      };
      try {
        final VmType before = VmTypes.lookup( request.getName( ) );
        if ( !RestrictedTypes.filterPrivileged().apply( before ) ) {
          throw new EucalyptusCloudException( "Authorization failed." );
        }
        final VmType after = Entities.asTransaction( modifyFunc ).apply( request.getName( ) );
        reply.setPreviousInstanceType( TypeMappers.transform(  before, VmTypeDetails.class ) );
        reply.setInstanceType( TypeMappers.transform(  after, VmTypeDetails.class ) );
      } catch ( NoSuchMetadataException ex ) {
        throw new EucalyptusCloudException( "Failed to lookup the requested instance type: " + request.getName( ), ex );
      }
    } else {
      throw new EucalyptusCloudException( "Authorization failed." );
    }
    return reply;
  }
}
