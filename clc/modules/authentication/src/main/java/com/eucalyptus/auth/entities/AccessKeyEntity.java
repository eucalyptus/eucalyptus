package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database secret key entity.
 * 
 * @author wenye
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_access_key" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AccessKeyEntity extends AbstractPersistent implements Serializable {
  
  @Transient
  private static final long serialVersionUID = 1L;
  
  // If the key is active
  @Column( name = "auth_access_key_active" )
  Boolean active;
  
  // The key
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
  
  public AccessKeyEntity( String key ) {
    this.key = key;
    this.createDate = new Date( );
  }

  public static AccessKeyEntity newInstanceWithId( final String id ) {
    AccessKeyEntity k = new AccessKeyEntity( );
    k.setId( id );
    return k;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    AccessKeyEntity that = ( AccessKeyEntity ) o;    
    if ( !this.getKey( ).equals( that.getKey( ) ) ) return false;
    
    return true;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Key(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "active=" ).append( this.isActive( ) ).append( ", " );
    sb.append( "key=" ).append( this.getKey( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getKey( ) {
    return this.key;
  }
  
  public void setKey( String key ) {
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
