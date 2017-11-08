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
package com.eucalyptus.auth.euare.identity.region;

import static com.eucalyptus.auth.RegionService.regionName;
import static com.eucalyptus.auth.RegionService.serviceType;
import static com.eucalyptus.auth.euare.identity.region.RegionInfo.RegionService;
import static com.eucalyptus.util.CollectionUtils.propertyContainsPredicate;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.net.InetAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.*;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.groups.ApiEndpointServicesGroup;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.common.net.InetAddresses;

/**
 *
 */
public class RegionConfigurationManager {

  private static final Supplier<Optional<RegionConfiguration>> regionConfigurationSupplier =
      Suppliers.memoizeWithExpiration( new Supplier<Optional<RegionConfiguration>>( ) {
        @Override
        public Optional<RegionConfiguration> get( ) {
          return RegionConfigurations.getRegionConfiguration( );
        }
      }, 1, TimeUnit.MINUTES );

  private static final Cache<String,byte[]> certificateDigestCache = CacheBuilder
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
          regionInfoOptional = Optional.of( regionToRegionInfoTransform( configurationOptional ).apply( region ) );
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
          regionInfoOptional = Optional.of( regionToRegionInfoTransform( configurationOptional ).apply( region ) );
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
    final Optional<RegionConfiguration> regionConfigurationOptional = regionConfigurationSupplier.get();
    return Iterables.tryFind(
          Iterables.concat( regionConfigurationOptional.asSet() ),
          propertyPredicate( RegionConfigurations.getRegionName( ).asSet(), RegionNameTransform.INSTANCE )
      ).transform( regionToRegionInfoTransform( regionConfigurationOptional ) );
  }

  /**
   * Get the region information for the local region (if any)
   *
   * @return The optional region information
   */
  public Optional<RegionInfo> getRegionInfoByHost( final String host ) {
    final Optional<RegionConfiguration> regionConfigurationOptional = regionConfigurationSupplier.get( );
    return Iterables.tryFind(
        Iterables.concat( regionConfigurationOptional.asSet() ),
        propertyContainsPredicate( host, RegionServiceHostTransform.INSTANCE )
    ).transform( regionToRegionInfoTransform( regionConfigurationOptional ) );
  }

  /**
   * Get all region information (if any)
   *
   * @return The region information
   */
  public Iterable<RegionInfo> getRegionInfos( ) {
    final Optional<RegionConfiguration> regionConfigurationOptional = regionConfigurationSupplier.get( );
    return Iterables.transform(
        Iterables.concat( regionConfigurationOptional.asSet( ) ),
        regionToRegionInfoTransform( regionConfigurationOptional ) );
  }

  public boolean isRegionSSLCertificate( final String host, final X509Certificate certificate ) {
    boolean valid = false;
    final Optional<RegionInfo> hostRegion = getRegionInfoByHost( host );
    if ( hostRegion.isPresent( ) ) {
      if ( hostRegion.get( ).getSslCertificateFingerprint( ) != null ) {
        valid = digestMatches(
            hostRegion.get( ).getSslCertificateFingerprintDigest( ),
            hostRegion.get( ).getSslCertificateFingerprint( ),
            certificate );
      } else {
        valid = digestMatches(
            hostRegion.get( ).getCertificateFingerprintDigest( ),
            hostRegion.get( ).getCertificateFingerprint( ),
            certificate );
      }
    }
    return valid;
  }

  public boolean isRegionCertificate( final X509Certificate certificate ) {
    boolean found = false;
    final Optional<RegionConfiguration> configurationOptional = regionConfigurationSupplier.get( );
    if ( configurationOptional.isPresent( ) ) {
      final RegionConfiguration configuration = configurationOptional.get( );
      for ( final Region region : configuration ) {
        if ( digestMatches( region.getCertificateFingerprintDigest( ), region.getCertificateFingerprint( ), certificate ) ) {
          found = true;
          break;
        }
      }
    }
    return found;
  }

  private boolean digestMatches(
      final String fingerprintDigest,
      final String fingerprint,
      final X509Certificate certificate
  ) {
    try {
      final Digest digest = Digest.forAlgorithm( fingerprintDigest ).or( Digest.SHA256 );
      final byte[] regionCertificateFingerprint = certificateDigestCache.get(
          fingerprint,
          new Callable<byte[]>( ) {
            @Override
            public byte[] call( ) throws Exception {
              return BaseEncoding.base16( ).withSeparator( ":", 2 ).decode( fingerprint );
            }
          } );
      return MessageDigest.isEqual( regionCertificateFingerprint, digest.digestBinary( certificate.getEncoded( ) ) );
    } catch ( ExecutionException | CertificateEncodingException e ) {
      // skip the certificate
      return false;
    }
  }

  public boolean isValidRemoteAddress( final InetAddress inetAddress ) {
    return isValidAddress( inetAddress, RegionInfoToCidrSetTransform.REMOTE );
  }

  public boolean isValidForwardedForAddress( final String address ) {
    boolean valid = false;
    try {
      valid = isValidAddress( InetAddresses.forString( address ), RegionInfoToCidrSetTransform.FORWARDED_FOR );
    } catch ( final IllegalArgumentException e ) {
      // invalid
    }
    return valid;
  }

  private boolean isValidAddress( final InetAddress inetAddress,
                                  final NonNullFunction<RegionInfo,Set<Cidr>> cidrTransform ) {
    final Optional<RegionInfo> regionInfoOptional = getRegionInfo( );
    final Predicate<InetAddress> addressPredicate =
        Predicates.or( Iterables.concat( regionInfoOptional.transform( cidrTransform ).asSet( ) ) );
    return addressPredicate.apply( inetAddress );
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

  private enum RegionInfoToCidrSetTransform implements NonNullFunction<RegionInfo,Set<Cidr>> {
    FORWARDED_FOR {
      @Nonnull
      @Override
      public Set<Cidr> apply( final RegionInfo regionInfo ) {
        return regionInfo.getForwardedForCidrs( );
      }
    },
    REMOTE {
      @Nonnull
      @Override
      public Set<Cidr> apply( final RegionInfo regionInfo ) {
        return regionInfo.getRemoteCidrs( );
      }
    },
  }

  private enum RegionConfigurationToCidrListTransform implements NonNullFunction<RegionConfiguration,List<String>> {
    FORWARDED_FOR {
      @Nonnull
      @Override
      public List<String>  apply( final RegionConfiguration regionConfiguration ) {
        final List<String> cidrs = regionConfiguration.getForwardedForCidrs( );
        return cidrs == null ?
            Collections.<String>emptyList( ) :
            cidrs;
      }
    },
    REMOTE {
      @Nonnull
      @Override
      public List<String> apply( final RegionConfiguration regionConfiguration ) {
        final List<String> cidrs = regionConfiguration.getRemoteCidrs( );
        return cidrs == null ?
            Collections.<String>emptyList( ) :
            cidrs;
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

  private enum RegionServiceHostTransform implements NonNullFunction<Region,Set<String>> {
    INSTANCE;

    @Nonnull
    @Override
    public Set<String> apply( final Region region ) {
      final Set<String> hosts = Sets.newHashSet( );
      if ( region.getServices( ) != null ) for ( final Service service : region.getServices( ) ) {
        if ( service.getEndpoints( ) != null ) for ( final String uri : service.getEndpoints( ) ) {
          hosts.add( URI.create( uri ).getHost( ) );
        }
      }
      return hosts;
    }
  }

  private enum RegionInfoPartitionsTransform implements NonNullFunction<RegionInfo,Set<Integer>> {
    INSTANCE;

    @Nonnull
    @Override
    public Set<Integer> apply(final RegionInfo region ) {
      return region.getPartitions();
    }
  }

  private static NonNullFunction<Region,RegionInfo> regionToRegionInfoTransform(
      final Optional<RegionConfiguration> regionConfigurationOptional
  ) {
    return new NonNullFunction<Region,RegionInfo>( ) {
      @Nonnull
      @Override
      public RegionInfo apply( final Region region ) {
        return new RegionInfo(
            region.getName( ),
            region.getIdentifierPartitions( ),
            Collections2.transform( region.getServices( ), TypeMappers.lookup( Service.class, RegionService.class ) ),
            buildCidrs(
                regionConfigurationOptional.transform( RegionConfigurationToCidrListTransform.REMOTE ).orNull( ),
                region.getRemoteCidrs( ) ),
            buildCidrs(
                regionConfigurationOptional.transform( RegionConfigurationToCidrListTransform.FORWARDED_FOR ).orNull( ),
                region.getForwardedForCidrs( ) ),
            region.getCertificateFingerprintDigest( ),
            region.getCertificateFingerprint( ),
            region.getSslCertificateFingerprintDigest( ),
            region.getSslCertificateFingerprint( )
        );
      }
    };
  }

  private static Set<Cidr> buildCidrs( final List<String> cidrList, final List<String> regionCidrList ) {
    final Set<String> cidrs = Sets.newLinkedHashSet( );
    if ( cidrList != null ) cidrs.addAll( cidrList );
    if ( regionCidrList != null ) cidrs.addAll( regionCidrList );
    if ( cidrs.isEmpty( ) ) cidrs.add( "0.0.0.0/0" );
    return Sets.newLinkedHashSet( Iterables.transform( cidrs, Cidr.parseUnsafe( ) ) );
  }

  @TypeMapper
  private enum ServiceToRegionServiceTransform implements Function<Service,RegionService> {
    INSTANCE;

    @Nullable
    @Override
    public RegionService apply( @Nullable final Service service ) {
      return service == null ?
          null :
          new RegionService( service.getType(), service.getEndpoints() );
    }
  }

  private enum RegionInfoToRegionServiceTransform implements NonNullFunction<RegionInfo, Iterable<com.eucalyptus.auth.RegionService>> {
    INSTANCE;

    @Nonnull
    @Override
    public Iterable<com.eucalyptus.auth.RegionService> apply( final RegionInfo region ) {
      final Collection<com.eucalyptus.auth.RegionService> services = Lists.newArrayList( );
      for ( final RegionService service : region.getServices( ) ) {
        for ( final String endpoint : service.getEndpoints( ) ) {
          services.add( new com.eucalyptus.auth.RegionService(
            region.getName( ),
            service.getType( ),
            endpoint
          ) );
        }
      }
      return services;
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

  public static class ConfiguredRegionProvider implements Regions.RegionProvider {
    private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );
    private final Supplier<RegionInfo> generatedRegionInfo = regionGeneratingSupplier( );

    @Override
    public List<com.eucalyptus.auth.RegionService> getRegionServicesByType( final String serviceType ) {
      final Optional<RegionInfo> configuredRegionInfo = regionConfigurationManager.getRegionInfo( );
      final RegionInfo localRegionInfo = configuredRegionInfo.or( generatedRegionInfo );
      final Ordering<com.eucalyptus.auth.RegionService> ordering =
          Ordering.natural().onResultOf( regionName( ) ).compound( Ordering.natural( ).onResultOf( serviceType( ) ) );
      final NonNullFunction<RegionInfo, Iterable<com.eucalyptus.auth.RegionService>> transformer =
          RegionInfoToRegionServiceTransform.INSTANCE;
      final Set<com.eucalyptus.auth.RegionService> services = Sets.newTreeSet( ordering );
      Iterables.addAll( services, transformer.apply( localRegionInfo ) );
      Iterables.addAll( services, Iterables.concat(
          Iterables.transform( regionConfigurationManager.getRegionInfos( ), transformer ) ) );
      return Lists.newArrayList( Iterables.filter(
          services,
          CollectionUtils.propertyPredicate( serviceType, serviceType( ) ) ) );
    }

    private static Supplier<RegionInfo> regionGeneratingSupplier( ) {
      return new Supplier<RegionInfo>( ) {
        @Override
        public RegionInfo get( ) {
          return new RegionInfo(
              RegionConfigurations.getRegionName( ).or( "eucalyptus" ),
              Collections.singleton( 0 ),
              Lists.newArrayList( Optional.presentInstances( Iterables.transform(
                  Iterables.filter( ComponentIds.list( ), ComponentIds.lookup( ApiEndpointServicesGroup.class ) ),
                  ComponentIdToRegionServiceTransform.INSTANCE ) ) ),
              Collections.<Cidr>emptySet( ),
              Collections.<Cidr>emptySet( ),
              null,
              null,
              null,
              null
          );
        }
      };
    }

    private enum ComponentIdToRegionServiceTransform implements NonNullFunction<ComponentId,Optional<RegionService>> {
      INSTANCE;

      @Nonnull
      @Override
      public Optional<RegionService> apply( final ComponentId componentId ) {
        final Iterable<ServiceConfiguration> serviceConfigurations = Components.lookup( componentId ).services( );
        return serviceConfigurations == null || Iterables.isEmpty( serviceConfigurations ) ?
            Optional.<RegionService>absent( ) :
            Optional.of( new RegionService(
                componentId.name( ),
                Sets.newTreeSet( Iterables.transform(
                    serviceConfigurations,
                    ServiceConfigurations.remotePublicify( ) ) ) ) );
      }
    }
  }
}
