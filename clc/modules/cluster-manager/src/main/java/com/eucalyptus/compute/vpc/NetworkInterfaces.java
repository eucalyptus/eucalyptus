/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.compute.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.NetworkInterfaceMetadata;
import static com.eucalyptus.tags.FilterSupport.PersistenceFilter.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.NetworkInterfaceAttachmentType;
import edu.ucsb.eucalyptus.msgs.NetworkInterfaceType;

/**
 *
 */
public interface NetworkInterfaces extends Lister<NetworkInterface> {

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super NetworkInterface> filter,
                    Function<? super NetworkInterface,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super NetworkInterface,T> transform ) throws VpcMetadataException;

  boolean delete( final NetworkInterfaceMetadata metadata ) throws VpcMetadataException;

  NetworkInterface save( NetworkInterface networkInterface ) throws VpcMetadataException;

  NetworkInterface updateByExample( NetworkInterface example,
                                    OwnerFullName ownerFullName,
                                    String key,
                                    Callback<NetworkInterface> updateCallback ) throws VpcMetadataException;

  @TypeMapper
  public enum NetworkInterfaceToNetworkInterfaceTypeTransform implements Function<NetworkInterface,NetworkInterfaceType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkInterfaceType apply( @Nullable final NetworkInterface networkInterface ) {
      //TODO:STEVE: security groups, attachments, associations and private ips
      return networkInterface == null ?
          null :
          new NetworkInterfaceType(
              networkInterface.getDisplayName( ),
              CloudMetadatas.toDisplayName( ).apply( networkInterface.getSubnet( ) ),
              CloudMetadatas.toDisplayName( ).apply( networkInterface.getVpc( ) ),
              networkInterface.getAvailabilityZone( ),
              networkInterface.getDescription( ),
              networkInterface.getOwnerAccountNumber( ),
              networkInterface.getRequesterId( ),
              networkInterface.getRequesterManaged( ),
              Objects.toString( networkInterface.getState( ), null ),
              networkInterface.getMacAddress( ),
              networkInterface.getPrivateIpAddress( ),
              networkInterface.getPrivateDnsName(),
              networkInterface.getSourceDestCheck( ),
              networkInterface.getAttachment( ) == null ?
                  null :
                  TypeMappers.transform( networkInterface.getAttachment( ), NetworkInterfaceAttachmentType.class )
          );
    }
  }

  @TypeMapper
  public enum NetworkInterfaceAttachmentToNetworkInterfaceAttachmentTypeTransform implements Function<NetworkInterfaceAttachment,NetworkInterfaceAttachmentType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkInterfaceAttachmentType apply( @Nullable final NetworkInterfaceAttachment networkInterfaceAttachment ) {
      return networkInterfaceAttachment == null ?
          null :
          new NetworkInterfaceAttachmentType(
              networkInterfaceAttachment.getAttachmentId( ),
              networkInterfaceAttachment.getInstanceId( ),
              networkInterfaceAttachment.getInstanceOwnerId( ),
              networkInterfaceAttachment.getDeviceIndex( ),
              networkInterfaceAttachment.getAttachTime( ),
              networkInterfaceAttachment.getDeleteOnTerminate( )
          );
    }
  }

  public static class NetworkInterfaceFilterSupport extends FilterSupport<NetworkInterface> {
    public NetworkInterfaceFilterSupport( ) {
      super( builderFor( NetworkInterface.class )
              .withTagFiltering( NetworkInterfaceTag.class, "networkInterface" )
              .withUnsupportedProperty( "addresses.private-ip-address" )
              .withUnsupportedProperty( "addresses.primary" )
              .withUnsupportedProperty( "addresses.association.public-ip" )
              .withUnsupportedProperty( "addresses.association.owner-id" )
              .withUnsupportedProperty( "association.association-id" ) //TODO:STEVE: filters for network interface EIP associations
              .withUnsupportedProperty( "association.allocation-id" )
              .withUnsupportedProperty( "association.ip-owner-id" )
              .withUnsupportedProperty( "association.public-ip" )
              .withUnsupportedProperty( "association.public-dns-name" )
              .withStringProperty( "attachment.attachment-id", FilterStringFunctions.ATTACHMENT_ATTACHMENT_ID )
              .withStringProperty( "attachment.instance-id", FilterStringFunctions.ATTACHMENT_INSTANCE_ID )
              .withStringProperty( "attachment.instance-owner-id", FilterStringFunctions.ATTACHMENT_INSTANCE_OWNER_ID )
              .withIntegerProperty( "attachment.device-index", FilterIntegerFunctions.ATTACHMENT_DEVICE_INDEX )
              .withStringProperty( "attachment.status", FilterStringFunctions.ATTACHMENT_STATUS )
              .withDateProperty( "attachment.attach.time", FilterDateFunctions.ATTACHMENT_ATTACH_TIME )
              .withBooleanProperty( "attachment.delete-on-termination", FilterBooleanFunctions.ATTACHMENT_DELETE_ON_TERMINATION )
              .withStringProperty( "availability-zone", FilterStringFunctions.AVAILABILITY_ZONE )
              .withStringProperty( "description", FilterStringFunctions.DESCRIPTION )
              .withUnsupportedProperty( "group-id" ) //TODO:STEVE: filters for network interface security groups
              .withUnsupportedProperty( "group-name" )
              .withStringProperty( "mac-address", FilterStringFunctions.MAC_ADDRESS )
              .withStringProperty( "network-interface-id", CloudMetadatas.toDisplayName() )
              .withStringProperty( "owner-id", FilterStringFunctions.OWNER_ID )
              .withStringProperty( "private-ip-address", FilterStringFunctions.PRIVATE_IP )
              .withStringProperty( "private-dns-name", FilterStringFunctions.PRIVATE_DNS_NAME )
              .withStringProperty( "requester-id", FilterStringFunctions.REQUESTER_ID )
              .withBooleanProperty( "requester-managed", FilterBooleanFunctions.REQUESTER_MANAGED )
              .withBooleanProperty( "source-dest-check", FilterBooleanFunctions.SOURCE_DEST_CHECK )
              .withStringProperty( "status", FilterStringFunctions.STATE )
              .withStringProperty( "subnet-id", FilterStringFunctions.SUBNET_ID )
              .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
              .withPersistenceAlias( "vpc", "vpc" )
              .withPersistenceAlias( "subnet", "subnet" )
              .withPersistenceFilter( "attachment.attachment-id", "attachment.attachmentId", Collections.<String>emptySet() )
              .withPersistenceFilter( "attachment.instance-id", "attachment.instanceId", Collections.<String>emptySet() )
              .withPersistenceFilter( "attachment.instance-owner-id", "attachment.instanceOwnerId", Collections.<String>emptySet() )
              .withPersistenceFilter( "attachment.device-index", "attachment.deviceIndex", Collections.<String>emptySet(), Type.Integer )
              .withPersistenceFilter( "attachment.status", "attachment.status", Collections.<String>emptySet(), Enums.valueOfFunction( NetworkInterfaceAttachment.Status.class ) )
              .withPersistenceFilter( "attachment.attach.time", "attachment.attachTime", Collections.<String>emptySet(), Type.Date )
              .withPersistenceFilter( "attachment.delete-on-termination", "attachment.deleteOnTerminate", Collections.<String>emptySet(), Type.Boolean )
              .withPersistenceFilter( "availability-zone", "availabilityZone" )
              .withPersistenceFilter( "description" )
              .withPersistenceFilter( "mac-address", "macAddress" )
              .withPersistenceFilter( "network-interface-id", "displayName" )
              .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
              .withPersistenceFilter( "private-ip-address", "privateIpAddress" )
              .withPersistenceFilter( "private-dns-name", "privateDnsName" )
              .withPersistenceFilter( "requester-id", "requesterId" )
              .withPersistenceFilter( "requester-managed", "requesterManaged", Collections.<String>emptySet(), Type.Boolean )
              .withPersistenceFilter( "source-dest-check", "sourceDestCheck", Collections.<String>emptySet(), Type.Boolean )
              .withPersistenceFilter( "status", "state", Enums.valueOfFunction( NetworkInterface.State.class ) )
              .withPersistenceFilter( "subnet-id", "subnet.displayName" )
              .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<NetworkInterface,String> {
    ATTACHMENT_ATTACHMENT_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getAttachment( ) == null ? null : networkInterface.getAttachment( ).getAttachmentId( );
      }
    },
    ATTACHMENT_INSTANCE_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getAttachment( ) == null ? null : networkInterface.getAttachment( ).getInstanceId( );
      }
    },
    ATTACHMENT_INSTANCE_OWNER_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getAttachment( ) == null ? null : networkInterface.getAttachment( ).getInstanceOwnerId( );
      }
    },
    ATTACHMENT_STATUS {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getAttachment( ) == null ?
            null :
            Objects.toString( networkInterface.getAttachment( ).getStatus( ), null );
      }
    },
    AVAILABILITY_ZONE {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getAvailabilityZone( );
      }
    },
    DESCRIPTION {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getDescription( );
      }
    },
    MAC_ADDRESS {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getMacAddress( );
      }
    },
    OWNER_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getOwnerAccountNumber( );
      }
    },
    PRIVATE_DNS_NAME {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getPrivateDnsName( );
      }
    },
    PRIVATE_IP {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getPrivateIpAddress( );
      }
    },
    REQUESTER_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return networkInterface.getRequesterId( );
      }
    },
    STATE {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return Objects.toString( networkInterface.getState( ), null );
      }
    },
    SUBNET_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return CloudMetadatas.toDisplayName().apply( networkInterface.getSubnet() );
      }
    },
    VPC_ID {
      @Override
      public String apply( final NetworkInterface networkInterface ){
        return CloudMetadatas.toDisplayName().apply( networkInterface.getVpc( ) );
      }
    },
  }

  public enum FilterBooleanFunctions implements Function<NetworkInterface,Boolean> {
    ATTACHMENT_DELETE_ON_TERMINATION {
      @Override
      public Boolean apply( final NetworkInterface networkInterface ) {
        return networkInterface.getAttachment() == null ? null : networkInterface.getAttachment( ).getDeleteOnTerminate( );
      }
    },
    REQUESTER_MANAGED {
      @Override
      public Boolean apply( final NetworkInterface networkInterface ) {
        return networkInterface.getRequesterManaged( );
      }
    },
    SOURCE_DEST_CHECK {
      @Override
      public Boolean apply( final NetworkInterface networkInterface ) {
        return networkInterface.getSourceDestCheck( );
      }
    },
  }

  public enum FilterIntegerFunctions implements Function<NetworkInterface,Integer> {
    ATTACHMENT_DEVICE_INDEX {
      @Override
      public Integer apply( final NetworkInterface networkInterface ) {
        return networkInterface.getAttachment() == null ? null : networkInterface.getAttachment( ).getDeviceIndex();
      }
    },
  }

  public enum FilterDateFunctions implements Function<NetworkInterface,Date> {
    ATTACHMENT_ATTACH_TIME {
      @Override
      public Date apply( final NetworkInterface networkInterface ) {
        return networkInterface.getAttachment() == null ? null : networkInterface.getAttachment( ).getAttachTime();
      }
    },
  }
}


