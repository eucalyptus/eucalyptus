/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
