/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.vm;

import static com.eucalyptus.util.Strings.isPrefixOf;
import static com.eucalyptus.util.Strings.upper;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.BaseInstanceProfile;
import com.eucalyptus.auth.principal.BaseRole;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Tokens;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.images.BlockStorageImageInfo;
import com.eucalyptus.compute.common.internal.images.MachineImageInfo;
import com.eucalyptus.compute.common.internal.network.NetworkGroup;
import com.eucalyptus.compute.common.internal.vm.VmEphemeralAttachment;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vm.VmVolumeAttachment;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.images.ImageManager;
import com.eucalyptus.tokens.AssumeRoleResponseType;
import com.eucalyptus.tokens.AssumeRoleType;
import com.eucalyptus.tokens.CredentialsType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import net.sf.json.JSONObject;

/**
 *
 */
public class VmInstanceMetadata {

  private static final Logger LOG = Logger.getLogger( VmInstanceMetadata.class );

  private static final com.google.common.cache.Cache<MetadataKey,ImmutableMap<String,String>> metadataCache =
      CacheBuilder.newBuilder().expireAfterWrite( 5, TimeUnit.MINUTES ).maximumSize( 1000 ).build( );

  public static String getByKey( final VmInstance vm, final String pathArg ) {
    final String path = Objects.firstNonNull( pathArg, "" );
    final String pathNoSlash;
    LOG.debug( "Servicing metadata request:" + path );
    if ( path.endsWith( "/" ) ) {
      pathNoSlash = path.substring( 0, path.length() -1 );
    } else {
      pathNoSlash = path;
    }

    Optional<MetadataGroup> groupOption = Optional.absent();
    for ( final MetadataGroup metadataGroup : MetadataGroup.values() ) {
      if ( metadataGroup.providesPath( pathNoSlash ) ||
          metadataGroup.providesPath( path ) ) {
        groupOption = Optional.of( metadataGroup );
      }
    }
    final MetadataGroup group = groupOption.or( MetadataGroup.Core );
    final Map<String,String> metadataMap =
        Optional.fromNullable( group.apply( vm ) ).or( Collections.<String, String>emptyMap() );
    final String value = metadataMap.get( path );
    return value == null ? metadataMap.get( pathNoSlash ) : value;
  }

  private static Map<String, String> getCoreMetadataMap( final VmInstance vm ) {
    final boolean dns = StackConfiguration.USE_INSTANCE_DNS && !ComponentIds.lookup( Dns.class ).runLimitedServices( );
    final Map<String, String> m = Maps.newHashMap();
    m.put( "ami-id", vm.getImageId() );
    if ( vm.getBootRecord( ).getMachine() != null && !vm.getBootRecord( ).getMachine().getProductCodes( ).isEmpty( ) ) {
      m.put( "product-codes", Joiner.on( '\n' ).join( vm.getBootRecord( ).getMachine().getProductCodes( ) ) );
    }
    m.put( "ami-launch-index", "" + vm.getLaunchIndex( ) );
//ASAP: FIXME: GRZE:
//    m.put( "ancestor-ami-ids", this.getImageInfo( ).getAncestorIds( ).toString( ).replaceAll( "[\\Q[]\\E]", "" ).replaceAll( ", ", "\n" ) );
    if ( vm.getBootRecord( ).getMachine() instanceof MachineImageInfo ) {
      m.put( "ami-manifest-path", ( ( MachineImageInfo ) vm.getBootRecord( ).getMachine() ).getManifestLocation( ) );
    }
    if ( dns ) {
      m.put( "hostname", vm.getPrivateDnsName() );
    } else {
      m.put( "hostname", vm.getPrivateAddress() );
    }
    m.put( "instance-id", vm.getInstanceId() );
    m.put( "instance-type", vm.getVmType().getName( ) );
    if ( dns ) {
      m.put( "local-hostname", vm.getPrivateDnsName() );
    } else {
      m.put( "local-hostname", vm.getPrivateAddress() );
    }
    m.put( "local-ipv4", vm.getPrivateAddress() );
    m.put( "mac", upper().apply( vm.getMacAddress() ) );
    if ( dns ) {
      m.put( "public-hostname", vm.getPublicDnsName() );
    } else {
      m.put( "public-hostname", vm.getPublicAddress() );
    }
    m.put( "public-ipv4", vm.getPublicAddress() );
    m.put( "reservation-id", vm.getReservationId( ) );
    if ( vm.getKernelId() != null ) {
      m.put( "kernel-id", vm.getKernelId() );
    }
    if ( vm.getRamdiskId() != null ) {
      m.put( "ramdisk-id", vm.getRamdiskId() );
    }
    m.put( "security-groups", Joiner.on('\n').join( Sets.newTreeSet( Iterables.transform( vm.getNetworkGroups(), CloudMetadatas.toDisplayName() ) ) ) );
    m.put( "services/domain", DomainNames.externalSubdomain( ).relativize( Name.root ).toString( ) );
    m.put( "placement/availability-zone", vm.getPartition() );
    return m;
  }

