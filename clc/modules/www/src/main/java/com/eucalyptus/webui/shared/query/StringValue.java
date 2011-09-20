package com.eucalyptus.webui.shared.query;

public class StringValue implements QueryValue {
  
  private String value;
  
  public StringValue( String v ) {
    this.value = v;
  }
  
  @Override
  public String getValue( ) {
    return this.value;
  }
  
  @Override
  public String toString( ) {
    return this.value;
  }
  
  @Override
  public boolean equals( Object other ) {
    if ( other == null ) {
      return false;
    }
    if ( !(other instanceof StringValue) ) {
      return false;
    }
    StringValue otherValue = ( StringValue )other;
    if ( this.value == null && otherValue.getValue( ) == null ) {
      return true;
    }
    if ( this.value != null && this.value.equals( otherValue.getValue( ) ) ) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode( ) {
    return this.value.hashCode( );
  }
}
