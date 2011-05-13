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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.NavigableSet;
import org.apache.log4j.Logger;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;

public class Partitions {
  static Logger         LOG                 = Logger.getLogger( Partitions.class );
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
  
  public static boolean exists( final String partitionName ) {
    EntityWrapper<Partition> db = EntityWrapper.get( Partition.class );
    Partition p = null;
    try {
      p = db.getUnique( Partition.newInstanceNamed( partitionName ) );
      db.commit( );
      return true;
    } catch ( EucalyptusCloudException ex1 ) {
      db.rollback( );
      return false;
    }
  }
  
  public static Partition lookup( final ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( config.getComponentId( ).isPartitioned( ) ) {
      final String partitionName = config.getPartition( );
      EntityWrapper<Partition> db = EntityWrapper.get( Partition.class );
      Partition p = null;
      try {
        p = db.getUnique( Partition.newInstanceNamed( partitionName ) );
        db.commit( );
      } catch ( EucalyptusCloudException ex1 ) {
        db.rollback( );
        LOG.warn( "Failed to lookup partition for " + config + ".  Generating new partition configuration." );
        p = Partitions.generatePartition( config );
      }
      return p;
    } else {
      return Partition.fakePartition( config.getComponentId( ) );
    }
  }
  
  private static Partition generatePartition( ServiceConfiguration config ) throws ServiceRegistrationException {
    File keyDir = SubDirectory.KEYS.getChildFile( config.getPartition( ) );
    if ( !keyDir.exists( ) && !keyDir.mkdir( ) ) {
      throw new ServiceRegistrationException( "Failed to create partition key directory: " + keyDir.getAbsolutePath( ) );
    }
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
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new ServiceRegistrationException( "Failed to generate credentials for partition: " + config, ex );
    }
    Partition partition = new Partition( config.getPartition( ), clusterKp, clusterX509, nodeKp, nodeX509 );
    EntityWrapper<Partition> db = EntityWrapper.get( Partition.class );
    try {
      db.persist( partition );
      db.commit( );
      return partition;
    } catch ( Exception ex ) {
      db.rollback( );
      throw new ServiceRegistrationException( "Failed to store partition credentials during registration: " + config, ex );
    }
  }
  
  public static boolean testPartitionCredentialsDirectory( String name ) {
    File keyDir = SubDirectory.KEYS.getChildFile( name );
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
  
  @Deprecated
  public static ServiceConfiguration lookupService( Class<? extends ComponentId> compClass, String partition ) throws NoSuchServiceException {
    NavigableSet<ServiceConfiguration> services = Components.lookup( compClass ).enabledPartitionServices( partition );
    if ( services.isEmpty( ) ) {
      throw new NoSuchServiceException( "Failed to find service of type: " + compClass.getSimpleName( ) + " in partition: " + partition );
    } else {
      return services.first( );
    }
  }
}
