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
  
}
