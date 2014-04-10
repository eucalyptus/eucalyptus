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

package com.eucalyptus.compute.metadata;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.ByteArray;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.vm.MetadataRequest;
import com.eucalyptus.vm.NetworkGroupsMetadata;
import com.eucalyptus.vm.SensorsConfigMetadata;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class VmMetadata {
  private static//
  Logger                                                      LOG                       = Logger.getLogger( VmMetadata.class );
 
  private static//
  Function<MetadataRequest, ByteArray>                        dynamicFunc               = new Function<MetadataRequest, ByteArray>( ) {
                                                                                          public ByteArray apply( MetadataRequest arg0 ) {
                                                                                            return ByteArray.newInstance( "" );
                                                                                          }
                                                                                        };
  private static//
  Function<MetadataRequest, ByteArray>                        userDataFunc              = new Function<MetadataRequest, ByteArray>( ) {
                                                                                          public ByteArray apply( MetadataRequest arg0 ) {
                                                                                            try ( final TransactionResource db = Entities.transactionFor( VmInstance.class ) ) {
                                                                                              final VmInstance instance = VmInstances.lookup( arg0.getVmInstanceId( ) );
                                                                                              return ByteArray.newInstance( instance.getUserData() );
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
                                                                                              final String res = instance.getByKey( arg0.getLocalPath( ) );
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
                                                                                                  for ( String key : keySet( ) ) {
                                                                                                    if ( !"".equals( key )
                                                                                                         && get( key ).apply( arg0 ) != null ) {
                                                                                                      listing += key + "\n";
                                                                                                    }
                                                                                                  }
                                                                                                  listing = listing.replaceAll( "\n$", "" );
                                                                                                  return ByteArray.newInstance( listing );
                                                                                                }
                                                                                              } );
                                                                                            put( "dynamic", dynamicFunc );
                                                                                            put( "user-data", cache( userDataFunc, VmInstances.VM_METADATA_USER_DATA_CACHE ) );
                                                                                            put( "meta-data", cache( metaDataFunc, VmInstances.VM_METADATA_INSTANCE_CACHE ) );
                                                                                          }
                                                                                        };
  
  private static//
  ConcurrentMap<String, Function<MetadataRequest, ByteArray>> //
                                                              systemMetadataEndpoints   = new ConcurrentSkipListMap<String, Function<MetadataRequest, ByteArray>>( ) {
                                                                                          {
                                                                                            put( "",
                                                                                              new Function<MetadataRequest, ByteArray>( ) {
                                                                                                public ByteArray apply( MetadataRequest arg0 ) {
                                                                                                  return ByteArray.newInstance( Joiner.on(
                                                                                                    "\n" ).join(
                                                                                                                            keySet( ) ) );
                                                                                                }
                                                                                              } );
                                                                                            put( "network-topology", new NetworkGroupsMetadata( ) );
                                                                                            put( "sensors-conf", new SensorsConfigMetadata( ) );
                                                                                          }
                                                                                        };

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


  public byte[] handle( final String path ) {
    final String[] parts = path.split( ":" );
    try {
      final String requestIp = parts[0];
      final MetadataRequest request = new MetadataRequest(
          requestIp,
          parts.length == 2 ?
              parts[1] :
              "/",
          ipToVmIdCache.get( requestIp ) );

      if ( instanceMetadataEndpoints.containsKey( request.getMetadataName( ) ) && request.isInstance( ) ) {
        return instanceMetadataEndpoints.get( request.getMetadataName( ) ).apply( request ).getBytes( );
      } else if ( systemMetadataEndpoints.containsKey( request.getMetadataName( ) ) && request.isSystem( ) ) {
        return systemMetadataEndpoints.get( request.getMetadataName( ) ).apply( request ).getBytes( );
      } else if ( publicMetadataEndpoints.containsKey( request.getMetadataName( ) ) ) {
        return publicMetadataEndpoints.get( request.getMetadataName( ) ).apply( request ).getBytes( );
      } else {
        throw new NoSuchElementException( "Metadata request failed: " + path ); 
      }
    } catch ( NoSuchElementException ex ) {
      throw ex;
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
