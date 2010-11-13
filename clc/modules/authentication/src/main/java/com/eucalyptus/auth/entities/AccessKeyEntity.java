package com.eucalyptus.auth.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
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

  @Transient
  private static Logger LOG = Logger.getLogger( AccessKeyEntity.class );
  
  // If the key is active
  @Column( name = "auth_access_key_active" )
  Boolean active;
  
  // The key
  @Column( name = "auth_access_key_key" )
  String key;
  
  public AccessKeyEntity( ) {
  }
  
  public AccessKeyEntity( String key ) {
    this.key = key;
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
  
  public Boolean isActive( ) {
    return this.active;
  }
  
  public void setActive( Boolean active ) {
    this.active = active;
  }
  
}
