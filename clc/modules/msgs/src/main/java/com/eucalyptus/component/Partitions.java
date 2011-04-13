/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.component.auth.SystemCredentialProvider;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;

public class Partitions {
  private static Logger LOG = Logger.getLogger( Partitions.class );
  private static String CLUSTER_KEY_FSTRING = "cc-%s";
  private static String NODE_KEY_FSTRING    = "nc-%s";

  
  
  public static void maybeRemove( final String partitionName ) {
    LOG.error( "Ignoring attempt for partition at the moment" );
//    try {
//      String otherClusters = Iterables.transform( Partitions.lookupPartition( partitionName ), HasFullName.GET_FULLNAME ).toString( );
//      LOG.info( String.format( "There still exist clusters within the partition=%s so the keys will not be removed.", partitionName, otherClusters ) );
//    } catch ( NoSuchElementException ex1 ) {
//      Partitions.remove( partitionName );
//    }
  }

  private static void remove( final String partitionName ) {
  LOG.info( String.format( "Removing credentials for the partition=%s.", partitionName ) );
  String directory = SubDirectory.KEYS.toString( ) + File.separator + partitionName;
  File keyDir = new File( directory );
  if ( keyDir.exists( ) ) {
    for ( File f : keyDir.listFiles( ) ) {
      if ( f.delete( ) ) {
        LOG.info( "Removing cluster key file: " + f.getAbsolutePath( ) );
      } else {
        LOG.info( "Failed to remove cluster key file: " + f.getAbsolutePath( ) );
      }
    }
    if ( keyDir.delete( ) ) {
      LOG.info( "Removing cluster key directory: " + keyDir.getAbsolutePath( ) );
    } else {
      LOG.info( "Failed to remove cluster key directory: " + keyDir.getAbsolutePath( ) );
    }
  }
  }
  
  public static Partition lookup( final ServiceConfiguration config ) throws ServiceRegistrationException {
    final String partitionName = config.getPartition( );
    EntityWrapper<Partition> db = EntityWrapper.get( Partition.class );
    Partition p = null;
    try {
      p = db.getUnique( new Partition( ) {
          {
            setName( partitionName );
          }
        } );
      db.commit( );
    } catch ( EucalyptusCloudException ex1 ) {
      db.rollback( );
      LOG.warn( "Failed to lookup partition for " + config + ".  Generating new partition configuration." );
      p = Partitions.generatePartition( config );
    }
    return p;
  }

  private static Partition generatePartition( ServiceConfiguration config ) throws ServiceRegistrationException {
    File keyDir = Partitions.makeKeyDir( config );
    X509Certificate clusterX509;
    X509Certificate nodeX509;    
    /** generate the cluster/node keys **/
    KeyPair clusterKp;
    KeyPair nodeKp;
    try {
      clusterKp = Certs.generateKeyPair( );
      clusterX509 = Certs.generateServiceCertificate( clusterKp, String.format( CLUSTER_KEY_FSTRING, config.getName( ) ) );
      nodeKp = Certs.generateKeyPair( );
      nodeX509 = Certs.generateServiceCertificate( nodeKp, String.format( NODE_KEY_FSTRING, config.getName( ) ) );
      Partition partition = new Partition( config.getPartition( ), clusterX509, nodeX509 );
      EntityWrapper<Partition> db = EntityWrapper.get( Partition.class );
      try {
        Partitions.writePartitionKeyFiles( partition, keyDir, clusterKp, clusterX509, nodeKp, nodeX509 );
        db.persist( partition );
        db.commit( );
        return partition;
      } catch ( Throwable ex ) {
        db.rollback( );
        Partitions.remove( partition.getName( ) );
        throw new ServiceRegistrationException( "Failed to store partition credentials during registration: " + config, ex );
      }
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( "Failed to generate credentials for partition: " + config, ex );
    }
  }

  private static File makeKeyDir( ServiceConfiguration config ) throws ServiceRegistrationException {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + config.getName( );
    File keyDir = new File( directory );
    LOG.info( "creating keys in " + directory );
    if ( !keyDir.exists( ) && !keyDir.mkdir( ) ) {
      throw new ServiceRegistrationException( "Failed to create cluster key directory: " + keyDir.getAbsolutePath( ) );
    }
    return keyDir;
  }

  private static void writePartitionKeyFiles( Partition partition, File keyDir, KeyPair clusterKp, X509Certificate clusterX509, KeyPair nodeKp, X509Certificate nodeX509 ) throws ServiceRegistrationException {
    X509Certificate systemX509 = SystemCredentialProvider.getCredentialProvider( Eucalyptus.class ).getCertificate( );
    FileWriter out = null;
    try {
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-pk.pem", clusterKp.getPrivate( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-cert.pem", clusterX509 );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-pk.pem", nodeKp.getPrivate( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-cert.pem", nodeX509 );
      
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cloud-cert.pem", systemX509 );
      out = new FileWriter( keyDir.getAbsolutePath( ) + File.separator + "vtunpass" );
      out.write( SystemIds.tunnelPassword( ) );
      out.flush( );
      out.close( );
    } catch ( Throwable ex ) {
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( "Failed to write partition credentials to disk: " + partition, ex );
    } finally {
      if ( out != null ) try {
        out.close( );
        } catch ( IOException e ) {
        LOG.error( e, e );
        }
    }
  }


  public static boolean testPartitionCredentialsDirectory( String name ) {
    String directory = SubDirectory.KEYS.toString( ) + File.separator + name;
    File keyDir = new File( directory );
    if ( !keyDir.exists( ) ) {
      try {
        return keyDir.mkdir( ) && keyDir.canWrite( );
      } catch ( Exception e ) {
        return false;
      }
    } else {
      return keyDir.canWrite( );
    }
  }

}
