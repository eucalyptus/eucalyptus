/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
import javaslang.Tuple;
import javaslang.collection.Stream;

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
