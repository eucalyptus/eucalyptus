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
 ************************************************************************/
package com.eucalyptus.auth.euare.identity.region;

import static com.eucalyptus.auth.euare.identity.region.RegionInfo.RegionService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

/**
 *
 */
public class RegionConfigurationManager {

  private final Supplier<Optional<RegionConfiguration>> regionConfigurationSupplier =
      Suppliers.memoizeWithExpiration( new Supplier<Optional<RegionConfiguration>>( ) {
        @Override
        public Optional<RegionConfiguration> get( ) {
          return RegionConfigurations.getRegionConfiguration( );
        }
      }, 1, TimeUnit.MINUTES );

  /**
   * Get information for a region based on an IAM identifier (AKI, etc)
   *
   * @param identifier The identifier to use
   * @return The region info optional
   */
  @Nonnull
  public Optional<RegionInfo> getRegionByIdentifier( @Nullable final String identifier ) {
    Optional<RegionInfo> regionInfoOptional = Optional.absent( );
    final Optional<RegionConfiguration> configurationOptional = regionConfigurationSupplier.get( );
    if ( configurationOptional.isPresent( ) && identifier != null && identifier.length( ) > 5 ) {
      final RegionConfiguration configuration = configurationOptional.get( );
      final String regionIdPartition = identifier.substring( 3, 5 );
      for ( final Region region : configuration ) {
        if ( Iterables.contains(
            Iterables.transform( region.getIdentifierPartitions( ), PartitionFunctions.IDENTIFIER ),
            regionIdPartition ) ) {
          regionInfoOptional = Optional.of( TypeMappers.transform( region, RegionInfo.class ) );
        }
      }
    }
    return regionInfoOptional;
  }

  private enum PartitionFunctions implements Function<Integer,String> {
    IDENTIFIER {
      private final char[] characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray( );

      @Nullable
      @Override
      public String apply( final Integer integer ) {
        return new String( new char[ ]{ characters[ integer / 32 ], characters[ integer % 32 ] } );
      }
    }
  }

  @TypeMapper
  private enum RegionToRegionInfoTransform implements Function<Region,RegionInfo> {
    INSTANCE;

    @Nullable
    @Override
    public RegionInfo apply( @Nullable final Region region ) {
      return region == null ?
          null :
          new RegionInfo(
              region.getName( ),
              Collections2.transform( region.getServices( ), TypeMappers.lookup( Service.class, RegionService.class ) ) );
    }
  }

  @TypeMapper
  private enum ServiceToRegionServiceTransform implements Function<Service,RegionService> {
    INSTANCE;

    @Nullable
    @Override
    public RegionService apply( @Nullable final Service service ) {
      return service == null ?
          null :
          new RegionService( service.getType( ), service.getEndpoints( ) );
    }
  }
}
