package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table( name = "image_product_code" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ProductCode {
  @Id
  @GeneratedValue
  @Column( name = "image_product_code_id" )
  private Long id = -1l;
  @Column( name = "image_product_code_value" )
  private String value;

  public ProductCode() { }

  public ProductCode( final String value ) {
    this.value = value;
  }

  public Long getId() {
    return id;
  }

  public String getValue() {
    return value;
  }

  public void setValue( final String value ) {
    this.value = value;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    ProductCode that = ( ProductCode ) o;

    if ( value != null ? !value.equals( that.value ) : that.value != null ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + ( value != null ? value.hashCode() : 0 );
    return result;
  }
}
