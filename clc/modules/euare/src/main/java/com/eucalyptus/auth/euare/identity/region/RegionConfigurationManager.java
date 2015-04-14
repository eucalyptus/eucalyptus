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
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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

  private final Cache<String,X509Certificate> certificateCache = CacheBuilder
      .<String,X509Certificate>newBuilder( )
      .expireAfterWrite( 1, TimeUnit.HOURS )
      .build( );

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

  /**
   * Get information for a region based on an account identifier
   *
   * @param accountNumber The account number to use
   * @return The region info optional
   */
  @Nonnull
  public Optional<RegionInfo> getRegionByAccountNumber( @Nullable final String accountNumber ) {
    Optional<RegionInfo> regionInfoOptional = Optional.absent( );
    final Optional<RegionConfiguration> configurationOptional = regionConfigurationSupplier.get( );
    if ( configurationOptional.isPresent( ) && accountNumber != null && accountNumber.length( ) == 12 ) {
      final RegionConfiguration configuration = configurationOptional.get( );
      final String regionIdPartition = accountNumber.substring( 0, 3 );
      for ( final Region region : configuration ) {
        if ( Iterables.contains(
            Iterables.transform( region.getIdentifierPartitions( ), PartitionFunctions.ACCOUNT_NUMBER ),
            regionIdPartition ) ) {
          regionInfoOptional = Optional.of( TypeMappers.transform( region, RegionInfo.class ) );
        }
      }
    }
    return regionInfoOptional;
  }

  /**
   * Get the region information for the local region (if any)
   *
   * @return The optional region information
   */
  public Optional<RegionInfo> getRegionInfo( ) {
    return Iterables.tryFind(
          Iterables.concat( regionConfigurationSupplier.get( ).asSet() ),
          propertyPredicate( RegionConfigurations.getRegionName( ).asSet(), RegionNameTransform.INSTANCE )
      ).transform( RegionToRegionInfoTransform.INSTANCE );
  }

  /**
   * Get all region information (if any)
   *
   * @return The region information
   */
  public Iterable<RegionInfo> getRegionInfos( ) {
    return Iterables.transform(
        Iterables.concat( regionConfigurationSupplier.get( ).asSet( ) ),
        RegionToRegionInfoTransform.INSTANCE );
  }

  public boolean isRegionCertificate( final X509Certificate certificate ) {
    boolean found = false;
    final Optional<RegionConfiguration> configurationOptional = regionConfigurationSupplier.get( );
    if ( configurationOptional.isPresent( ) ) {
      final RegionConfiguration configuration = configurationOptional.get( );
      for ( final Region region : configuration ) {
        try {
          final X509Certificate regionCertificate = certificateCache.get(
              region.getCertificate( ),
              new Callable<X509Certificate>( ) {
                @Override
                public X509Certificate call() throws Exception {
                  return PEMFiles.getCert( region.getCertificate().getBytes( StandardCharsets.UTF_8 ) );
                }
              } );
          found = regionCertificate.equals( certificate );
          if ( found ) break;
        } catch ( ExecutionException e ) {
          // skip the certificate
        }
      }
    }
    return found;
  }

  private enum PartitionFunctions implements NonNullFunction<Integer,String> {
    ACCOUNT_NUMBER {
      @Nonnull
      @Override
      public String apply( final Integer integer ) {
        return Strings.padStart( String.valueOf( integer ), 3, '0' );
      }
    },
    IDENTIFIER {
      private final char[] characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray( );

      @Nonnull
      @Override
      public String apply( final Integer integer ) {
        return new String( new char[ ]{ characters[ integer / 32 ], characters[ integer % 32 ] } );
      }
    },
  }

  private enum RegionNameTransform implements NonNullFunction<Region,String> {
    INSTANCE;

    @Nonnull
    @Override
    public String apply( final Region region ) {
      return region.getName( );
    }
  }

  private enum RegionInfoPartitionsTransform implements NonNullFunction<RegionInfo,Set<Integer>> {
    INSTANCE;

    @Nonnull
    @Override
    public Set<Integer> apply(final RegionInfo region ) {
      return region.getPartitions( );
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
              region.getIdentifierPartitions( ),
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

  public static class ConfiguredIdentifierPartitionSupplier implements Identifiers.IdentifierPartitionSupplier {
    private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

    @Override
    public Iterable<String> getIdentifierPartitions( ) {
      return Iterables.transform(
          Iterables.concat(
              regionConfigurationManager.getRegionInfo( ).transform( RegionInfoPartitionsTransform.INSTANCE ).asSet( ) ),
          PartitionFunctions.IDENTIFIER );
    }

    @Override
    public Iterable<String> getAccountNumberPartitions( ) {
      return Iterables.transform(
          Iterables.concat(
              regionConfigurationManager.getRegionInfo( ).transform( RegionInfoPartitionsTransform.INSTANCE ).asSet( ) ),
          PartitionFunctions.ACCOUNT_NUMBER );
    }
  }
}
