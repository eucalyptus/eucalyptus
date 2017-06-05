package com.eucalyptus.cluster.service.conf

import com.eucalyptus.component.annotation.ComponentNamed
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic

import java.util.function.Supplier

/**
 *
 */
@CompileStatic
@ComponentNamed("clusterEucalyptusConfigurationLoader")
class ClusterEucaConfLoader {

  private final Supplier<Map<String,String>> propertiesSupplier;

  ClusterEucaConfLoader( ) {
    this( { loadEucalyptusConf( ).collectEntries( Maps.<String,String>newHashMap( ) ){
      Object key, Object value -> [ String.valueOf(key), String.valueOf(value) ]
    } } as Supplier<Map<String,String>> )
  }

  ClusterEucaConfLoader( final Supplier<Map<String,String> > propertiesSupplier ) {
    this.propertiesSupplier = propertiesSupplier;
  }

  ClusterEucaConf load( final long now ) {
    load( now, propertiesSupplier.get( ) )
  }

  ClusterEucaConf load( final long now, Map<String,String> properties ) {
    new ClusterEucaConf(
        now,
        getTrimmedDequotedProperty( properties, 'SCHEDPOLICY', 'ROUNDROBIN' ),
        Sets.newLinkedHashSet( getFilteredDequotedPropertyList( properties, 'NODES', '', InetAddresses.&isInetAddress ) ),
        getTrimmedDequotedMappedProperty( properties, 'NC_PORT', '8775', Integer.&valueOf ) as Integer,
        getTrimmedDequotedMappedProperty( properties, 'MAX_INSTANCES_PER_CC', '10000', Integer.&valueOf ) as Integer,
        Math.max( 30, getTrimmedDequotedMappedProperty( properties, 'INSTANCE_TIMEOUT', '300', Integer.&valueOf ) as Integer)
    )
  }

  private static Properties loadEucalyptusConf( ) {
    Properties properties = new Properties()
    File eucalyptusConf = new File( "${System.getenv('EUCALYPTUS')}/etc/eucalyptus/eucalyptus.conf" )
    if ( eucalyptusConf.canRead( ) && eucalyptusConf.isFile( ) ) {
      eucalyptusConf.newInputStream( ).withStream{ BufferedInputStream input ->
        properties.load( input )
      }
    }
    properties
  }

  private static String getTrimmedDequotedProperty( Map<String,String> properties, String name, String defaultValue = '' ) {
    properties.get( name, defaultValue ).trim( ).with{
      startsWith('"') && endsWith('"') ? substring( 1, length() - 1 ) : toString( )
    }.trim( )
  }

  private static <T> T getTrimmedDequotedMappedProperty( Map<String,String> properties, String name, String defaultValue, Closure<T> mapper ) {
    mapper( getTrimmedDequotedProperty( properties, name, defaultValue ) )
  }

  private static List<String> getFilteredDequotedPropertyList( Map<String,String> properties, String name, String defaultValue, Closure filter ) {
    final Splitter splitter = Splitter.on( ' ' ).trimResults( ).omitEmptyStrings( )
    Lists.newArrayList( splitter.split( getTrimmedDequotedProperty( properties, name, defaultValue ) ) )
        .findAll( filter ) as List<String>
  }
}
