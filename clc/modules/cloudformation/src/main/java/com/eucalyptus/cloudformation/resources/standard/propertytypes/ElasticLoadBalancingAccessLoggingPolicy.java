/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class ElasticLoadBalancingAccessLoggingPolicy {

  @Property
  private Integer emitInterval;

  @Required
  @Property
  private Boolean enabled;

  @Property( name = "S3BucketName" )
  private String s3BucketName;

  @Property( name = "S3BucketPrefix" )
  private String s3BucketPrefix;

  public Integer getEmitInterval( ) {
    return emitInterval;
  }

  public void setEmitInterval( Integer emitInterval ) {
    this.emitInterval = emitInterval;
  }

  public Boolean getEnabled( ) {
    return enabled;
  }

  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }

  public String getS3BucketName( ) {
    return s3BucketName;
  }

  public void setS3BucketName( String s3BucketName ) {
    this.s3BucketName = s3BucketName;
  }

  public String getS3BucketPrefix( ) {
    return s3BucketPrefix;
  }

  public void setS3BucketPrefix( String s3BucketPrefix ) {
    this.s3BucketPrefix = s3BucketPrefix;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ElasticLoadBalancingAccessLoggingPolicy that = (ElasticLoadBalancingAccessLoggingPolicy) o;
    return Objects.equals( getEmitInterval( ), that.getEmitInterval( ) ) &&
        Objects.equals( getEnabled( ), that.getEnabled( ) ) &&
        Objects.equals( getS3BucketName( ), that.getS3BucketName( ) ) &&
        Objects.equals( getS3BucketPrefix( ), that.getS3BucketPrefix( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getEmitInterval( ), getEnabled( ), getS3BucketName( ), getS3BucketPrefix( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "emitInterval", emitInterval )
        .add( "enabled", enabled )
        .add( "s3BucketName", s3BucketName )
        .add( "s3BucketPrefix", s3BucketPrefix )
        .toString( );
  }
}
