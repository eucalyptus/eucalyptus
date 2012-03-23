package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Hmacs;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database secret key entity.
 * 
 * @author wenye
 *
 */
/**
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_access_key" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AccessKeyEntity extends AbstractPersistent implements Serializable {
  
  @Transient
  private static final long serialVersionUID = 1L;
  
  // If the key is active
  @Column( name = "auth_access_key_active" )
  Boolean active;
  
  // The Access Key ID
  @Column( name = "auth_access_key_query_id" )
  String accessKey;
  // The SECRET key
  @Column( name = "auth_access_key_key" )
  String key;
  
  // The create date
  @Column( name = "auth_access_key_create_date" )
  Date createDate;
  
  // The owning user
  @ManyToOne
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
      this.accessKey = Crypto.generateQueryId();
    }
  }
  
  /**
   * NOTE: should not be needed, replaced by {@link #newInstanceWithAccessKeyId()}
   */
//  public static AccessKeyEntity newInstanceWithId( final String id ) {
//    AccessKeyEntity k = new AccessKeyEntity( );
//    k.setId( id );
//    return k;
//  }

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
  
}