  private static Map<String, String> getNetworkMetadataMap( final VmInstance vm ) {
    final Map<String, String> m = Maps.newHashMap( );
    if ( !vm.getNetworkInterfaces( ).isEmpty( ) ) for ( final NetworkInterface networkInterface : vm.getNetworkInterfaces( ) ) {
      final String prefix = "network/interfaces/macs/" + networkInterface.getMacAddress( ) + "/";
      m.put( prefix + "device-number", String.valueOf( networkInterface.getAttachment( ).getDeviceIndex( ) ) );
      m.put( prefix + "interface-id", networkInterface.getDisplayName( ) );
      if ( networkInterface.isAssociated( ) ) {
        m.put(
            prefix + "ipv4-associations/" + networkInterface.getAssociation( ).getPublicIp( ),
            networkInterface.getPrivateIpAddress( ) );
      }
      m.put( prefix + "local-hostname", networkInterface.getPrivateDnsName( ) );
      m.put( prefix + "local-ipv4s", networkInterface.getPrivateIpAddress( ) );
      m.put( prefix + "mac", networkInterface.getMacAddress( ) );
      m.put( prefix + "owner-id", networkInterface.getOwnerAccountNumber( ) );
      if ( networkInterface.isAssociated( ) ) {
        m.put( prefix + "public-hostname", networkInterface.getAssociation( ).getPublicDnsName( ) );
        m.put( prefix + "public-ipv4s", networkInterface.getAssociation( ).getPublicIp( ) );
      }
      m.put( prefix + "security-groups", Joiner.on( '\n' ).join( Iterables.transform( networkInterface.getNetworkGroups( ), RestrictedTypes.toDisplayName( ) ) ) );
      m.put( prefix + "security-group-ids", Joiner.on( '\n' ).join( Iterables.transform( networkInterface.getNetworkGroups( ), NetworkGroup.groupId( ) ) ) );
      m.put( prefix + "subnet-id", networkInterface.getSubnet( ).getDisplayName( ) );
      m.put( prefix + "subnet-ipv4-cidr-block", networkInterface.getSubnet( ).getCidr( ) );
      m.put( prefix + "vpc-id", networkInterface.getVpc( ).getDisplayName( ) );
      m.put( prefix + "vpc-ipv4-cidr-block", networkInterface.getVpc( ).getCidr( ) );
    } else { // EC2-Classic instance
      final boolean dns = StackConfiguration.USE_INSTANCE_DNS && !ComponentIds.lookup( Dns.class ).runLimitedServices( );
      final String prefix = "network/interfaces/macs/" + vm.getMacAddress( ) + "/";
      m.put( prefix + "device-number", "0" );
      if ( dns ) {
        m.put( prefix + "local-hostname", vm.getPrivateDnsName() );
      } else {
        m.put( prefix + "local-hostname", vm.getPrivateAddress() );
      }
      m.put( prefix + "local-ipv4s", vm.getPrivateAddress( ) );
      m.put( prefix + "mac", vm.getMacAddress( ) );
      m.put( prefix + "owner-id", vm.getOwnerAccountNumber( ) );
      if ( dns ) {
        m.put( prefix + "public-hostname", vm.getPublicDnsName() );
      } else {
        m.put( prefix + "public-hostname", vm.getPublicAddress() );
      }
      m.put( prefix + "public-ipv4s", vm.getPublicAddress( ) );
    }
    return m;
  }

