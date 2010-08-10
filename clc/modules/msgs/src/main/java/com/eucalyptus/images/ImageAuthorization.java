package com.eucalyptus.images;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@PersistenceContext( name = "eucalyptus_general" )
@Table( name = "image_authorization" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ImageAuthorization {
  @Id
  @GeneratedValue
  @Column( name = "image_auth_id" )
  private Long   id = -1l;
  @Column( name = "image_auth_name" )
  private String value;
  
  public ImageAuthorization( ) {}
  
  public ImageAuthorization( String value ) {
    this.value = value;
  }
  
  public Long getId( ) {
    return this.id;
  }
  
  public void setId( Long id ) {
    this.id = id;
  }
  
  public String getValue( ) {
    return this.value;
  }
  
  public void setValue( String value ) {
    this.value = value;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.value == null ) ? 0 : this.value.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    ImageAuthorization other = ( ImageAuthorization ) obj;
    if ( this.value == null ) {
      if ( other.value != null ) return false;
    } else if ( !this.value.equals( other.value ) ) return false;
    return true;
  }
  
  
  
}
