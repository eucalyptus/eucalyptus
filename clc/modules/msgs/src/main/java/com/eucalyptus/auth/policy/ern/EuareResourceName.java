package com.eucalyptus.auth.policy.ern;

import com.eucalyptus.auth.policy.PolicySpec;

public class EuareResourceName extends Ern {

  private String userOrGroup;
  private String path;
  private String name;
  
  public EuareResourceName( String namespace, String userOrGroup, String path, String name ) {
    this.namespace = namespace;
    this.userOrGroup = userOrGroup;
    this.path = path;
    this.name = name;
    this.vendor = PolicySpec.VENDOR_IAM;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( ARN_PREFIX ).append( this.getVendor( ) ).append( "::" ).append( this.getNamespace( ) )
        .append( ':' ).append( this.userOrGroup ).append( this.path );
    if ( !"/".equals( this.path ) ) {
      sb.append( '/' );
    }
    sb.append( this.name );
    return sb.toString( );
  }
  
  public void setUserOrGroup( String userOrGroup ) {
    this.userOrGroup = userOrGroup;
  }
  public String getUserOrGroup( ) {
    return userOrGroup;
  }
  public void setPath( String path ) {
    this.path = path;
  }
  public String getPath( ) {
    return path;
  }
  public void setName( String name ) {
    this.name = name;
  }
  public String getName( ) {
    return name;
  }

  @Override
  public String getResourceType( ) {
    return this.vendor + ":" + this.userOrGroup;
  }

  @Override
  public String getResourceName( ) {
    if ( "/".equals( this.path ) ) {
      return this.path + this.name;
    } else {
      return this.path + "/" + this.name;
    }
  }
  
}
