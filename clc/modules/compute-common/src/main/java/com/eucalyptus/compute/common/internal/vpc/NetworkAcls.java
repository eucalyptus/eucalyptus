/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.compute.common.internal.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.NetworkAclMetadata;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.NetworkAclAssociationType;
import com.eucalyptus.compute.common.NetworkAclEntryType;
import com.eucalyptus.compute.common.NetworkAclType;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 *
 */
public interface NetworkAcls extends Lister<NetworkAcl> {

  static Ordering<NetworkAclEntry> ENTRY_ORDERING =
      Ordering.natural().reverse().onResultOf( NetworkAcls.NetworkAclEntryFilterBooleanFunctions.EGRESS ).compound(
          Ordering.natural().onResultOf( NetworkAcls.NetworkAclEntryFilterIntegerFunctions.RULE_NUMBER ) );

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super NetworkAcl> filter,
                    Function<? super NetworkAcl,T> transform ) throws VpcMetadataException;

  long countByExample( NetworkAcl example,
                       Criterion criterion,
                       Map<String,String> aliases ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super NetworkAcl,T> transform ) throws VpcMetadataException;

  <T> T lookupDefault( String vpcId,
                       Function<? super NetworkAcl,T> transform ) throws VpcMetadataException;

  boolean delete( final NetworkAclMetadata metadata ) throws VpcMetadataException;

  NetworkAcl save( NetworkAcl networkAcl ) throws VpcMetadataException;

  NetworkAcl updateByExample( NetworkAcl example,
                              OwnerFullName ownerFullName,
                              String key,
                              Callback<NetworkAcl> updateCallback ) throws VpcMetadataException;

  AbstractPersistentSupport<NetworkAclMetadata,NetworkAcl,VpcMetadataException> withRetries( );

  @TypeMapper
  public enum NetworkAclToNetworkAclTypeTransform implements Function<NetworkAcl,NetworkAclType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkAclType apply( @Nullable final NetworkAcl networkAcl ) {
      return networkAcl == null ?
          null :
          new NetworkAclType(
              networkAcl.getDisplayName( ),
              networkAcl.getVpc().getDisplayName( ),
              networkAcl.getDefaultForVpc( ),
              Collections2.transform( ENTRY_ORDERING.sortedCopy( networkAcl.getEntries( ) ), NetworkAclEntryToNetworkAclEntryType.INSTANCE ),
              Collections2.transform( networkAcl.getSubnets(), SubnetToNetworkAclAssociationType.INSTANCE )
          );
    }
  }

  @TypeMapper
  public enum NetworkAclEntryToNetworkAclEntryType implements Function<NetworkAclEntry,NetworkAclEntryType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkAclEntryType apply( @Nullable final NetworkAclEntry networkAclEntry ) {
      return networkAclEntry == null ?
          null :
          new NetworkAclEntryType(
              networkAclEntry.getRuleNumber( ),
              Integer.toString( networkAclEntry.getProtocol( ) ),
              networkAclEntry.getRuleAction( ).toString( ),
              networkAclEntry.getEgress( ),
              networkAclEntry.getCidr(),
              networkAclEntry.getIcmpCode(),
              networkAclEntry.getIcmpType(),
              networkAclEntry.getPortRangeFrom(),
              networkAclEntry.getPortRangeTo()
          );
    }
  }

  @TypeMapper
  public enum SubnetToNetworkAclAssociationType implements Function<Subnet,NetworkAclAssociationType> {
    INSTANCE;

    @Nullable
    @Override
    public NetworkAclAssociationType apply( @Nullable final Subnet subnet ) {
      return subnet == null ?
          null :
          new NetworkAclAssociationType(
            subnet.getNetworkAclAssociationId(),
            subnet.getNetworkAcl( ).getDisplayName(),
            subnet.getDisplayName()
          );
    }
  }

  public static class NetworkAclFilterSupport extends FilterSupport<NetworkAcl> {
    public NetworkAclFilterSupport( ) {
      super( builderFor( NetworkAcl.class )
              .withTagFiltering( NetworkAclTag.class, "networkAcl" )
              .withStringSetProperty( "association.association-id", FilterStringSetFunctions.ASSOCIATION_ID )
              .withStringProperty( "association.network-acl-id", FilterStringFunctions.ASSOCIATION_NETWORK_ACL_ID )
              .withStringSetProperty( "association.subnet-id", FilterStringSetFunctions.ASSOCIATION_SUBNET_ID )
              .withBooleanProperty( "default", FilterBooleanFunctions.DEFAULT_FOR_VPC )
              .withStringSetProperty( "entry.cidr", FilterStringSetFunctions.ENTRY_CIDR )
              .withBooleanSetProperty( "entry.egress", FilterBooleanSetFunctions.ENTRY_EGRESS )
              .withIntegerSetProperty( "entry.icmp.code", FilterIntegerSetFunctions.ICMP_CODE )
              .withIntegerSetProperty( "entry.icmp.type", FilterIntegerSetFunctions.ICMP_TYPE )
              .withUnsupportedProperty( "entry.ipv6-cidr" )
              .withIntegerSetProperty( "entry.port-range.from", FilterIntegerSetFunctions.PORT_FROM )
              .withIntegerSetProperty( "entry.port-range.to", FilterIntegerSetFunctions.PORT_TO )
              .withIntegerSetProperty( "entry.protocol", FilterIntegerSetFunctions.PROTOCOL, ProtocolValueFunction.INSTANCE )
              .withStringSetProperty( "entry.rule-action", FilterStringSetFunctions.ENTRY_RULE_ACTION )
              .withIntegerSetProperty( "entry.rule-number", FilterIntegerSetFunctions.RULE_NUMBER )
              .withStringProperty( "network-acl-id", CloudMetadatas.toDisplayName() )
              .withStringProperty( "owner-id", FilterStringFunctions.ACCOUNT_ID )
              .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
              .withPersistenceAlias( "subnets", "subnets" )
              .withPersistenceAlias( "entries", "entries" )
              .withPersistenceAlias( "vpc", "vpc" )
              .withPersistenceFilter( "association.association-id", "subnets.networkAclAssociationId" )
              .withPersistenceFilter( "association.subnet-id", "subnets.displayName" )
              .withPersistenceFilter( "default", "defaultForVpc", PersistenceFilter.Type.Boolean )
              .withPersistenceFilter( "entry.cidr", "entries.cidr" )
              .withPersistenceFilter( "entry.egress", "entries.egress", PersistenceFilter.Type.Boolean )
              .withPersistenceFilter( "entry.icmp.code", "entries.icmpCode", PersistenceFilter.Type.Integer )
              .withPersistenceFilter( "entry.icmp.type", "entries.icmpType", PersistenceFilter.Type.Integer )
              .withPersistenceFilter( "entry.port-range.from", "entries.portRangeFrom", PersistenceFilter.Type.Integer )
              .withPersistenceFilter( "entry.port-range.to", "entries.portRangeTo", PersistenceFilter.Type.Integer )
              .withPersistenceFilter( "entry.protocol", "entries.protocol", ProtocolValueFunction.INSTANCE )
              .withPersistenceFilter( "entry.rule-action", "entries.ruleAction", FUtils.valueOfFunction( NetworkAclEntry.RuleAction.class ) )
              .withPersistenceFilter( "entry.rule-number", "entries.ruleNumber", PersistenceFilter.Type.Integer )
              .withPersistenceFilter( "network-acl-id", "displayName" )
              .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
              .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }

    private enum ProtocolValueFunction implements Function<String,Integer> {
      INSTANCE {
        @Nullable
        @Override
        public Integer apply( final String value ) {
          switch ( value.toLowerCase( ) ) {
            case "tcp":
              return 6;
            case "udp":
              return 17;
            case "icmp":
              return 1;
            default:
              return Ints.tryParse( value );
          }
        }
      }
    }
  }

  public enum FilterStringFunctions implements Function<NetworkAcl,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final NetworkAcl networkAcl ){
        return networkAcl.getOwnerAccountNumber();
      }
    },
    ASSOCIATION_NETWORK_ACL_ID {
      @Override
      public String apply( final NetworkAcl networkAcl ){
        return networkAcl.getSubnets().isEmpty() ? null : networkAcl.getDisplayName( );
      }
    },
    VPC_ID {
      @Override
      public String apply( final NetworkAcl networkAcl ){
        return networkAcl.getVpc( ).getDisplayName();
      }
    },
  }

  public enum FilterBooleanFunctions implements Function<NetworkAcl,Boolean> {
    DEFAULT_FOR_VPC {
      @Override
      public Boolean apply( final NetworkAcl networkAcl ){
        return networkAcl.getDefaultForVpc();
      }
    },
  }

  public enum NetworkAclEntryFilterStringFunctions implements Function<NetworkAclEntry, String> {
    CIDR {
      @Override
      public String apply( final NetworkAclEntry entry ) {
        return entry.getCidr( );
      }
    },
    RULE_ACTION {
      @Override
      public String apply( final NetworkAclEntry entry ) {
        return Objects.toString( entry.getRuleAction(), null );
      }
    },
  }

  public enum NetworkAclEntryFilterBooleanFunctions implements Function<NetworkAclEntry, Boolean> {
    EGRESS {
      @Override
      public Boolean apply( final NetworkAclEntry entry ) {
        return entry.getEgress();
      }
    },
  }

  public enum NetworkAclEntryFilterIntegerFunctions implements Function<NetworkAclEntry, Integer> {
    ICMP_CODE {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getIcmpCode( );
      }
    },
    ICMP_TYPE {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getIcmpType();
      }
    },
    PORT_FROM {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getPortRangeFrom();
      }
    },
    PORT_TO {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getPortRangeTo();
      }
    },
    PROTOCOL {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getProtocol();
      }
    },
    RULE_NUMBER {
      @Override
      public Integer apply( final NetworkAclEntry entry ) {
        return entry.getRuleNumber();
      }
    },
  }

  public enum FilterStringSetFunctions implements Function<NetworkAcl, Set<String>> {
    ASSOCIATION_ID {
      @Override
      public Set<String> apply( final NetworkAcl networkAcl ) {
        return subnetPropertySet( networkAcl, Subnets.FilterStringFunctions.NETWORK_ACL_ASSOCIATION_ID );
      }
    },
    ASSOCIATION_SUBNET_ID {
      @Override
      public Set<String> apply( final NetworkAcl networkAcl ) {
        return subnetPropertySet( networkAcl, CloudMetadatas.toDisplayName() );
      }
    },
    ENTRY_CIDR {
      @Override
      public Set<String> apply( final NetworkAcl networkAcl ) {
        return entryPropertySet( networkAcl, NetworkAclEntryFilterStringFunctions.CIDR );
      }
    },
    ENTRY_RULE_ACTION {
      @Override
      public Set<String> apply( final NetworkAcl networkAcl ) {
        return entryPropertySet( networkAcl, NetworkAclEntryFilterStringFunctions.RULE_ACTION );
      }
    },
    ;

    static <T> Set<T> entryPropertySet( final NetworkAcl networkAcl,
                                                 final Function<? super NetworkAclEntry, T> propertyGetter ) {
      return Sets.newHashSet( Iterables.transform( networkAcl.getEntries( ), propertyGetter ) );
    }

    static <T> Set<T> subnetPropertySet( final NetworkAcl networkAcl,
                                                 final Function<? super Subnet, T> propertyGetter ) {
      return Sets.newHashSet( Iterables.transform( networkAcl.getSubnets(), propertyGetter ) );
    }
  }

  public enum FilterBooleanSetFunctions implements Function<NetworkAcl, Set<Boolean>> {
    ENTRY_EGRESS {
      @Override
      public Set<Boolean> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterBooleanFunctions.EGRESS );
      }
    },
  }

  public enum FilterIntegerSetFunctions implements Function<NetworkAcl, Set<Integer>> {
    ICMP_CODE {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.ICMP_CODE );
      }
    },
    ICMP_TYPE {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.ICMP_TYPE );
      }
    },
    PORT_FROM {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.PORT_FROM );
      }
    },
    PORT_TO {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.PORT_TO );
      }
    },
    PROTOCOL {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.PROTOCOL );
      }
    },
    RULE_NUMBER {
      @Override
      public Set<Integer> apply( final NetworkAcl networkAcl ) {
        return FilterStringSetFunctions.entryPropertySet( networkAcl, NetworkAclEntryFilterIntegerFunctions.RULE_NUMBER );
      }
    },
  }
}
