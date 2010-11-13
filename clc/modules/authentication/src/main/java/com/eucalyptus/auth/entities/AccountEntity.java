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
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database account entity.
 * 
 * @author wenye
 *
 */

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_account" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AccountEntity extends AbstractPersistent implements Account, Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( AccountEntity.class );
  
  // Account name, it is unique.
  @Column( name = "auth_account_name", unique = true )
  String name;
  
  public AccountEntity( ) {
  }
  
  public AccountEntity( String name ) {
    this( );
    this.name = name;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    AccountEntity that = ( AccountEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Account(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }

  @Override
  public String getAccountId( ) {
    return this.getId( );
  }

}
