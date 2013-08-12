/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Comparable;
import java.lang.Object;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.SubDirectory;

@Entity
@PersistenceContext( name = "eucalyptus_config" )
@Table( name = "config_partition" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class Partition extends AbstractPersistent implements Comparable<Partition> {
  private static Logger LOG = Logger.getLogger( Partition.class );
  @Column( name = "config_partition_name", unique = true )
  String                name;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "config_partition_x509_certificate" )
  private String        pemCertificate;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "config_partition_node_x509_certificate" )
  private String        pemNodeCertificate;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "config_partition_kp" )
  private String        pemPrivateKey;
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  @Column( name = "config_partition_node_kp" )
  private String        pemNodePrivateKey;
  
  public static final String     DEFAULT_NAME = "default";
  public static final Partition  DEFAULT = new Partition(DEFAULT_NAME);
  
  public Partition( ) {}
  
  private Partition( String name ) {
    this.name = name;
  }
  
  public static Partition newInstanceNamed( String partitionName ) {
    return new Partition( partitionName );
  }
  
  public class Fake extends Partition {
    
    public Fake( String name, KeyPair keyPair, X509Certificate certificate ) {
      super( name, keyPair, certificate, keyPair, certificate );
    }
    
  }
  
  public Partition( String name, KeyPair keyPair, X509Certificate certificate, KeyPair nodeKeyPair, X509Certificate nodeCertificate ) {
    this.name = name;
    this.pemCertificate = PEMFiles.fromCertificate( certificate );
    this.pemNodeCertificate = PEMFiles.fromCertificate( nodeCertificate );
    this.pemPrivateKey = PEMFiles.fromKeyPair( keyPair );
    this.pemNodePrivateKey = PEMFiles.fromKeyPair( nodeKeyPair );
  }
  
  public X509Certificate getNodeCertificate( ) {
    return PEMFiles.toCertificate( this.getPemNodeCertificate( ) );
  }
  
  public X509Certificate getCertificate( ) {
    return PEMFiles.toCertificate( this.getPemCertificate( ) );
  }
  
  public PrivateKey getNodePrivateKey( ) {
    return PEMFiles.toKeyPair( this.getPemNodePrivateKey( ) ).getPrivate( );
  }
  
  public PrivateKey getPrivateKey( ) {
    return PEMFiles.toKeyPair( this.getPemPrivateKey( ) ).getPrivate( );
  }
    
  @PrePersist
  void prepareKeyDirectory( ) {
    File keyDir = SubDirectory.KEYS.getChildFile( this.name );
    LOG.info( "Creating key directory: " + keyDir.getAbsolutePath( ) );
    if ( !keyDir.exists( ) && !keyDir.mkdir( ) ) {
      throw new RuntimeException( "Failed to create partition key directory: " + keyDir.getAbsolutePath( ) );
    }
  }
  
  /**
   * This removes the key directory link for related components. This is temporary, do not plan on
   * using it.
   * 
   * @param config
   */
  @Deprecated
  public void link( ServiceConfiguration config ) {
    File keyLink = SubDirectory.KEYS.getChildFile( config.getName( ) );
    if ( !keyLink.exists( ) ) {
      LOG.debug( "Creating key directory link for " + config.getFullName( ) + " at " + keyLink.getAbsolutePath( ) );
      try {
        Groovyness.exec( "ln -sf " + SubDirectory.KEYS.getChildFile( this.name ).getAbsolutePath( ) + " " + keyLink.getAbsolutePath( ) );
        try {
          LOG.debug( "Created key directory link: " + keyLink.getAbsolutePath( ) + " -> " + keyLink.getCanonicalPath( ) );
        } catch ( IOException ex ) {}
      } catch ( ScriptExecutionFailedException ex ) {
        LOG.error( ex, ex );
      }
    } else {
      LOG.debug( "Skipped creating key directory link for " + config.getFullName( ) + " because it already exists at " + keyLink.getAbsolutePath( ) );
    }
  }
  
  /**
   * This removes the key directory link for related components. This is temporary, do not plan on
   * using it.
   * 
   * @param config
   */
  @Deprecated
  public void unlink( ServiceConfiguration config ) {
    LOG.info( "Removing key directory link for " + config );
    SubDirectory.KEYS.getChildFile( config.getName( ) ).delete( );
  }
  
  protected String getPemCertificate( ) {
    return this.pemCertificate;
  }
  
  protected void setPemCertificate( String clusterCertificate ) {
    this.pemCertificate = clusterCertificate;
  }
  
  protected String getPemNodeCertificate( ) {
    return this.pemNodeCertificate;
  }
  
  protected void setPemNodeCertificate( String nodeCertificate ) {
    this.pemNodeCertificate = nodeCertificate;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  protected void setName( String name ) {
    this.name = name;
  }
  
  protected String getPemPrivateKey( ) {
    return this.pemPrivateKey;
  }
  
  protected void setPemPrivateKey( String pemPrivateKey ) {
    this.pemPrivateKey = pemPrivateKey;
  }
  
  protected String getPemNodePrivateKey( ) {
    return this.pemNodePrivateKey;
  }
  
  protected void setPemNodePrivateKey( String pemNodePrivateKey ) {
    this.pemNodePrivateKey = pemNodePrivateKey;
  }
  
  @PostRemove
  private void removePartitionKeyFiles( ) {
    LOG.info( String.format( "Removing credentials for the partition=%s.", this.getName( ) ) );
    File keyDir = SubDirectory.KEYS.getChildFile( this.getName( ) );
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
  
  public void syncKeysToDisk( ) {
    this.writePartitionKeyFiles( );
  }
  
  @PostUpdate
  @PostPersist
  private void writePartitionKeyFiles( ) {
    File keyDir = SubDirectory.KEYS.getChildFile( this.getName( ) );
    if ( !keyDir.exists( ) && !keyDir.mkdir( ) ) {
      throw new RuntimeException( "Failed to create directory for partition credentials: " + this );
    }
    X509Certificate systemX509 = SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
    FileWriter out = null;
    try {
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-pk.pem", this.getPrivateKey( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cluster-cert.pem", this.getCertificate( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-pk.pem", this.getNodePrivateKey( ) );
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "node-cert.pem", this.getNodeCertificate( ) );
      
      PEMFiles.write( keyDir.getAbsolutePath( ) + File.separator + "cloud-cert.pem", systemX509 );
      out = new FileWriter( keyDir.getAbsolutePath( ) + File.separator + "vtunpass" );
      out.write( SystemIds.tunnelPassword( ) );
      out.flush( );
      out.close( );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new RuntimeException( "Failed to write partition credentials to disk: " + this, ex );
    } finally {
      if ( out != null ) {
        try {
          out.close( );
        } catch ( IOException e ) {
          LOG.error( e, e );
        }
      }
    }
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    if( this.name.equals(Partition.DEFAULT_NAME) ) {
	    builder.append( "Partition:name=" ).append( this.name ).append( ":cc-cert-serial=" ).append( ":nc-cert-serial=" );
    } else {
	    builder.append( "Partition:name=" ).append( this.name ).append( ":cc-cert-serial=" ).append( this.getCertificate( ).getSerialNumber( ) ).append( ":nc-cert-serial=" ).append( this.getNodeCertificate( ).getSerialNumber( ) );
    }
    return builder.toString( );
  }
  
  /**
   * @see Comparable#compareTo(Object)
   */
  @Override
  public int compareTo( Partition that ) {
    return this.name.compareTo( that.name );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = prime * ( ( this.name == null )
      ? 0
      : this.name.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    Partition other = ( Partition ) obj;
    if ( this.name == null ) {
      if ( other.name != null ) {
        return false;
      }
    } else if ( !this.name.equals( other.name ) ) {
      return false;
    }
    return true;
  }
  
}
