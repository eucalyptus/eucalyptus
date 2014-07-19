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

import static com.eucalyptus.compute.common.CloudMetadata.DhcpOptionSetMetadata;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.DhcpConfigurationItemType;
import edu.ucsb.eucalyptus.msgs.DhcpOptionsType;

/**
 *
 */
public interface DhcpOptionSets extends Lister<DhcpOptionSet> {

  String DHCP_OPTION_DOMAIN_NAME_SERVERS = "domain-name-servers";
  String DHCP_OPTION_DOMAIN_NAME = "domain-name";
  String DHCP_OPTION_NTP_SERVERS = "ntp-servers";
  String DHCP_OPTION_NETBIOS_NAME_SERVERS = "netbios-name-servers";
  String DHCP_OPTION_NETBIOS_NODE_TYPE = "netbios-node-type";

  Set<String> DHCP_OPTIONS = ImmutableSortedSet.of(
      DHCP_OPTION_DOMAIN_NAME_SERVERS,
      DHCP_OPTION_DOMAIN_NAME,
      DHCP_OPTION_NTP_SERVERS,
      DHCP_OPTION_NETBIOS_NAME_SERVERS,
      DHCP_OPTION_NETBIOS_NODE_TYPE
  );

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
