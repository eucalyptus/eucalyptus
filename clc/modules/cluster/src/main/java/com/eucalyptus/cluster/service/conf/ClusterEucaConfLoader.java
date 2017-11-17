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
package com.eucalyptus.cluster.service.conf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import io.vavr.Tuple;
import io.vavr.collection.Stream;

/**
 *
 */
@ComponentNamed( "clusterEucalyptusConfigurationLoader" )
public class ClusterEucaConfLoader {
  private final Supplier<Map<String, String>> propertiesSupplier;

  public ClusterEucaConfLoader( ) {
    this( ( ) -> Stream.ofAll( loadEucalyptusConf( ).entrySet( ) ).toJavaMap(
        HashMap::new,
        entry -> Tuple.of( String.valueOf( entry.getKey( ) ), String.valueOf( entry.getValue( ) ) ) ) );
  }

  public ClusterEucaConfLoader( final Supplier<Map<String, String>> propertiesSupplier ) {
    this.propertiesSupplier = propertiesSupplier;
  }

  public ClusterEucaConf load( final long now ) {
    return load( now, propertiesSupplier.get( ) );
  }

  public ClusterEucaConf load( final long now, Map<String, String> properties ) {
    return new ClusterEucaConf(
        now,
        getTrimmedDequotedProperty( properties, "SCHEDPOLICY", "ROUNDROBIN" ),
        Sets.newLinkedHashSet( getFilteredDequotedPropertyList( properties, "NODES", "", InetAddresses::isInetAddress ) ),
        getTrimmedDequotedMappedProperty( properties, "NC_PORT", "8775", Integer::valueOf ),
        getTrimmedDequotedMappedProperty( properties, "MAX_INSTANCES_PER_CC", "10000", Integer::valueOf ),
        Math.max( 30, getTrimmedDequotedMappedProperty( properties, "INSTANCE_TIMEOUT", "300", Integer::valueOf ) ) );
  }

  private static Properties loadEucalyptusConf( ) {
    final Properties properties = new Properties( );
    File eucalyptusConf = BaseDirectory.CONF.getChildFile( "eucalyptus.conf" );
    if ( eucalyptusConf.canRead( ) && eucalyptusConf.isFile( ) ) {
      try ( final BufferedInputStream input = new BufferedInputStream( new FileInputStream( eucalyptusConf ) ) ) {
        properties.load( input );
      } catch ( IOException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
    return properties;
  }

  private static String getTrimmedDequotedProperty( Map<String, String> properties, String name, String defaultValue ) {
    final String perhapsQuotedValue = properties.getOrDefault( name, defaultValue ).trim( );
    return perhapsQuotedValue.startsWith("\"") && perhapsQuotedValue.endsWith("\"") ?
        perhapsQuotedValue.substring( 1, perhapsQuotedValue.length() - 1 ) :
        perhapsQuotedValue;

  }

  private static <R> R getTrimmedDequotedMappedProperty( Map<String, String> properties, String name, String defaultValue, Function<String,R> mapper ) {
    return mapper.apply( getTrimmedDequotedProperty( properties, name, defaultValue ) );
  }

  private static List<String> getFilteredDequotedPropertyList( Map<String, String> properties, String name, String defaultValue, Predicate<String> filter ) {
    final Splitter splitter = Splitter.on( ' ' ).trimResults( ).omitEmptyStrings( );
    return Stream.ofAll( splitter.split( getTrimmedDequotedProperty( properties, name, defaultValue ) ) )
        .filter( filter )
        .toJavaList( );
  }
}
