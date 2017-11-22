/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.vmtypes;

import javax.annotation.Nullable;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.cluster.common.ResourceState.VmTypeAvailability;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesResponseType;
import com.eucalyptus.compute.common.backend.DescribeInstanceTypesType;
import com.eucalyptus.compute.common.backend.ModifyInstanceTypeAttributeResponseType;
import com.eucalyptus.compute.common.backend.ModifyInstanceTypeAttributeType;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.compute.common.VmTypeEphemeralDisk;
import com.eucalyptus.compute.common.VmTypeZoneStatus;
import com.eucalyptus.compute.common.internal.vmtypes.EphemeralDisk;
import com.eucalyptus.compute.common.internal.vmtypes.VmType;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.vmtypes.VmTypes.PredefinedTypes;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;

@ComponentNamed("computeVmTypesManager")
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
  
  public DescribeInstanceTypesResponseType DescribeInstanceTypes( final DescribeInstanceTypesType request ) throws ClientComputeException {
    DescribeInstanceTypesResponseType reply = request.getReply( );
    final boolean administrator = Contexts.lookup( ).isAdministrator( );
    for ( final VmType v : Iterables.filter( VmTypes.list( ), CloudMetadatas.filteringFor( VmType.class ).byId( request.getInstanceTypes( ) ).byPrivileges( ).buildPredicate( ) ) ) {
      VmTypeDetails vmTypeDetails = new VmTypeDetails( ) {
        {
          this.setName( v.getName( ) );
          this.setDisk( v.getDisk( ) );
          this.setCpu( v.getCpu( ) );
          this.setMemory( v.getMemory( ) );
          this.setNetworkInterfaces( v.getNetworkInterfaces( ) );
          if ( request.getVerbose( ) ) {
            for ( EphemeralDisk e : v.getEphemeralDisks() ) {
              this.getEphemeralDisk( ).add( new VmTypeEphemeralDisk( e.getDiskName( ), e.getDeviceName( ), e.getSize( ), e.getFormat( ).name( ) ) );
            }
          }
          if ( request.getAvailability( ) ) {
            if ( !administrator ) {
              throw new ClientComputeException( "AuthFailure", "Not permitted to access availability" );
            }
            for ( ServiceConfiguration cc : Topology.enabledServices( ClusterController.class ) ) {
              VmTypeAvailability available = Clusters.lookupAny( cc ).getNodeState( ).getAvailability( v );
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
              vmType.setNetworkInterfaces( defaultVmType.getEthernetInterfaceLimit( ) );
            } else {
              vmType.setCpu( MoreObjects.firstNonNull( request.getCpu( ), vmType.getCpu( ) ) );
              vmType.setDisk( MoreObjects.firstNonNull( request.getDisk( ), vmType.getDisk( ) ) );
              vmType.setMemory( MoreObjects.firstNonNull( request.getMemory( ), vmType.getMemory( ) ) );
              vmType.setNetworkInterfaces( MoreObjects.firstNonNull( request.getNetworkInterfaces( ), vmType.getNetworkInterfaces( ) ) );
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
