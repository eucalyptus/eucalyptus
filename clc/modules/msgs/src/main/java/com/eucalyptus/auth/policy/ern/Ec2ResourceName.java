package com.eucalyptus.auth.policy.ern;

import com.eucalyptus.auth.policy.PolicySpec;

public class Ec2ResourceName extends Ern {
  
  private String type;
  private String id;
  
  public Ec2ResourceName( String type, String id ) {
    this.type = type;
    this.id = id;
    this.vendor = PolicySpec.VENDOR_EC2;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( ARN_PREFIX ).append( this.getVendor( ) ).append( ":::" ).append( this.type ).append( '/' ).append( this.id );
    return sb.toString( );
  }
  
  @Override
  public String getResourceType( ) {
    return this.vendor + ":" + this.type;
  }

  @Override
  public String getResourceName( ) {
    return this.id;
  }
  
}
