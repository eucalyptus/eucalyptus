/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.vm.dns;

import static com.eucalyptus.util.dns.DnsResolvers.DnsRequest;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.Vpc;
import com.eucalyptus.compute.common.internal.vpc.VpcMetadataNotFoundException;
import com.eucalyptus.compute.common.internal.vpc.Vpcs;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.compute.vpc.persist.PersistenceVpcs;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.IPRange;
import com.eucalyptus.network.config.NetworkConfigurationApi.Cluster;
import com.eucalyptus.network.config.NetworkConfigurationApi.NetworkConfiguration;
import com.eucalyptus.network.config.NetworkConfigurations;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Subnets;
import com.eucalyptus.util.Subnets.SystemSubnetPredicate;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.util.dns.DnsResolvers;
import com.eucalyptus.util.dns.DnsResolvers.DnsResolver;
import com.eucalyptus.util.dns.DnsResolvers.DnsResponse;
import com.eucalyptus.util.dns.DnsResolvers.RequestType;
import com.eucalyptus.util.dns.DomainNameRecords;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.vavr.collection.Array;
import io.vavr.control.Option;

@ConfigurableClass( root = "dns.split_horizon",
                    description = "Options controlling Split-Horizon DNS resolution." )
public abstract class SplitHorizonResolver extends DnsResolver {
  private static final Logger LOG     = Logger.getLogger( SplitHorizonResolver.class );

  private static final Supplier<Boolean> privateNetworksManagedSupplier =
      Suppliers.memoizeWithExpiration( PrivateNetworksManaged.INSTANCE, 15, TimeUnit.SECONDS );

  private static final Supplier<Predicate<InetAddress>> publicAddressPredicateSupplier =
      Suppliers.memoizeWithExpiration( PublicAddresses.INSTANCE, 15, TimeUnit.SECONDS );

  private static final Supplier<Iterable<Cidr>> clusterSubnetsSupplier =
      Suppliers.memoizeWithExpiration( ClusterSubnets.INSTANCE, 1, TimeUnit.MINUTES );

  @ConfigurableField( description = "Enable the split-horizon DNS resolution for internal instance public DNS name queries.  "
                                    + "Note: dns.enable must also be 'true'", initial = "true" )
  public static Boolean       enabled = Boolean.TRUE;

  private static final LoadingCache<VmDnsCacheKey, Optional<VmDnsInfo>> instanceCache = CacheBuilder.newBuilder( )
      .maximumSize( 25_000 )
      .refreshAfterWrite( 5, TimeUnit.SECONDS )
      .build( new VmDnsCacheLoader( ) );

  private static final LoadingCache<String,Optional<VpcDnsInfo>> vpcCache = CacheBuilder.newBuilder( )
      .refreshAfterWrite( 1, TimeUnit.MINUTES )
      .build( new VpcDnsCacheLoader( ) );

  /**
   * Resolve the network (cidr) for an instance (i.e. a VPC or EC2-Classic)
   */
  public static Optional<Cidr> lookupNetwork( final InetAddress address ) {
    final Optional<VmDnsInfo> vmInfo = privateNetworksManagedSupplier.get() ?
        lookupPublic( address ) :
        lookupAny( address );
    if ( vmInfo.isPresent( ) ) {
      final String vpcId = vmInfo.get( ).getVpcId( );
      if (  vpcId != null ) {
        return vpcCache.getUnchecked( vpcId ).transform( VpcDnsInfo.cidr( ) );  
      } else {
        final InetAddress privateAddress = InetAddresses.forString( vmInfo.get( ).getPrivateIp( ) );
        for ( final Cidr cidr : clusterSubnetsSupplier.get( ) ) {
          if ( cidr.apply( privateAddress ) ) {
            return Optional.of( cidr );
          }
        }
      }
    }
    return Optional.absent( );
  }

  /**
   * Test whether the address is one which belongs to an instance or is external.
   * 
   * The checks are as follows:
   * 1. If the system has not yet fully bootstrapped, return false.
   * 2. If the address is a registered public address, return true.
   * 3. If the address falls w/in a subnet owned by a registered cluster, return true.
   * 4. Otherwise, return false.
   */
  @SystemSubnetPredicate
  public static class SystemInternalSubnet implements Predicate<InetAddress> {
    
