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

package com.eucalyptus.compute.metadata;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.network.Networking;
import com.eucalyptus.compute.common.network.NetworkingFeature;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.ByteArray;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.vm.MetadataRequest;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.vm.VmInstanceMetadata;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ComponentNamed("computeVmMetadata")
public class VmMetadata {
  private static//
  Logger                                                      LOG                       = Logger.getLogger( VmMetadata.class );

  private static//
  Function<MetadataRequest, ByteArray>                        dynamicFunc               = new Function<MetadataRequest, ByteArray>( ) {
                                                                                          public ByteArray apply( MetadataRequest arg0 ) {
                                                                                            try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
                                                                                              final VmInstance instance = VmInstances.lookup( arg0.getVmInstanceId() );
                                                                                              final String res = VmInstanceMetadata.getDynamicByKey( instance, arg0.getLocalPath() );
                                                                                              if ( res != null ) {
                                                                                                return ByteArray.newInstance( res );
                                                                                              }
                                                                                            } catch ( NoSuchElementException e ) {
                                                                                              // fall through and throw
                                                                                            }
                                                                                            throw new NoSuchElementException( "Failed to lookup path: " + arg0.getLocalPath( ) );

                                                                                          }
                                                                                        };
  private static//
  Function<MetadataRequest, ByteArray>                        userDataFunc              = new Function<MetadataRequest, ByteArray>( ) {
                                                                                          public ByteArray apply( MetadataRequest arg0 ) {
                                                                                            try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
                                                                                              final VmInstance instance = VmInstances.lookup( arg0.getVmInstanceId( ) );
                                                                                              final byte[] userData = instance.getUserData( );
                                                                                              if ( userData == null ) {
                                                                                                throw new NoSuchElementException( );
                                                                                              }
                                                                                              return ByteArray.newInstance( userData );
                                                                                            } catch ( NoSuchElementException e ) {
                                                                                              throw new NoSuchElementException( "Failed to lookup path: " + arg0.getLocalPath( ) );
                                                                                            }
                                                                                          }
                                                                                        };
  private static//
  Function<MetadataRequest, ByteArray>                        metaDataFunc              = new Function<MetadataRequest, ByteArray>( ) {
                                                                                          public ByteArray apply( MetadataRequest arg0 ) {
                                                                                            try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
                                                                                              final VmInstance instance = VmInstances.lookup( arg0.getVmInstanceId() );
                                                                                              final String res = VmInstanceMetadata.getByKey( instance, arg0.getLocalPath() );
                                                                                              if ( res != null ) {
                                                                                                return ByteArray.newInstance( res );
                                                                                              }
                                                                                            } catch ( NoSuchElementException e ) {
                                                                                              // fall through and throw
                                                                                            }
                                                                                            throw new NoSuchElementException( "Failed to lookup path: " + arg0.getLocalPath( ) );
                                                                                          }
                                                                                        };

  private static//
  ConcurrentMap<String, Function<MetadataRequest, ByteArray>> publicMetadataEndpoints   = new ConcurrentSkipListMap<String, Function<MetadataRequest, ByteArray>>( ) {
                                                                                          {
                                                                                            put( "",
                                                                                              new Function<MetadataRequest, ByteArray>( ) {
                                                                                                public ByteArray apply( MetadataRequest arg0 ) {
                                                                                                  return ByteArray.newInstance( Joiner.on( "\n" ).join(
                                                                                                    keySet( ) ) );
                                                                                                }
                                                                                              } );
                                                                                          }
                                                                                        };
  private static//
  ConcurrentMap<String, Function<MetadataRequest, ByteArray>> instanceMetadataEndpoints = new ConcurrentSkipListMap<String, Function<MetadataRequest, ByteArray>>( ) {
                                                                                          {
                                                                                            put( "",
                                                                                              new Function<MetadataRequest, ByteArray>( ) {
                                                                                                public ByteArray apply( MetadataRequest arg0 ) {
                                                                                                  String listing = "";
                                                                                                  for ( String key : keySet( ) ) try {
                                                                                                    if ( !"".equals( key )
                                                                                                         && get( key ).apply( arg0 ) != null ) {
                                                                                                      listing += key + "\n";
                                                                                                    }
                                                                                                  } catch ( final RuntimeException e ) {
                                                                                                    if ( !Exceptions.isCausedBy( e, NoSuchElementException.class ) ) {
                                                                                                      throw e;
                                                                                                    }
                                                                                                  }
                                                                                                  listing = listing.replaceAll( "\n$", "" );
                                                                                                  return ByteArray.newInstance( listing );
                                                                                                }
                                                                                              } );
                                                                                            put( "dynamic", cache( dynamicFunc, VmInstances.VM_METADATA_INSTANCE_CACHE ) );
                                                                                            put( "user-data", cache( userDataFunc, VmInstances.VM_METADATA_USER_DATA_CACHE ) );
                                                                                            put( "meta-data", cache( metaDataFunc, VmInstances.VM_METADATA_INSTANCE_CACHE ) );
                                                                                          }
                                                                                        };

  private static final Supplier<Set<NetworkingFeature>> networkingFeatureSupplier =
      Suppliers.memoizeWithExpiration( new Supplier<Set<NetworkingFeature>>( ) {
        @Override
        public Set<NetworkingFeature> get( ) {
          return Networking.getInstance( ).describeFeatures( );
        }
      }, 30, TimeUnit.SECONDS );

  private static final LoadingCache<String, Optional<String>> ipToVmIdCache =
      cache( resolveVm(), VmInstances.VM_METADATA_REQUEST_CACHE );

  private static <K,V> LoadingCache<K,V> cache( final Function<K,V> loader,
                                                final String cacheSpec ) {
    return CacheBuilder
        .from( CacheBuilderSpec.parse( cacheSpec ) )
        .build( CacheLoader.from( loader ) );
  }

  private static Function<String,Optional<String>> resolveVm( ) {
    return new Function<String,Optional<String>>() {
      @Nullable
      @Override
      public Optional<String> apply( final String requestIp  ) {
        VmInstance findVm = null;
        if ( !Databases.isVolatile() ) {
          try {
            findVm = VmInstances.lookupByPublicIp( requestIp );
          } catch ( Exception ex2 ) {
            try {
              findVm = VmInstances.lookupByPrivateIp( requestIp );
            } catch ( Exception ex ) {
              Logs.exhaust().error( ex );
            }
          }
        }
        return Optional.fromNullable( findVm ).transform( CloudMetadatas.toDisplayName( ) );
      }
    };
  }


  /**
   * @return byte[] for content or String for expected error
   */
  public Object handle( final String path ) {
    final String[] parts = path.split( ":", 2 );
    try {
      final String requestIpOrInstanceId = ResourceIdentifiers.tryNormalize( ).apply( parts[0] );
      final boolean isInstanceId = requestIpOrInstanceId.startsWith( "i-" );
      final MetadataRequest request = new MetadataRequest(
          isInstanceId ? "127.0.0.1" : requestIpOrInstanceId,
          parts.length == 2 ?
              parts[1] :
              "/",
          isInstanceId ? Optional.of( requestIpOrInstanceId ) : ipToVmIdCache.get( requestIpOrInstanceId ) );

      if ( instanceMetadataEndpoints.containsKey( request.getMetadataName( ) ) && request.isInstance( ) ) {
        if ( ( isInstanceId && !networkingFeatureSupplier.get( ).contains( NetworkingFeature.Vpc ) ) ||
            ( !isInstanceId && !networkingFeatureSupplier.get( ).contains( NetworkingFeature.Classic ) ) ) {
          throw new NoSuchElementException( "Metadata request failed (invalid for platform): " + path );
        }
        return instanceMetadataEndpoints.get( request.getMetadataName( ) ).apply( request ).getBytes( );
      } else if ( publicMetadataEndpoints.containsKey( request.getMetadataName( ) ) ) {
        return publicMetadataEndpoints.get( request.getMetadataName( ) ).apply( request ).getBytes( );
      } else {
        return "Metadata request failed: " + path;
      }
    } catch ( Exception ex ) {
      NoSuchElementException noSuchElementException = Exceptions.findCause( ex, NoSuchElementException.class );
      if ( noSuchElementException != null ) throw noSuchElementException;
      String errorMsg = "Metadata request failed: " + path + ( Logs.isExtrrreeeme( )
                                                                                    ? " cause: " + ex.getMessage( )
                                                                                    : "" );
      LOG.error( errorMsg, ex );
      throw Exceptions.toUndeclared( ex );
    }
  }

}