  private static Map<String, String> getBlockDeviceMappingMetadataMap( final VmInstance vm ) {
    final Map<String, String> m = Maps.newHashMap( );
    // Metadata should accurately reflect all the ebs mappings and ephemeral mappings if any.
    // Fixes EUCA-4081, EUCA-3954 and implements EUCA-4786
    if( vm.getBootRecord().getMachine() instanceof BlockStorageImageInfo ) {
      // Get all the volume attachments and order them in some way (by device name for now)
      Set<VmVolumeAttachment> volAttachments = new TreeSet<VmVolumeAttachment>(VolumeAttachmentComparator.INSTANCE);
      volAttachments.addAll(vm.getBootRecord().getPersistentVolumes());

      // Keep track of all ebs keys for populating block-device-mapping list
      int ebsCount = 0;

      // Iterate through the list of volume attachments and populate ebs mappings
      for (VmVolumeAttachment attachment : volAttachments ) {
        if (attachment.getIsRootDevice()) {
          m.put( "block-device-mapping/ami", attachment.getShortDeviceName() );
          m.put( "block-device-mapping/emi", attachment.getShortDeviceName() );
          m.put( "block-device-mapping/root", attachment.getDevice() );
        }
        // add only volumes added at start up time and don't list root see EUCA-8636
        if (attachment.getAttachedAtStartup() && !attachment.getIsRootDevice())
          m.put( "block-device-mapping/ebs" + String.valueOf(++ebsCount), attachment.getShortDeviceName() );
      }

      // Using ephemeral attachments for bfebs instances only, can be extended to be used by all other instances
      // Get all the ephemeral attachments and order them in some way (by device name for now)
      Set<VmEphemeralAttachment> ephemeralAttachments = new TreeSet<VmEphemeralAttachment>(vm.getBootRecord().getEphemeralStorage());

      // Iterate through the list of ephemeral attachments and populate ephemeral mappings
      if (!ephemeralAttachments.isEmpty()) {
        for(VmEphemeralAttachment attachment : ephemeralAttachments){
          m.put( "block-device-mapping/" + attachment.getEphemeralId(), attachment.getShortDeviceName() );
        }
      }
    } else if (vm.getBootRecord().getMachine() instanceof MachineImageInfo) {
      MachineImageInfo mii = (MachineImageInfo) vm.getBootRecord().getMachine();
      String s = mii.getRootDeviceName();
      m.put( "block-device-mapping/emi", mii.getShortRootDeviceName() );
      m.put( "block-device-mapping/ami", mii.getShortRootDeviceName() );
      m.put( "block-device-mapping/root", s );
      if ( ImageManager.isPathAPartition( s )) {
        m.put( "block-device-mapping/ephemeral0", "sda2" );
        m.put( "block-device-mapping/swap", "sda3" );
      } else {
        m.put( "block-device-mapping/ephemeral0", "sdb" );
      }
    }
    return m;
  }

