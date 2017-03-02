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
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Templates;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import edu.ucsb.eucalyptus.util.SystemUtil;

/**
 *
 */
class Util {
  private static final Logger logger = Logger.getLogger( Util.class );

  private static final String PATH_CASSANDRA = "/usr/sbin/cassandra";
  private static final String PATH_ROOTWRAP = "/usr/lib/eucalyptus/euca_rootwrap";
  private static final String PATH_EUCACASS = "/usr/libexec/eucalyptus/euca-cassandra";
  private static final String PATH_CASSPID = "/var/run/eucalyptus/cassandra.pid";
  private static final String PATH_CASSLOCK = "/var/run/eucalyptus/cassandra.lock";
  private static final String YAML_RESOURCE_NAME = "cassandra.yaml";

  static boolean checkCassandra( ) {
    if ( !new File( PATH_CASSANDRA ).isFile( ) ) {
      return false;
    }
    final int code = SystemUtil.runAndGetCode( new String[]{ PATH_ROOTWRAP, PATH_EUCACASS, "status" } );
    switch ( code ) {
      case 0:
        logger.trace( "Cassandra running" );
        break;
      case 1:
        logger.warn( "Cassandra dead but pid file exists" );
        cleanup( );
        return false;
      case 2:
        logger.warn( "Cassandra dead but lock file exists" );
        cleanup( );
        return false;
      case 3:
        logger.trace( "Cassandra stopped" );
        break;
      default:
        logger.warn( "Cassandra status unknown, code: " + code );
        return false;
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

  static void createDirectories( ) {
    BaseDirectory.VAR.getChildFile( "cassandra", "data" ).mkdirs( );
    BaseDirectory.VAR.getChildFile( "cassandra", "commitlog" ).mkdirs( );
    BaseDirectory.VAR.getChildFile( "cassandra", "saved_caches" ).mkdirs( );
    BaseDirectory.VAR.getChildFile( "cassandra", "conf", "triggers" ).mkdirs( );
  }

  private static void cleanup( ) {
    final File pid = new File( PATH_CASSPID );
    if ( pid.exists( ) ) {
      logger.info( "Deleting pid file: " + pid );
      if ( !pid.delete( ) ) {
        logger.warn( "Error removing pid file: " + pid );
      }
    }

    final File lock = new File( PATH_CASSLOCK );
    if ( lock.exists( ) ) {
      logger.info( "Deleting lock file: " + lock );
      if ( !lock.delete( ) ) {
        logger.warn( "Error removing lock file: " + lock );
      }
    }
  }

  static void writeConfiguration( ) throws IOException {
    Files.write(
        generateCassandraYaml( ),
        BaseDirectory.VAR.getChildFile( "cassandra", "conf", "cassandra.yaml" ),
        StandardCharsets.UTF_8 );
  }

  private static String generateCassandraYaml( ) throws IOException {
    final String name = SystemIds.createShortCloudUniqueName( "cassandra" );
    final String bindAddr = Internets.localHostInetAddress( ).getHostAddress( );
    final String dir = BaseDirectory.VAR.toString( );
    return generateCassandraYaml( name, bindAddr, dir );
  }

  static String generateCassandraYaml(
      final String name,
      final String bindAddr,
      final String dir
  ) throws IOException {
    final String cassandraYamlTemplate =
        Resources.toString( Resources.getResource( YAML_RESOURCE_NAME ), StandardCharsets.UTF_8 );
    return Templates.prepare( YAML_RESOURCE_NAME )
        .withProperty( "cluster_name", name )
        .withProperty( "seeds", bindAddr )
        .withProperty( "bind_addr", bindAddr )
        .withProperty( "port_native", 8787 )
        .withProperty( "port_storage", 8782 )
        .withProperty( "port_storage_ssl", 8783 )
        .withProperty( "dir_lib", dir )
        .evaluate( cassandraYamlTemplate );
  }
}
