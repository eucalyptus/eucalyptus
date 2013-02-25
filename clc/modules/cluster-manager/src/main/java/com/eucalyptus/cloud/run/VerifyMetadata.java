/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.cloud.run;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloud.ImageMetadata;
import com.eucalyptus.cloud.ImageMetadata.Platform;
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
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class VerifyMetadata {
  private static Logger LOG = Logger.getLogger( VerifyMetadata.class );
  public static Predicate<Allocation> get( ) {
    return Predicates.and( Lists.transform( verifiers, AsPredicate.INSTANCE ) );
  }
  

  private interface MetadataVerifier {
    public abstract boolean apply( Allocation allocInfo ) throws MetadataException, AuthException;
  }
  
  private static final ArrayList<? extends MetadataVerifier> verifiers = Lists.newArrayList( VmTypeVerifier.INSTANCE, PartitionVerifier.INSTANCE,
                                                                                                ImageVerifier.INSTANCE, KeyPairVerifier.INSTANCE,
                                                                                                NetworkGroupVerifier.INSTANCE );
  
  private enum AsPredicate implements Function<MetadataVerifier, Predicate<Allocation>> {
    INSTANCE;
    @Override
    public Predicate<Allocation> apply( final MetadataVerifier arg0 ) {
      return new Predicate<Allocation>( ) {
        
        @Override
        public boolean apply( Allocation allocInfo ) {
          try {
            return arg0.apply( allocInfo );
          } catch ( Exception ex ) {
            throw Exceptions.toUndeclared( ex );
          }
        }
      };
    }
  }
  
  enum VmTypeVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      Context ctx = allocInfo.getContext( );
      User user = ctx.getUser( );
      String instanceType = allocInfo.getRequest( ).getInstanceType( );
      VmType vmType = VmTypes.lookup( instanceType );
      if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( vmType ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to allocate vm type " + instanceType + " for " + ctx.getUserFullName( ) );
      }
      allocInfo.setVmType( vmType );
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
        Partition partition = Partitions.lookupByName( zoneName );
        allocInfo.setPartition( partition );
      } else if ( Partition.DEFAULT_NAME.equals( zoneName ) ) {
        Partition partition = Partition.DEFAULT;
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
    public boolean apply( Allocation allocInfo ) throws MetadataException, AuthException {
      RunInstancesType msg = allocInfo.getRequest( );
      String imageId = msg.getImageId( );
      VmType vmType = allocInfo.getVmType( );
      try {
        BootableSet bootSet = Emis.newBootableSet( imageId );
        allocInfo.setBootableSet( bootSet );
        
        // Add (1024L * 1024L * 10) to handle NTFS min requirements.
        if ( bootSet.isBlockStorage( ) ) {
        } else if ( Platform.windows.equals( bootSet.getMachine( ).getPlatform( ) ) && bootSet.getMachine( ).getImageSizeBytes( ) > ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) + ( 1024L * 1024L * 10 ) ) ) {
          throw new VerificationException( "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
                                           " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
                                           " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) + " size " + vmType.getDisk( )
                                           + " GB." );
        } else if ( bootSet.getMachine( ).getImageSizeBytes( ) >= ( ( 1024L * 1024L * 1024L * vmType.getDisk( ) ) ) ) {
            throw new VerificationException( "Unable to run instance " + bootSet.getMachine( ).getDisplayName( ) +
                    " in which the size " + bootSet.getMachine( ).getImageSizeBytes( ) +
                    " bytes of the instance is greater than the vmType " + vmType.getDisplayName( ) + " size " + vmType.getDisk( )
                    + " GB." );
        }
      } catch ( MetadataException ex ) {
        LOG.error( ex );
        throw ex;
      } catch ( RuntimeException ex ) {
        LOG.error( ex );
        throw new VerificationException( "Failed to verify references for request: " + msg.toSimpleString( ) + " because of: " + ex.getMessage( ), ex );
      }
      return true;
    }
  }
  
  enum KeyPairVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      if ( allocInfo.getRequest( ).getKeyName( ) == null || "".equals( allocInfo.getRequest( ).getKeyName( ) ) ) {
        if ( ImageMetadata.Platform.windows.equals( allocInfo.getBootSet( ).getMachine( ).getPlatform( ) ) ) {
          throw new InvalidMetadataException( "You must specify a keypair when running a windows vm: " + allocInfo.getRequest( ).getImageId( ) );
        } else {
          allocInfo.setSshKeyPair( KeyPairs.noKey( ) );
          return true;
        }
      }
      Context ctx = allocInfo.getContext( );
      RunInstancesType request = allocInfo.getRequest( );
      String keyName = request.getKeyName( );
      SshKeyPair key = KeyPairs.lookup( ctx.getUserFullName( ).asAccountFullName( ), keyName );
      if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( key ) ) {
        throw new IllegalMetadataAccessException( "Not authorized to use keypair " + keyName + " by " + ctx.getUser( ).getName( ) );
      }
      allocInfo.setSshKeyPair( key );
      return true;
    }
  }
  
  enum NetworkGroupVerifier implements MetadataVerifier {
    INSTANCE;
    
    @Override
    public boolean apply( Allocation allocInfo ) throws MetadataException {
      Context ctx = allocInfo.getContext( );
      NetworkGroups.lookup( ctx.getUserFullName( ).asAccountFullName( ), NetworkGroups.defaultNetworkName( ) );
      
      Set<String> networkNames = Sets.newHashSet( allocInfo.getRequest( ).getGroupSet( ) );
      if ( networkNames.isEmpty( ) ) {
        networkNames.add( NetworkGroups.defaultNetworkName( ) );
      }
      
      Map<String, NetworkGroup> networkRuleGroups = Maps.newHashMap( );
      for ( String groupName : networkNames ) {
        NetworkGroup group = NetworkGroups.lookup( ctx.getUserFullName( ).asAccountFullName( ), groupName );
        if ( !ctx.hasAdministrativePrivileges( ) && !RestrictedTypes.filterPrivileged( ).apply( group ) ) {
          throw new IllegalMetadataAccessException( "Not authorized to use network group " + groupName + " for " + ctx.getUser( ).getName( ) );
        }
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