  private static Map<String, String> getIamMetadataMap( final VmInstance vm ) {
    final Map<String, String> m = new HashMap<>( );
    final String instanceProfileNameOrArn = vm.getIamInstanceProfileArn();
    if ( !Strings.isNullOrEmpty( instanceProfileNameOrArn ) ) {
      BaseInstanceProfile profile = null;
      String profileArn = null;
      String roleArn = vm.getIamRoleArn();
      String roleName = null;
      if ( !Strings.isNullOrEmpty( roleArn ) ) {
        roleName = roleArn.substring( roleArn.lastIndexOf('/') + 1 );
      } else try {
        String profileName;
        if ( instanceProfileNameOrArn.startsWith("arn:") ) {
          profileName = instanceProfileNameOrArn.substring( instanceProfileNameOrArn.lastIndexOf('/') + 1 );
        } else {
          profileName = instanceProfileNameOrArn;
        }
        profile = Accounts.lookupInstanceProfileByName( vm.getOwnerAccountNumber( ), profileName );
        profileArn = Accounts.getInstanceProfileArn( profile );
        if ( roleArn == null ) {
          final BaseRole role = profile.getRole();
          if ( role != null ) {
            roleArn = Accounts.getRoleArn( role );
            roleName = role.getName();
          }
        } else {
          // Authorized role from instance creation time must be used if present
          final EuareResourceName ern = (EuareResourceName) Ern.parse( roleArn );
          roleName = ern.getName();
        }
      } catch (AuthException e) {
        LOG.debug(e);
      }

      CredentialsType credentials = null;
      if ( roleArn != null ) {
        final AssumeRoleType assumeRoleType = new AssumeRoleType( );
        assumeRoleType.setRoleArn(roleArn);
        assumeRoleType.setRoleSessionName( Crypto.generateId( vm.getOwner().getUserId() ));

        ServiceConfiguration serviceConfiguration = Topology.lookup( Tokens.class );
        try {
          credentials = ((AssumeRoleResponseType) AsyncRequests.sendSync( serviceConfiguration, assumeRoleType ))
              .getAssumeRoleResult().getCredentials();
        } catch (Exception e) {
          LOG.debug("Unable to send assume role request to token service",e);
        }
      }

      if ( profile != null ) {
        m.put("iam/info/last-updated-date", Timestamps.formatIso8601Timestamp( new Date() ) );
        m.put("iam/info/instance-profile-arn", profileArn );
        m.put("iam/info/instance-profile-id", profile.getInstanceProfileId() );
      }

      if ( roleName != null && credentials != null ) {
        final String jsonCredentials = new JSONObject( )
            .element( "Code", "Success" )
            .element( "LastUpdated", Timestamps.formatIso8601Timestamp( new Date( ) ) )
            .element( "Type", "AWS-HMAC" )
            .element( "AccessKeyId", credentials.getAccessKeyId( ) )
            .element( "SecretAccessKey", credentials.getSecretAccessKey( ) )
            .element( "Token", credentials.getSessionToken( ) )
            .element( "Expiration", Timestamps.formatIso8601Timestamp( credentials.getExpiration( ) ) )
            .toString( 2 );

        m.put("iam/security-credentials/" + roleName + "/AccessKeyId", credentials.getAccessKeyId());
        m.put("iam/security-credentials/" + roleName + "/Expiration",Timestamps.formatIso8601Timestamp(credentials.getExpiration()));
        m.put("iam/security-credentials/" + roleName + "/SecretAccessKey", credentials.getSecretAccessKey());
        m.put("iam/security-credentials/" + roleName + "/Token", credentials.getSessionToken());
        m.put("iam/security-credentials/" + roleName, jsonCredentials );
        m.put("iam/security-credentials", roleName );
        m.put("iam/security-credentials/", roleName );
      }

    }
    return m;
  }

  private static Map<String, String> getPublicKeysMetadataMap( final VmInstance vm ) {
    final Map<String, String> m = Maps.newHashMap( );
    if ( vm.getBootRecord( ).getSshKeyPair() != null ) {
      m.put( "public-keys", "0=" + vm.getBootRecord( ).getSshKeyPair().getName( ) );
      m.put( "public-keys/", "0=" + vm.getBootRecord( ).getSshKeyPair().getName( ) );
      m.put( "public-keys/0/openssh-key", vm.getBootRecord( ).getSshKeyPair().getPublicKey( ) );
    }
    return m;
  }

  private static enum VolumeAttachmentComparator implements Comparator<VmVolumeAttachment> {
    INSTANCE;

    @Override
    public int compare(VmVolumeAttachment arg0, VmVolumeAttachment arg1) {
      return arg0.getDevice().compareToIgnoreCase(arg1.getDevice());
    }
  }

