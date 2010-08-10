package com.eucalyptus.auth.principal;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_authorization" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(
   name="auth_authorization_class",
   discriminatorType=DiscriminatorType.STRING
)
public class BaseAuthorization<T> extends AbstractPersistent implements Authorization<T>, Serializable{
  
  @Column( name = "auth_authorization_value" )
  private String value;
  
  public BaseAuthorization( ) {
    super( );
  }
  
  public BaseAuthorization( String value ) {
    this.value = value;
  }
  
  public String getValue( ) {
    return this.value;
  }
  
  public void setValue( String value ) {
    this.value = value;
  }

  @Override
  public String getDescription( ) {
    return "Abstract root of all authorization types";
  }
  
  @Override
  public String getDisplayName( ) {
    return "Abstract Authorization";
  }
  @Deprecated
  public String getName( ) {
    return this.getDisplayName( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.value == null ) ? 0 : this.value.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    BaseAuthorization other = ( BaseAuthorization ) obj;
    if ( this.value == null ) {
      if ( other.value != null ) return false;
    } else if ( !this.value.equals( other.value ) ) return false;
    return true;
  }

  @Override
  public boolean check( T t ) {
    return false;
  }
  
}
