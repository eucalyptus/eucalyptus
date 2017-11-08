/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.component;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Databases.DatabaseStateException;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
    EntityTransaction db = Entities.get( Partition.class );
    Partition p = null;
    try {
      p = Entities.uniqueResult( Partition.newInstanceNamed( partitionName ) );
      db.commit( );
      return true;
    } catch ( Exception ex ) {
      db.rollback( );
      return false;
    }
  }
  
  private static final LoadingCache<String, Partition> partitionMap = CacheBuilder.newBuilder().build(
    new CacheLoader<String, Partition>() {
      @Override
      public Partition load( final String input ) {
        try {
          Databases.awaitSynchronized( );
          EntityTransaction db = Entities.get( Partition.class );
          Partition p = null;
          try {
            p = Entities.uniqueResult( Partition.newInstanceNamed( input ) );
            db.commit( );
            return p;
          } catch ( NoSuchElementException ex ) {
            db.rollback( );
            throw ex;
          } catch ( Exception ex ) {
            db.rollback( );
            throw Exceptions.toUndeclared( ex );
          }
        } catch ( NoSuchElementException ex ) {
          throw ex;
        } catch ( DatabaseStateException ex ) {
          Databases.awaitSynchronized( );
          return load( input );
        } catch ( RuntimeException ex ) {
          throw ex;
        }
      }
    });
  

  public static Partition lookupByName( String partitionName ) {
    return partitionMap.getUnchecked( partitionName );
  }
  
  public static Partition lookup( final ServiceConfiguration config ) {
    try {
      if ( config.getComponentId( ).isPartitioned( ) && config.getComponentId( ).isRegisterable( ) ) {
        Partition p;
        try {
          p = Partitions.lookupByName( config.getPartition( ) );
        } catch ( Exception ex ) {
          if ( Exceptions.isCausedBy( ex, NoSuchElementException.class ) ) {
            LOG.warn( "Failed to lookup partition for " + config
                      + ".  Generating new partition configuration.\nCaused by: " + Exceptions.causeString( ex ) );
            try {
              p = Partitions.generatePartition( config );
            } catch ( ServiceRegistrationException ex1 ) {
              LOG.error( ex1, ex1 );
              throw Exceptions.toUndeclared( ex1 );
            }
          } else {
            throw ex;
          }
        }
        return p;
      } else if ( config.getComponentId( ).isPartitioned( ) ) {
        return Partitions.lookupInternal( config );
      } else {
        return Partitions.lookupInternal( config );
      }
    } catch ( DatabaseStateException ex ) {
      throw ex;
    } catch ( Exception ex ) {
      LOG.trace( ex );
      return Partitions.lookupInternal( config );
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
    EntityTransaction db = Entities.get( Partition.class );
    try {
      Entities.persist( partition );
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
  
  public static Partition lookupInternal( final ServiceConfiguration config ) {
    ComponentId compId = config.getComponentId( );
    if ( compId.isRegisterable( ) ) {
      throw new IllegalArgumentException( "Provided compId is partitioned: " + compId.getFullName( ) );
    } else {
      if ( compId.isAlwaysLocal( ) ) {
        return new Partition( ).new Fake( config.getHostName( ), SystemCredentials.lookup( Empyrean.class ).getKeyPair( ),
                                          SystemCredentials.lookup( Empyrean.class ).getCertificate( ) );
      } else if ( compId.isCloudLocal( ) ) {
        return new Partition( ).new Fake( config.getHostName( ), SystemCredentials.lookup( Eucalyptus.class ).getKeyPair( ),
                                          SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ) );
      } else {
        if ( !compId.hasCredentials( ) ) {
          return new Partition( ).new Fake( compId.getPartition( ), SystemCredentials.lookup( Eucalyptus.class ).getKeyPair( ),
                                            SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ) );
        } else {
          return new Partition( ).new Fake( compId.getPartition( ), SystemCredentials.lookup( compId ).getKeyPair( ),
                                            SystemCredentials.lookup( compId ).getCertificate( ) );
        }
      }
    }
  }
  
  /**
   * @return
   * @return
   */
  public static List<Partition> list( ) {
    EntityTransaction db = Entities.get( Partition.class );
    try {
      List<Partition> entities = Entities.query( new Partition( ) );
      db.commit( );
      return entities;
    } catch ( RuntimeException ex ) {
      db.rollback( );
      throw ex;
    }
  }
  
}