  private enum MetadataGroup implements Function<VmInstance,Map<String,String>> {
    Core {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( instance, getCoreMetadataMap( instance ), true );
      }

    },
    Network( "network" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( getNetworkMetadataMap( instance ) );
      }
    },
    BlockDeviceMapping( "block-device-mapping" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( getBlockDeviceMappingMetadataMap( instance ) );
      }
    },
    Iam( "iam" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        try {
          return metadataCache.get( new MetadataKey( instance.getInstanceUuid(), this ), new Callable<ImmutableMap<String,String>>() {
            @Override
            public ImmutableMap<String,String> call( ) throws Exception {
              return ImmutableMap.copyOf( addListingEntries( getIamMetadataMap( instance ) ) );
            }
          } );
        } catch ( ExecutionException e ) {
          throw Exceptions.toUndeclared( e ); // Cache load exception not expected
        }
      }

      @Override
      protected boolean isPresent( final VmInstance instance ) {
        return !Strings.isNullOrEmpty( instance.getIamInstanceProfileArn() );
      }
    },
    PublicKeys( "public-keys" ) {
      @Override
      public Map<String, String> apply( final VmInstance instance ) {
        return addListingEntries( getPublicKeysMetadataMap( instance ) );
      }

      @Override
      protected boolean isPresent( final VmInstance instance ) {
        return instance.getBootRecord( ).getSshKeyPair() != null;
      }
    };

    private final Optional<String> prefix;

    private MetadataGroup( ) {
      prefix = Optional.absent( );
    }

    private MetadataGroup( final String path ) {
      prefix = Optional.of( path );
    }

    public boolean providesPath( final String path ) {
      return
          prefix.transform( Functions.forPredicate( isPrefixOf( path ) ) )
              .or( Boolean.FALSE );
    }

    protected boolean isPresent(  final VmInstance instance   ) {
      return true;
    }

    private static Map<String,String> addListingEntries( final Map<String,String> metadataMap ) {
      return addListingEntries( null, metadataMap, false );
    }

    private static Map<String,String> addListingEntries( @Nullable final VmInstance instance,
                                                         final Map<String,String> metadataMap,
                                                         final boolean addRoots ) {
      final TreeMultimap<String,String> listingMap = TreeMultimap.create( );
      final Splitter pathSplitter = Splitter.on( '/' );
      final Joiner pathJoiner = Joiner.on( '/' );
      for ( final String path : metadataMap.keySet() ) {
        final List<String> pathSegments = Lists.newArrayList( pathSplitter.split( path ) );
        for ( int i=0; i<pathSegments.size(); i++ ) {
          listingMap.put(
              pathJoiner.join( pathSegments.subList( 0, i ) ),
              pathSegments.get( i ) + ( i < pathSegments.size() -1 ? "/" : "" ) );
        }
      }

      if ( addRoots && instance != null ) {
        for ( MetadataGroup group : MetadataGroup.values() ) {
          if ( group.isPresent( instance ) && group.prefix.isPresent(  ) ) {
            listingMap.put( "", group.prefix.get() + "/" );
          }
        }
      }

      final Joiner listingJoiner = Joiner.on( "\n" );
      for ( final String key : listingMap.keySet() ) {
        final Set<String> values = listingMap.get( key );
        final Iterator<String> valueIterator = values.iterator( );
        while ( valueIterator.hasNext( ) ) {
          final String value = valueIterator.next( );
          if ( values.contains( value+"/" ) ) valueIterator.remove( );
        }
        if ( !metadataMap.containsKey( key ) ) {
          metadataMap.put( key, listingJoiner.join( values ) );
        } else if ( !metadataMap.containsKey( key + "/" ) ) {
          metadataMap.put( key + "/", listingJoiner.join( values ) );
        }
      }

      return metadataMap;
    }
  }

  private static final class MetadataKey {
    private final String id; // internal id
    private final MetadataGroup metadataGroup;

    private MetadataKey( final String id, final MetadataGroup metadataGroup ) {
      this.id = id;
      this.metadataGroup = metadataGroup;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final MetadataKey that = (MetadataKey) o;

      if ( !id.equals( that.id ) ) return false;
      if ( metadataGroup != that.metadataGroup ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + metadataGroup.hashCode();
      return result;
    }
  }
}
