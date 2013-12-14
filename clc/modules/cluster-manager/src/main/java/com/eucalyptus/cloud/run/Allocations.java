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
 ************************************************************************/

package com.eucalyptus.cloud.run;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.cloud.ResourceToken;
import com.eucalyptus.cloud.util.MetadataException;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.component.Partition;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.images.Emis;
import com.eucalyptus.images.Emis.BootableSet;
import com.eucalyptus.keys.KeyPairs;
import com.eucalyptus.keys.SshKeyPair;
import com.eucalyptus.network.ExtantNetwork;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.UniqueIds;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vmtypes.VmType;
import com.eucalyptus.vmtypes.VmTypes;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.HasRequest;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class Allocations {
  private static Logger LOG = Logger.getLogger( Allocations.class );
  
  public static class Allocation implements HasRequest {
    /** to be eliminated **/
    private final Context              context;
    private final RunInstancesType     request;
    /** values determined by the request **/
    private final UserFullName         ownerFullName;
    private byte[]                     userData;
    private final int                  minCount;
    private final int                  maxCount;
    private final boolean              usePrivateAddressing;
    private final boolean              monitoring;
    @Nullable
    private final String               clientToken;

    /** verified references determined by the request **/
    private Partition                  partition;
    private SshKeyPair                 sshKeyPair;
    private BootableSet                bootSet;
    private VmType                     vmType;
    private NetworkGroup               primaryNetwork;
    private Map<String, NetworkGroup>  networkGroups;
    private String iamInstanceProfileArn;
    private String iamInstanceProfileId;
    private String iamRoleArn;

    /** intermediate allocation state **/
    private final String               reservationId;
    private final List<ResourceToken>  allocationTokens  = Lists.newArrayList( );
    private final Long                 reservationIndex;
    private final Map<Integer, String> instanceIds;
    private final Map<Integer, String> instanceUuids;
    private Date                       expiration;
    
    private Allocation( final RunInstancesType request ) {
      this.context = Contexts.lookup( );
      this.instanceIds = Maps.newHashMap( );
      this.instanceUuids = Maps.newHashMap( );
      this.request = request;
      this.minCount = request.getMinCount( );
      this.maxCount = request.getMaxCount( );
      this.usePrivateAddressing = "private".equals(request.getAddressingType());
      this.monitoring = request.getMonitoring() == null ? Boolean.FALSE : request.getMonitoring();
      this.clientToken = Strings.emptyToNull( request.getClientToken( ) );
     
      this.ownerFullName = this.context.getUserFullName( );
      if ( ( this.request.getInstanceType( ) == null ) || "".equals( this.request.getInstanceType( ) ) ) {
        this.request.setInstanceType( VmTypes.defaultTypeName( ) );
      }

      this.reservationIndex = UniqueIds.nextIndex( VmInstance.class, ( long ) request.getMaxCount( ) );
      this.reservationId = VmInstances.getId( this.reservationIndex, 0 ).replaceAll( "i-", "r-" );
      this.request.setMonitoring(this.monitoring);
      //GRZE:FIXME: moved all this encode/decode junk into util.UserDatas
      if ( this.request.getUserData( ) != null ) {
        try {
          this.userData = Base64.decode( this.request.getUserData( ) );
          this.request.setUserData( new String( Base64.encode( this.userData ) ) );
        } catch ( Exception e ) {}
      } else {
        try {
          this.request.setUserData( new String( Base64.encode(  new byte[0] ) ) );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    }
    
    private Allocation( final String reservationId,
                        final String instanceId,
                        final String instanceUuid,
                        final byte[] userData,
                        final Date expiration,
                        final Partition partition,
                        final SshKeyPair sshKeyPair,
                        final BootableSet bootSet,
                        final VmType vmType,
                        final Set<NetworkGroup> networkGroups,
                        final boolean isUsePrivateAddressing, 
                        final boolean monitoring,
                        final String clientToken,
                        final String iamInstanceProfileArn,
                        final String iamInstanceProfileId,
                        final String iamRoleArn
                        ) {
      this.context = Contexts.lookup( );
      this.minCount = 1;
      this.maxCount = 1;
      this.usePrivateAddressing = isUsePrivateAddressing;
      this.ownerFullName = this.context.getUserFullName( );
      this.reservationId = reservationId;
      this.reservationIndex = UniqueIds.nextIndex( VmInstance.class, (long) this.maxCount );
      this.instanceIds = Maps.newHashMap();
      this.instanceIds.put( 0, instanceId );
      this.instanceUuids = Maps.newHashMap();
      this.instanceUuids.put( 0, instanceUuid );
      this.userData = userData;
      this.partition = partition;
      this.sshKeyPair = ( sshKeyPair != null ? sshKeyPair : KeyPairs.noKey( ) );
      this.bootSet = bootSet;
      this.expiration = expiration;
      this.vmType = vmType;
      this.monitoring = monitoring;
      this.clientToken = clientToken;
      this.iamInstanceProfileArn = iamInstanceProfileArn;
      this.iamInstanceProfileId = iamInstanceProfileId;
      this.iamRoleArn = iamRoleArn;

      this.networkGroups = new HashMap<String, NetworkGroup>( ) {
        {
          for ( NetworkGroup g : networkGroups ) {
            if ( Allocation.this.primaryNetwork == null ) {
              Allocation.this.primaryNetwork = g;
            }
            put( g.getDisplayName( ), g );
          }
        }
      };
      this.request = new RunInstancesType( ) {
        {
          this.setMinCount( 1 );
          this.setMaxCount( 1 );
          this.setImageId( bootSet.getMachine().getDisplayName() );
          this.setAvailabilityZone( partition.getName() );
          this.getGroupSet( ).addAll( Allocation.this.networkGroups.keySet() );
          this.setInstanceType( vmType.getName() );
        }
      };
      
    }
    
    @Override
    public RunInstancesType getRequest( ) {
      return this.request;
    }
    
    public NetworkGroup getPrimaryNetwork( ) {
      return this.primaryNetwork;
    }
    
    public ExtantNetwork getExtantNetwork( ) {
      final EntityTransaction db = Entities.get( ExtantNetwork.class );
      try {
        final NetworkGroup net = Entities.merge( this.primaryNetwork );
        final ExtantNetwork ex = net.extantNetwork( );
        db.commit( );
        return ex;
      } catch ( final TransientEntityException ex ) {
        LOG.error( ex, ex );
        db.rollback( );
        throw new RuntimeException( ex );
      } catch ( final NotEnoughResourcesException ex ) {
        db.rollback( );
        return ExtantNetwork.bogus( this.getPrimaryNetwork( ) );
      }
    }
    
    public void commit( ) throws Exception {
      try {
        for ( final ResourceToken t : this.getAllocationTokens( ) ) {
          VmInstance.Create.INSTANCE.apply( t );
        }
      } catch ( final Exception ex ) {
        this.abort( );
        throw ex;
      }
    }
    
    public void abort( ) {
      for ( final ResourceToken token : this.allocationTokens ) {
        LOG.error( "Aborting resource token: " + token, new RuntimeException( ) );
        final EntityTransaction db = Entities.get( VmInstance.class );
        try {
          token.abort( );
          db.commit( );
        } catch ( final Exception ex ) {
          LOG.warn( ex.getMessage( ) );
          Logs.exhaust( ).error( ex, ex );
          db.rollback( );
        }
      }
    }
    
    public List<NetworkGroup> getNetworkGroups( ) {
      return Lists.newArrayList( this.networkGroups.values( ) );
    }

    public TreeMap<String, String> getNetworkGroupsMap( ) {

      TreeMap<String,String> networkGroupMap = Maps.newTreeMap();

      for (NetworkGroup network : this.getNetworkGroups() ){
        networkGroupMap.put(network.getGroupId(), network.getDisplayName());
      }

      return networkGroupMap;

    }

    public VmType getVmType( ) {
      return this.vmType;
    }
    
    public Partition getPartition( ) {
      return this.partition;
    }
    
    public void setBootableSet( final BootableSet bootSet ) {
      this.bootSet = bootSet;
    }
    
    public void setVmType( final VmType vmType ) {
      this.vmType = vmType;
    }
    
    public UserFullName getOwnerFullName( ) {
      return this.ownerFullName;
    }
    
    public List<ResourceToken> getAllocationTokens( ) {
      return this.allocationTokens;
    }
    
    public byte[] getUserData( ) {
      return this.userData;
    }
    
    public Long getReservationIndex( ) {
      return this.reservationIndex;
    }
    
    public String getReservationId( ) {
      return this.reservationId;
    }
    
    public BootableSet getBootSet( ) {
      return this.bootSet;
    }
    
    public Context getContext( ) {
      return this.context;
    }
    
    public void setPartition( final Partition partition2 ) {
      this.partition = partition2;
    }
    
    public SshKeyPair getSshKeyPair( ) {
      return this.sshKeyPair;
    }
    
    public void setSshKeyPair( final SshKeyPair sshKeyPair ) {
      this.sshKeyPair = sshKeyPair;
    }
    
    public void setNetworkRules( final Map<String, NetworkGroup> networkRuleGroups ) {
      final Entry<String, NetworkGroup> ent = networkRuleGroups.entrySet( ).iterator( ).next( );
      this.primaryNetwork = ent.getValue( );
      this.networkGroups = networkRuleGroups;
    }
    
    public VmTypeInfo getVmTypeInfo( ) throws MetadataException {
      return this.bootSet.populateVirtualBootRecord( this.vmType );
    }

    @Nullable
    public String getIamInstanceProfileArn() {
      return iamInstanceProfileArn;
    }

    public void setInstanceProfileArn( final String instanceProfileArn ) {
      this.iamInstanceProfileArn = instanceProfileArn;
    }

    public String getIamInstanceProfileId() {
      return iamInstanceProfileId;
    }

    public void setIamInstanceProfileId( final String iamInstanceProfileId ) {
      this.iamInstanceProfileId = iamInstanceProfileId;
    }

    public String getIamRoleArn() {
      return iamRoleArn;
    }

    public void setIamRoleArn( final String iamRoleArn ) {
      this.iamRoleArn = iamRoleArn;
    }

    public int getMinCount( ) {
      return this.minCount;
    }
    
    public int getMaxCount( ) {
      return this.maxCount;
    }

    public boolean isUsePrivateAddressing() {
      return usePrivateAddressing;
    }

    public final boolean isMonitoring() {
        return monitoring;
    }

    @Nullable
    public String getClientToken( ) {
      return clientToken;
    }

    @Nullable
    public String getUniqueClientToken( @Nonnull final Integer launchIndex ) {
      return clientToken == null ?
          null :
          String.format( "%s:%d:%s", getOwnerFullName( ).getAccountNumber( ), launchIndex, clientToken );
    }

    public String getInstanceId( int index ) {
      while ( this.instanceIds.size( ) <= index ) {
        this.instanceIds.put( index, VmInstances.getId( ( long ) this.getReservationIndex( ), index ) );
      }
      return this.instanceIds.get( index );
    }

    public String getInstanceUuid( int index ) {
      while ( this.instanceUuids.size( ) <= index ) {
        this.instanceUuids.put( index, UUID.randomUUID( ).toString( ) );
      }
      return this.instanceUuids.get( index );
    }
    
    public Date getExpiration( ) {
      return this.expiration;
    }

    public void setExpiration(final Date expiration) {
      this.expiration = expiration;
    }
  }
  
  public static Allocation run( final RunInstancesType request ) {
    return new Allocation( request );
  }
  
  public static Allocation start( final VmInstance vm ) {
    BootableSet bootSet = Emis.recreateBootableSet( vm );
    return new Allocation( vm.getReservationId( ),
                           vm.getInstanceId( ),
                           vm.getInstanceUuid( ),
                           vm.getUserData( ),
                           vm.getExpiration( ),
                           vm.lookupPartition( ),
                           vm.getKeyPair( ),
                           bootSet,
                           vm.getVmType( ),
                           vm.getNetworkGroups( ),
                           vm.isUsePrivateAddressing(),
                           vm.getMonitoring(),
                           vm.getClientToken(),
                           vm.getIamInstanceProfileArn(),
                           vm.getIamInstanceProfileId(),
                           vm.getIamRoleArn() );
  }
}
