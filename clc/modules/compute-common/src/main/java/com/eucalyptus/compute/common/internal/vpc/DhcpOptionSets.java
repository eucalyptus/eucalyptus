/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static com.eucalyptus.compute.common.CloudMetadata.DhcpOptionSetMetadata;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.DhcpConfigurationItemType;
import com.eucalyptus.compute.common.DhcpOptionsType;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public interface DhcpOptionSets extends Lister<DhcpOptionSet> {

  String DHCP_OPTION_DOMAIN_NAME_SERVERS = "domain-name-servers";
  String DHCP_OPTION_DOMAIN_NAME = "domain-name";
  String DHCP_OPTION_NTP_SERVERS = "ntp-servers";
  String DHCP_OPTION_NETBIOS_NAME_SERVERS = "netbios-name-servers";
  String DHCP_OPTION_NETBIOS_NODE_TYPE = "netbios-node-type";

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super DhcpOptionSet> filter,
                    Function<? super DhcpOptionSet,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super DhcpOptionSet,T> transform ) throws VpcMetadataException;

  <T> T lookupByExample( final DhcpOptionSet example,
                         @Nullable final OwnerFullName ownerFullName,
                         final String key,
                         final Predicate<? super DhcpOptionSet> filter,
                         final Function<? super DhcpOptionSet,T> transform ) throws VpcMetadataException;

  boolean delete( final DhcpOptionSetMetadata metadata ) throws VpcMetadataException;

  DhcpOptionSet save( DhcpOptionSet subnet ) throws VpcMetadataException;


  @TypeMapper
  public enum DhcpOptionSetToDhcpOptionsTypeTransform implements Function<DhcpOptionSet,DhcpOptionsType> {
    INSTANCE;

    @Nullable
    @Override
    public DhcpOptionsType apply( @Nullable final DhcpOptionSet dhcpOptionSet ) {
      return dhcpOptionSet == null ?
          null :
          new DhcpOptionsType(
              dhcpOptionSet.getDisplayName( ),
              Collections2.transform(
                  dhcpOptionSet.getDhcpOptions( ),
                  DhcpOptionToDhcpConfigurationItemTypeTransform.INSTANCE )
          );
    }
  }

  @TypeMapper
  public enum DhcpOptionToDhcpConfigurationItemTypeTransform implements Function<DhcpOption,DhcpConfigurationItemType> {
    INSTANCE;

    @Nullable
    @Override
    public DhcpConfigurationItemType apply( @Nullable final DhcpOption dhcpOption ) {
      return dhcpOption == null ?
          null :
          new DhcpConfigurationItemType(
              dhcpOption.getKey(),
              dhcpOption.getValues( )
      );
    }
  }

  public static class DhcpOptionSetFilterSupport extends FilterSupport<DhcpOptionSet> {
    public DhcpOptionSetFilterSupport( ) {
      super( builderFor( DhcpOptionSet.class )
          .withTagFiltering( DhcpOptionSetTag.class, "dhcpOptionSet" )
          .withStringProperty( "dhcp-options-id", CloudMetadatas.toDisplayName() )
          .withStringSetProperty( "key", FilterStringSetFunctions.KEY )
          .withStringSetProperty( "value", FilterStringSetFunctions.VALUE )
          .withPersistenceAlias( "dhcpOptions", "dhcpOptions" )
          .withPersistenceFilter( "dhcp-options-id", "displayName")
          .withPersistenceFilter( "key", "dhcpOptions.key" )
      );
    }
  }

  public enum FilterStringSetFunctions implements Function<DhcpOptionSet,Set<String>> {
    KEY {
      @Override
      public Set<String> apply( final DhcpOptionSet dhcpOptionSet ){
        return Sets.newHashSet( Iterables.transform( dhcpOptionSet.getDhcpOptions( ), DhcpOptionStringFunctions.KEY ) );
      }
    },
    VALUE {
      @Override
      public Set<String> apply( final DhcpOptionSet dhcpOptionSet ){
        return Sets.newHashSet( Iterables.concat(
            Iterables.transform( dhcpOptionSet.getDhcpOptions(), DhcpOptionStringSetFunctions.VALUE ) ) );
      }
    },
  }

  public enum DhcpOptionStringFunctions implements Function<DhcpOption,String> {
    KEY {
      @Override
      public String apply( final DhcpOption dhcpOption ){
        return dhcpOption.getKey();
      }
    },
  }

  public enum DhcpOptionStringSetFunctions implements Function<DhcpOption,Set<String>> {
    VALUE {
      @Override
      public Set<String> apply( final DhcpOption dhcpOption ){
        return Sets.newHashSet( dhcpOption.getValues( ) );
      }
    },
  }
}