    /**
     * @see com.google.common.base.Predicate#apply(Object)
     * @return true if the address is internal
     */
    @Override
    public boolean apply( final InetAddress input ) {
      if ( !Bootstrap.isOperational( ) ) {
        return false;
      } else if ( publicAddressPredicateSupplier.get( ).apply( input ) ) {
        return true;
      } else {
        return lookupPublic( input ).isPresent( ) ||
            !privateNetworksManagedSupplier.get( ) && Predicates.or( clusterSubnetsSupplier.get( ) ).apply( input );
      }
    }
  }
  
  /**
   * Do the split-horizon DNS lookup. The request here is necessarily from an internal instance
   * because {@link SplitHorizonResolver#checkAccepts(DnsRequest)} only allows for
   * source addresses which are system internal.
   * 
   * The procedure is to:
   * 1. Check we can parse the subdomain; otherwise fail w/ UNKNOWN
   * 2. Parse out the ip address; otherwise fail w/ NXDOMAIN
   * 3. Verify the existence of an instance for the indicate ip; otherwise fail w/ NXDOMAIN
   * 4. Construct the response record accordingly; otherwise fail w/ NXDOMAIN
   * 
   * @see DnsResolvers#findRecords(org.xbill.DNS.Message, DnsResolvers.DnsRequest)
   */
  @Override
  public DnsResponse lookupRecords( DnsRequest request ) {
    final Record query = request.getQuery( );
    try{
      if ( RequestType.PTR.apply( query ) ) {
        final InetAddress ip = DomainNameRecords.inAddrArpaToInetAddress( query.getName( ) );
        final Optional<VmDnsInfo> vmInfo = lookupAny( ip );
        if ( vmInfo.isPresent( ) ) {
          final String hostAddress = ip.getHostAddress( );
          if ( hostAddress.equals( vmInfo.get( ).getPrivateIp( ) ) && ip.isSiteLocalAddress()) {
            final Name dnsName = InstanceDomainNames.fromInetAddress( InstanceDomainNames.INTERNAL, ip );
            return DnsResponse.forName( query.getName( ) ).answer( DomainNameRecords.ptrRecord( dnsName, ip ) );
          } else if ( hostAddress.equals( vmInfo.get( ).getPublicIp( ) ) ) {
            final Name dnsName = InstanceDomainNames.fromInetAddress( InstanceDomainNames.EXTERNAL, ip );
            return DnsResponse.forName( query.getName( ) ).answer( DomainNameRecords.ptrRecord( dnsName, ip ) );
          }else {
            return null;
          }
        } else {
          // reverse lookup of non-EUCA ip will be resolved by recursive resolver
          return null;
        }
      } 
    }catch( final Exception ex ){
      LOG.debug(ex);
    }
    return DnsResponse.forName( query.getName( ) ).nxdomain( );
  }


  /**
   * Enforces that this resolver is only used under the following conditions:
   * 1. The system is currently operational (e.g., database access is safe)
   * 2. This resolver is enabled
   * 3. The source ip address is system controlled; either a public address or in a vnet subnet
   * 4. The request name is a subdomain request for the subdomains the system should respond
   *
   * @see com.eucalyptus.util.dns.DnsResolvers#findRecords(org.xbill.DNS.Message, DnsRequest)
   */
  @Override
  public boolean checkAccepts( final DnsRequest request ) {
    final Record query = request.getQuery( );
    if ( !Bootstrap.isOperational( ) || !enabled ) {
      return false;
    } else if ( InstanceDomainNames.isInstanceDomainName( query.getName( ) ) ) {
      return true;
    } else if ( RequestType.PTR.apply( query ) ) {
      return true;
    }
    return false;
  }

  private static Optional<VmDnsInfo> lookupAny( InetAddress ip ) {
    return lookupPrivate( ip ).or( lookupPublic( ip ) );
  }

  private static Optional<VmDnsInfo> lookupPrivate( InetAddress ip ) {
    return instanceCache.getUnchecked( VmDnsCacheKey.forPrivateAddress( ip ) );
  }

  private static Optional<VmDnsInfo> lookupPublic( InetAddress ip ) {
    return instanceCache.getUnchecked( VmDnsCacheKey.forPublicAddress( ip ) );
  }

  @Override
  public String toString( ) {
    return this.getClass( ).getSimpleName( );
  }

  @SuppressWarnings( "EqualsWhichDoesntCheckParameterClass" )
  @Override
  public boolean equals( Object obj ) {
    return this.getClass( ).equals( MoreObjects.firstNonNull( Classes.typeOf( obj ), Object.class ) );
  }  

