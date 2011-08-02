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
 *    THE REGENTS DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cloud.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.Image;
import com.eucalyptus.cloud.run.Allocations.Allocation;
import com.eucalyptus.cloud.util.IllegalMetadataAccessException;
import com.eucalyptus.cloud.util.InvalidMetadataException;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.cloud.util.VerificationException;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.context.Context;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.NetworkRulesGroup;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class VerifyMetadata {
  private static Logger LOG = Logger.getLogger( VerifyMetadata.class );
  
  interface MetadataVerifier {
    public abstract boolean apply( Allocation allocInfo ) throws MetadataException;
  }
  
  private static final ArrayList<? extends MetadataVerifier> verifiers = Lists.newArrayList( VmTypeVerifier.INSTANCE, PartitionVerifier.INSTANCE,
                                                                                                ImageVerifier.INSTANCE, KeyPairVerifier.INSTANCE,
                                                                                                NetworkGroupVerifier.INSTANCE );
  
  public static Allocation handle( RunInstancesType request ) throws MetadataException {
    Allocation alloc = Allocations.begin( request );
    for ( MetadataVerifier v : verifiers ) {
      v.apply( alloc );
    }
    return alloc;
  }
  
  enum VmTypeVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      Context ctx = allocInfo.getContext( );
      User user = ctx.getUser( );
      String instanceType = allocInfo.getRequest( ).getInstanceType( );
      String action = PolicySpec.requestToAction( allocInfo.getRequest( ) );
      try {
        if ( !ctx.hasAdministrativePrivileges( )
             && !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_VMTYPE, instanceType, user.getAccount( ), action, user ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to allocate vm type " + instanceType + " for " + ctx.getUserFullName( ) );
        }
      } catch ( AuthException ex ) {
        LOG.error( ex, ex );
        throw new IllegalMetadataAccessException( "Not authorized to allocate vm type " + instanceType + " for " + ctx.getUserFullName( ) );
      }
      allocInfo.setVmType( VmTypes.getVmType( instanceType ) );
      return true;
    }
  }
  
  enum PartitionVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      RunInstancesType request = allocInfo.getRequest( );
      String zoneName = request.getAvailabilityZone( );
      if ( Clusters.getInstance( ).listValues( ).isEmpty( ) ) {
        LOG.debug( "enabled values: " + Joiner.on( "\n" ).join( Clusters.getInstance( ).listValues( ) ) );
        LOG.debug( "disabled values: " + Joiner.on( "\n" ).join( Clusters.getInstance( ).listValues( ) ) );
        throw new VerificationException( "Not enough resources: no cluster controller is currently available to run instances." );
      } else if ( Partitions.exists( zoneName ) ) {
        Partition partition = Partitions.lookupService( ClusterController.class, zoneName ).lookupPartition( );
        allocInfo.setPartition( partition );
      } else if ( "default".equals( zoneName ) ) {
        String defaultZone = Clusters.getInstance( ).listValues( ).get( 0 ).getPartition( );
        Partition partition = Partitions.lookupService( ClusterController.class, defaultZone ).lookupPartition( );
        allocInfo.setPartition( partition );
      } else {
        throw new VerificationException( "Not enough resources: no cluster controller is currently available to run instances." );
      }
      return true;
    }
  }
  
  enum ImageVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      RunInstancesType msg = allocInfo.getRequest( );
      String imageId = msg.getImageId( );
      VmType vmType = allocInfo.getVmType( );
      Partition partition = allocInfo.getPartition( );
      try {
        BootableSet bootSet = Emis.newBootableSet( vmType, partition, imageId );
        allocInfo.setBootableSet( bootSet );
      } catch ( AuthException ex ) {
        LOG.error( ex );
        throw new VerificationException( ex );
      } catch ( MetadataException ex ) {
        LOG.error( ex );
        throw new VerificationException( ex );
      }
      return true;
    }
  }
  
  enum KeyPairVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      if ( allocInfo.getRequest( ).getKeyName( ) == null || "".equals( allocInfo.getRequest( ).getKeyName( ) ) ) {
        if ( Image.Platform.windows.name( ).equals( allocInfo.getBootSet( ).getMachine( ).getPlatform( ) ) ) {
          throw new InvalidMetadataException( "You must specify a keypair when running a windows vm: " + allocInfo.getRequest( ).getImageId( ) );
        } else {
          allocInfo.setSshKeyPair( KeyPairs.noKey( ) );
          return true;
        }
      }
      Context ctx = allocInfo.getContext( );
      RunInstancesType request = allocInfo.getRequest( );
      String keyName = request.getKeyName( );
      if ( !ctx.hasAdministrativePrivileges( )
           && !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_KEYPAIR, keyName, ctx.getAccount( ),
                                         PolicySpec.requestToAction( request ), ctx.getUser( ) ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use keypair " + keyName + " by " + ctx.getUser( ).getName( ) );
      }
      allocInfo.setSshKeyPair( KeyPairs.lookup( ctx.getUserFullName( ), keyName ) );
      return true;
    }
  }
  
  enum NetworkGroupVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      Context ctx = allocInfo.getContext( );
      NetworkGroups.createDefault( ctx.getUserFullName( ) );
      
      Set<String> networkNames = Sets.newHashSet( allocInfo.getRequest( ).getGroupSet( ) );
      if ( networkNames.isEmpty( ) ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) );
      }
      
      for ( String groupName : networkNames ) {
        if ( !ctx.hasAdministrativePrivileges( )
             && !Permissions.isAuthorized( PolicySpec.VENDOR_EC2, PolicySpec.EC2_RESOURCE_SECURITYGROUP, groupName, ctx.getAccount( ),
                                           PolicySpec.requestToAction( allocInfo.getRequest( ) ), ctx.getUser( ) ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " + groupName + " for " + ctx.getUser( ).getName( ) );
        }
      }
      
      Map<String, NetworkRulesGroup> networkRuleGroups = Maps.newHashMap( );
      for ( String groupName : networkNames ) {
        NetworkRulesGroup group = NetworkGroups.lookup( ctx.getUserFullName( ), groupName );
        networkRuleGroups.put( groupName, group );
      }
      Set<String> missingNets = Sets.difference( networkNames, networkRuleGroups.keySet( ) );
      if ( !missingNets.isEmpty( ) ) {
        throw new NoSuchMetadataException( "Failed to find security group info for: " + missingNets );
      } else {
        allocInfo.setNetworkRules( networkRuleGroups );
      }
      return true;
    }
  }
  
}
