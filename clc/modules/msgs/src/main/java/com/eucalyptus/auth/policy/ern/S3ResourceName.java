package com.eucalyptus.auth.policy.ern;

import com.eucalyptus.auth.policy.PolicySpec;

public class S3ResourceName extends Ern {

  private String bucket;
  private String object;
  
  public S3ResourceName( String bucket, String object ) {
    this.bucket = bucket;
    this.object = object;
    this.vendor = PolicySpec.VENDOR_S3;
  }
  
  public boolean isBucket( ) {
    if ( this.object == null || "".equals( this.object ) ) {
      return true;
    }
    return false;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( ARN_PREFIX ).append( this.getVendor( ) ).append( ":::" ).append( this.bucket );
    if ( this.object != null ) {
      sb.append( this.object );
    }
    return sb.toString( );
  }
  
  public String getBucket( ) {
    return this.bucket;
  }
  
  public String getObject( ) {
    return this.object;
  }

  @Override
  public String getResourceType( ) {
    if ( this.isBucket( ) ) {
      return this.vendor + ":" + PolicySpec.S3_RESOURCE_BUCKET;
    } else {
      return this.vendor + ":" + PolicySpec.S3_RESOURCE_OBJECT;
    }
  }

  @Override
  public String getResourceName( ) {
    String resourceName = this.bucket;
    if ( this.object != null ) {
      resourceName += this.object;
    }
    return resourceName;
  }
  
}