  public static class InternalARecordResolver extends SplitHorizonResolver {
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if( RequestType.PTR.apply( query ) )
        return super.lookupRecords( request );
      try {
        final Name name = query.getName( );
        final Name instanceDomain = InstanceDomainNames.lookupInstanceDomain( name );
        final InetAddress ip = InstanceDomainNames.toInetAddress( name.relativize( instanceDomain ) );
        if ( ( privateNetworksManagedSupplier.get( ) && ip.isSiteLocalAddress( ) ) || 
            lookupPrivate( ip ).isPresent( ) ) {
          if ( RequestType.A.apply( query ) ) {
            final Record rec = DomainNameRecords.addressRecord( name, ip );
            return DnsResponse.forName( name ).answer( rec );
          } else {
            return DnsResponse.forName( name ).answer( Lists.<Record>newArrayList() );
          }
        }
      } catch ( Exception ex ) {
        LOG.debug( ex );
      }
      return super.lookupRecords( request );
    }
    
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && ( InstanceDomainNames.isInstanceSubdomain( query.getName( ) )
            && !query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) ) );
    }
    
  }

  /**
   * Handle instance public DNS name lookup from instances
   */
  public static class HorizonARecordResolver extends SplitHorizonResolver {
    
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      final InetAddress source = request.getRemoteAddress( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && lookupNetwork( source ).isPresent( )
            && query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) );
    }

    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if( RequestType.PTR.apply( query ) )
        return super.lookupRecords( request );
      try {
        final Name name = query.getName( );
        final InetAddress requestIp = InstanceDomainNames.toInetAddress( name.relativize( InstanceDomainNames.EXTERNAL.get( ) ) );
        final Optional<Cidr> instanceNetwork = lookupNetwork( request.getRemoteAddress( ) );
        final Optional<VmDnsInfo> vmInfo = lookupPublic( requestIp );
        if ( vmInfo.isPresent( ) ) {
          final InetAddress instanceAddress =
              instanceNetwork.isPresent( ) && instanceNetwork.get( ).contains( vmInfo.get( ).getPrivateIp( ) ) ?
                  InetAddresses.forString( vmInfo.get( ).getPrivateIp( ) ) :
                  requestIp;
          if ( RequestType.A.apply(query) ) {
            final Record rec = DomainNameRecords.addressRecord( name, instanceAddress );
            return DnsResponse.forName( name ).answer( rec );
          } else {
            return DnsResponse.forName( name ).answer( Lists.<Record>newArrayList() );
          }
        }
      } catch ( Exception ex ) {
        LOG.debug( ex );
      }
      return super.lookupRecords( request );
    }
    
  }

  /**
   * Handle instance public DNS name lookup from external hosts
   */
  public static class ExternalARecordResolver extends SplitHorizonResolver {
    @Override
    public boolean checkAccepts( DnsRequest request ) {
      final Record query = request.getQuery( );
      final InetAddress source = request.getRemoteAddress( );
      return RequestType.PTR.apply( query ) ?
        super.checkAccepts( request ) :
        super.checkAccepts( request )
            && !lookupNetwork( source ).isPresent( )
            && query.getName( ).subdomain( InstanceDomainNames.EXTERNAL.get( ) );
    }
    
    @Override
    public DnsResponse lookupRecords( DnsRequest request ) {
      final Record query = request.getQuery( );
      if( RequestType.PTR.apply( query ) )
        return super.lookupRecords( request );
      try {
        final Name name = query.getName( );
        final InetAddress requestIp = InstanceDomainNames.toInetAddress( name.relativize( InstanceDomainNames.EXTERNAL.get( ) ) );
        if ( lookupPublic( requestIp ).isPresent( ) ) {
          if ( RequestType.A.apply( query ) ) {
            final Record rec = DomainNameRecords.addressRecord( name, requestIp );
            return DnsResponse.forName( name ).answer( rec );
          } else {
            return DnsResponse.forName( name ).answer( Lists.<Record>newArrayList( ) );
          }
        }
      } catch ( Exception ex ) {
        LOG.debug( ex );
      }
      return super.lookupRecords( request );
    }
  }
  
  private static final class VmDnsCacheLoader extends CacheLoader<VmDnsCacheKey, Optional<VmDnsInfo>> {
    @Override
    public Optional<VmDnsInfo> load( @Nonnull final VmDnsCacheKey key ) {
      return key.load( Optional.<VmDnsInfo>absent( ) );
    }

    @Override
    public ListenableFuture<Optional<VmDnsInfo>> reload( final VmDnsCacheKey key, final Optional<VmDnsInfo> oldValue ) {
      if ( Databases.isVolatile( ) ) {
        return Futures.immediateFuture( oldValue );
      } else {
        return Futures.immediateFuture( key.load( oldValue ) );
      }
    }
  }

  private static final class VpcDnsCacheLoader extends CacheLoader<String, Optional<VpcDnsInfo>> {
    @Override
    public Optional<VpcDnsInfo> load( @Nonnull final String vpcId ) {
      try {
        final Vpcs vpcs = new PersistenceVpcs( );
        return vpcs.lookupByName( null, vpcId, new Function<Vpc, Optional<VpcDnsInfo>>( ) {
          @Override
          public Optional<VpcDnsInfo> apply( final Vpc vpc ) {
            try {
              return Optional.of( new VpcDnsInfo(
                  vpc.getDisplayName( ),
                  Cidr.parse( vpc.getCidr( ) )
              ) );
            } catch ( IllegalArgumentException e ) {
              return Optional.absent( );
            }
          }
        } );
      } catch ( VpcMetadataNotFoundException e ) {
        LOG.debug( "VPC not found for instance network " + vpcId );
      } catch ( Throwable t ) {
        LOG.error( "Error finding VPC for instance network " + vpcId, t );
      }
      return Optional.absent( );
    }

    @Override
    public ListenableFuture<Optional<VpcDnsInfo>> reload( final String key, final Optional<VpcDnsInfo> oldValue ) {
      if ( Databases.isVolatile( ) ) {
        return Futures.immediateFuture( oldValue );
      } else {
        return Futures.immediateFuture( load( key ).or( oldValue ) );
      }
    }
  }

  private static abstract class VmDnsCacheKey {
    @Nonnull public static VmDnsCacheKey forPublicAddress( InetAddress address ) {
      return new VmPublicIpDnsCacheKey( address.getHostAddress( ) );
    }

    @Nonnull public static VmDnsCacheKey forPrivateAddress( InetAddress address ) {
      return new VmPrivateIpDnsCacheKey( address.getHostAddress( ) );
    }

    Optional<VmDnsInfo> load( Optional<VmDnsInfo> currentValue ) {
      try ( TransactionResource db = Entities.readOnlyDistinctTransactionFor( VmInstance.class ) ) {
        final VmInstance example = example( );
        final Conjunction conjunction = Restrictions.conjunction( );
        if ( currentValue.isPresent( ) ) {
          // Load either a different VM than the current one
          // or the current one if it has been modified
          conjunction.add( Restrictions.or(
              Restrictions.and(
                  Restrictions.eq( "naturalId", currentValue.get( ).uuid ),
                  Restrictions.ne( "version", currentValue.get( ).version )
              ),
              Restrictions.ne( "naturalId", currentValue.get( ).uuid )
          ) );
        }
        final VmInstance vm = ( VmInstance ) Entities.createCriteriaUnique( VmInstance.class )
            .add( Example.create( example ) )
            .add( conjunction )
            .uniqueResult( );
        if ( vm == null ) {
          return currentValue;
        } else if ( VmInstance.VmStateSet.RUN.apply( vm ) ) {
          return Optional.of( new VmDnsInfo(
              vm.getNaturalId( ),
              vm.getDisplayName( ),
              vm.getVersion( ),
              vm.getPrivateAddress( ),
              vm.hasPublicAddress( ) ? vm.getPublicAddress( ) : null,
              vm.getVpcId( )
          ) );
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
      }
      return Optional.absent( );
    }

    abstract VmInstance example( );
  }

  private static final class VmPublicIpDnsCacheKey extends VmDnsCacheKey {
    private final String ip;

    public VmPublicIpDnsCacheKey( final String ip ) {
      this.ip = ip;
    }

    @Override
    VmInstance example( ) {
      return VmInstance.exampleWithPublicIp( ip );
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final VmPublicIpDnsCacheKey that = (VmPublicIpDnsCacheKey) o;
      return Objects.equals( ip, that.ip );
    }

    @Override
    public int hashCode() {
      return Objects.hash( ip );
    }
  }

  private static final class VmPrivateIpDnsCacheKey extends VmDnsCacheKey {
    private final String ip;

    public VmPrivateIpDnsCacheKey( final String ip ) {
      this.ip = ip;
    }

    @Override
    VmInstance example( ) {
      return VmInstance.exampleWithPrivateIp( ip );
    }
    
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final VmPrivateIpDnsCacheKey that = (VmPrivateIpDnsCacheKey) o;
      return Objects.equals( ip, that.ip );
    }

    @Override
    public int hashCode() {
      return Objects.hash( ip );
    }
  }

  static final class VmDnsInfo {
    private final String uuid;
    private final String id;
    private final Integer version;
    private final String privateIp;
    @Nullable
    private final String publicIp;
    @Nullable
    private final String vpcId;

    public VmDnsInfo(
        final String uuid,
        final String id,
        final Integer version,
        final String privateIp,
        final String publicIp,
        final String vpcId
    ) {
      this.uuid = uuid;
      this.id = id;
      this.version = version;
      this.privateIp = privateIp;
      this.publicIp = publicIp;
      this.vpcId = vpcId;
    }

    public String getPrivateIp( ) {
      return privateIp;
    }

    @Nullable
    public String getPublicIp( ) {
      return publicIp;
    }

    @Nullable
    public String getVpcId( ) {
      return vpcId;
    }
  }

  private static final class VpcDnsInfo {
    private final String id;
    private final Cidr cidr;

    public VpcDnsInfo(
        final String id,
        final Cidr cidr
    ) {
      this.id = id;
      this.cidr = cidr;
    }

    public Cidr getCidr( ) {
      return cidr;
    }

    static Function<VpcDnsInfo,Cidr> cidr( ) {
      return VpcDnsInfoProperties.CIDR;
    }

    enum VpcDnsInfoProperties implements Function<VpcDnsInfo,Cidr> {
      CIDR {
        @Override
        public Cidr apply( final VpcDnsInfo vpcDnsInfo ) {
          return vpcDnsInfo.getCidr( );
        }
      },
    }
  }

  private enum PrivateNetworksManaged implements Supplier<Boolean> {
    INSTANCE {
      @Override
      public Boolean get() {
        try {
          return Networking.getInstance( ).supports( NetworkingFeature.SiteLocalManaged );
        } catch ( Exception e ) {
          return false;
        }
      }
    }
  }

  private enum PublicAddresses implements Supplier<Predicate<InetAddress>> {
    INSTANCE {
      @Override
      public Predicate<InetAddress> get() {
        Predicate<InetAddress> predicate = Predicates.alwaysFalse( );
        final Option<NetworkConfiguration> config = NetworkConfigurations.getNetworkConfiguration( );
        if ( config.isDefined( ) ) try {
          final Array<IPRange> ranges = config.get( ).publicIps().flatMap( IPRange.optParse( ) );
          predicate = new Predicate<InetAddress>( ) {
            @Override
            public boolean apply( @Nullable final InetAddress inetAddress ) {
              final IPRange addressAsRange = IPRange.parse( inetAddress.getHostAddress( ) );
              boolean contained = false;
              for ( final IPRange range : ranges ) {
                contained = range.contains( addressAsRange );
                if ( contained ) break;
              }
              return contained;
            }
          };
        } catch ( Exception e ) {
          LOG.error( "Error building public address predicate", e );
        }
        return predicate;
      }
    }
  }

  private enum ClusterSubnets implements Supplier<Iterable<Cidr>> {
    INSTANCE {
      private final AtomicReference<Iterable<Cidr>> lastLoaded = new AtomicReference<>( Array.empty( ) );

      @Override
      public Iterable<Cidr> get( ) {
        final Option<NetworkConfiguration> configurationOptional = NetworkConfigurations.getNetworkConfiguration( );
        if ( !configurationOptional.isDefined( ) || Databases.isVolatile( ) ) {
          return lastLoaded.get( );
        } else try {
          final Set<String> clusters = Components.services( ClusterController.class )
              .map( ServiceConfiguration::getPartition )
              .toJavaSet( );
          final NetworkConfiguration configuration =
              NetworkConfigurations.explode( configurationOptional.get( ), clusters );
          final Array<Cidr> cidrs = configuration.clusters( )
              .flatMap( Cluster::subnet )
              .flatMap( FUtils.vOption( FUtils.optional( ThrowingFunction.undeclared(
                  subnet -> Subnets.cidr( subnet.subnet( ).get(), subnet.netmask( ).get( ) )
              ) ) ) );
          lastLoaded.set( cidrs );
          return cidrs;
        } catch ( final Throwable e ) {
          LOG.error( "Error reloading cluster information, using cached value", e );
          return lastLoaded.get( );
        }
      }
    }
  }  
}
