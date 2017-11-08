/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.auth.euare.persist.entities;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.Callable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;

/**
 * Database secret key entity.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_access_key", indexes = {
    @Index( name = "auth_access_key_owning_user_idx", columnList = "auth_access_key_owning_user" )
} )
public class AccessKeyEntity extends AbstractPersistent implements Serializable {
  
  @Transient
  private static final long serialVersionUID = 1L;
  
  // If the key is active
  @Column( name = "auth_access_key_active" )
  Boolean active;
  
  // The Access Key ID
  @Column( name = "auth_access_key_query_id", unique = true  )
  String accessKey;
  // The SECRET key
  @Column( name = "auth_access_key_key" )
  String key;
  
  // The create date
  @Column( name = "auth_access_key_create_date" )
  Date createDate;
  
  // The owning user
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "auth_access_key_owning_user" )
  UserEntity user;
  
  public AccessKeyEntity( ) {
  }
  
  public AccessKeyEntity( UserEntity user ) {
    this.user = user;
    this.key = Crypto.generateSecretKey();
    this.createDate = new Date( );
  }

  @PrePersist
  public void generateOnCommit() {
    if( this.accessKey == null && this.key != null ) {/** NOTE: first time that AKey is committed it needs to generate its own ID (i.e., not the database id), do this at commit time and generate if null **/
      this.accessKey = Identifiers.generateAccessKeyIdentifier( );
    }
  }
  
  public static AccessKeyEntity newInstanceWithAccessKeyId( final String accessKeyId ) {
    AccessKeyEntity k = new AccessKeyEntity( );
    k.accessKey = accessKeyId;
    return k;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    AccessKeyEntity that = ( AccessKeyEntity ) o;    
    if ( !this.getAccessKey( ).equals( that.getAccessKey( ) ) ) return false;//NOTE: prefer for equality check to not rely on sensitive data -- e.g., secret key.
    if ( !this.getSecretKey( ).equals( that.getSecretKey( ) ) ) return false;
    
    return true;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Key(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "active=" ).append( this.isActive( ) ).append( ", " );
    sb.append( "key=" ).append( this.getSecretKey( ) );
    sb.append( ")" );
    return sb.toString( );
  }

  public String getAccessKey( ) {
    return this.accessKey;
  }
  
  public void setAccess( String accessKey ) {
    this.accessKey = accessKey;
  }

  public String getSecretKey( ) {
    return this.key;
  }
  
  public void setSecretKey( String key ) {
    this.key = key;
  }
  
  public Boolean isActive( ) {
    return this.active;
  }
  
  public void setActive( Boolean active ) {
    this.active = active;
  }
  
  public Date getCreateDate( ) {
    return this.createDate;
  }
  
  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }
  
  public UserEntity getUser( ) {
    return this.user;
  }
  
  public void setUser( UserEntity user ) {
    this.user = user;
  }

  @Upgrades.PreUpgrade( value = Euare.class, since = v4_2_0 )
  public static class AccessKeyPreUpgrade420 implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( AccessKeyPreUpgrade420.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_auth");
        sql.execute( "alter table auth_access_key add constraint uk_8n6ryppss5fpcb09w867acb1w unique ( auth_access_key_query_id )" );
        return true;
      } catch (Exception ex) {
        logger.error( ex, ex );
        return false;
      } finally {
        if (sql != null) {
          sql.close();
        }
      }
    }
  }
}
