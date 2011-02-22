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
  
  public String getType( ) {
    return type;
  }

  public String getId( ) {
    return this.id;
  }

  @Override
  public String getResourceType( ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getResourceName( ) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
