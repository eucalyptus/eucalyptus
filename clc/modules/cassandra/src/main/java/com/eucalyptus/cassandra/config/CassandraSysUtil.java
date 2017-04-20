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
package com.eucalyptus.cassandra.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.cassandra.CassandraCluster;
import com.eucalyptus.cassandra.CassandraDirectory;
import com.eucalyptus.cassandra.CassandraKeyspaces;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Templates;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import edu.ucsb.eucalyptus.util.SystemUtil;

/**
 *
 */
class CassandraSysUtil {
  private static final Logger logger = Logger.getLogger( CassandraSysUtil.class );

  private static final String PATH_CASSANDRA = "/usr/sbin/cassandra";
  private static final String PATH_ROOTWRAP = "/usr/lib/eucalyptus/euca_rootwrap";
  private static final String PATH_EUCACASS = "/usr/libexec/eucalyptus/euca-cassandra-ctl";
  private static final String YAML_RESOURCE_NAME = "cassandra.yaml";
  private static final String RACKDC_RESOURCE_NAME = "cassandra-rackdc.properties";

  static boolean checkCassandra( ) {
    if ( !new File( PATH_CASSANDRA ).isFile( ) ||
        !new File( PATH_EUCACASS ).isFile( ) ) {
      return false;
    }
    boolean running = false;
    final int code = SystemUtil.runAndGetCode( new String[]{ PATH_ROOTWRAP, PATH_EUCACASS, "status" } );
    switch ( code ) {
      case 0:
        logger.trace( "Cassandra running" );
        running = true;
        break;
      case 1:
      case 2:
        logger.warn( "Cassandra dead, performing cleanup" );
        cleanup( );
        return false;
      case 3:
        logger.trace( "Cassandra stopped" );
        break;
      default:
        logger.warn( "Cassandra status unknown, code: " + code );
        return false;
    }
    if ( !running ) {
      return CassandraCluster.canStart( );
    }
    return true;
  }

  static void startCassandra( ) throws EucalyptusCloudException {
    final int code = SystemUtil.runAndGetCode( new String[]{ PATH_ROOTWRAP, PATH_EUCACASS, "start" } );
    if ( code != 0 ) {
      throw new EucalyptusCloudException( "Error starting cassandra, status code: " + code );
    }
  }

  static void stopCassandra( ) throws EucalyptusCloudException {
    final int code = SystemUtil.runAndGetCode( new String[]{ PATH_ROOTWRAP, PATH_EUCACASS, "stop" } );
    if ( code != 0 ) {
      throw new EucalyptusCloudException( "Error stopping cassandra, status code: " + code );
    }
  }

  static void createKeyspaces( ) {
    CassandraKeyspaces.all( CassandraCluster.datacenter( ), 1 ).forEach( t -> {
      final String keyspace = t._1( );
      final Optional<Throwable> throwableOptional = t._2( );
      if ( throwableOptional.isPresent( ) ) {
        logger.error( "Error processing keyspace " + keyspace, throwableOptional.get( ) );
      } else {
        logger.info( "Processed keyspace " + keyspace );
      }
    } );
  }

  static void createDirectories( ) {
    EnumSet.allOf( CassandraDirectory.class ).forEach( CassandraDirectory::mkdirs );
  }

  private static void cleanup( ) {
    final int code = SystemUtil.runAndGetCode( new String[]{ PATH_ROOTWRAP, PATH_EUCACASS, "clean" } );
    if ( code != 0 ) {
      logger.error( "Error running cassandra clean up, status code: " + code );
    }
  }

  static void writeConfiguration( ) throws IOException {
    Files.write(
        generateCassandraYaml( ),
        BaseDirectory.VAR.getChildFile( "cassandra", "conf", "cassandra.yaml" ),
        StandardCharsets.UTF_8 );
    Files.write(
        generateCassandraRackDcProperties( ),
        BaseDirectory.VAR.getChildFile( "cassandra", "conf", "cassandra-rackdc.properties" ),
        StandardCharsets.UTF_8 );
  }

  private static String generateCassandraYaml( ) throws IOException {
    final String name = CassandraCluster.name( );
    final String bindAddr = Internets.localHostInetAddress( ).getHostAddress( );
    final Set<String> seeds = CassandraCluster.seeds( );
    final String dir = BaseDirectory.VAR.toString( );
    final boolean autoBootstrap = CassandraCluster.autoBootstrap( );
    return generateCassandraYaml( name, bindAddr, seeds, dir, autoBootstrap );
  }

  private static String generateCassandraRackDcProperties( ) throws IOException {
    return generateCassandraRackDcProperties(
        CassandraCluster.datacenter( ),
        CassandraCluster.rack( ) );
  }

  static String generateCassandraYaml(
      final String name,
      final String bindAddr,
      final Set<String> seeds,
      final String dir,
      final boolean autoBootstrap
  ) throws IOException {
    final String cassandraYamlTemplate =
        Resources.toString( Resources.getResource( YAML_RESOURCE_NAME ), StandardCharsets.UTF_8 );
    return Templates.prepare( YAML_RESOURCE_NAME )
        .withProperty( "cluster_name", name )
        .withProperty( "seeds", seeds )
        .withProperty( "bind_addr", bindAddr )
        .withProperty( "port_native", 8787 )
        .withProperty( "port_storage", 8782 )
        .withProperty( "port_storage_ssl", 8783 )
        .withProperty( "dir_lib", dir )
        .withProperty( "auto_bootstrap", autoBootstrap )
        .evaluate( cassandraYamlTemplate );
  }

  static String generateCassandraRackDcProperties(
      final String dc,
      final String rack
  ) throws IOException {
    final String cassandraRackDcTemplate =
        Resources.toString( Resources.getResource( RACKDC_RESOURCE_NAME ), StandardCharsets.UTF_8 );
    return Templates.prepare( RACKDC_RESOURCE_NAME )
        .withProperty( "dc", dc )
        .withProperty( "rack", rack )
        .evaluate( cassandraRackDcTemplate );
  }
}
