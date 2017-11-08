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
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSEC2InstanceResourceInfo extends ResourceInfo {

  @AttributeJson
  private String availabilityZone;
  @AttributeJson
  private String privateDnsName;
  @AttributeJson
  private String publicDnsName;
  @AttributeJson
  private String privateIp;
  @AttributeJson
  private String publicIp;
  @AttributeJson
  private String eucaCreateStartTime;

  public AWSEC2InstanceResourceInfo( ) {
    setType( "AWS::EC2::Instance" );
  }

  @Override
  public boolean supportsSignals( ) {
    return true;
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getPrivateDnsName( ) {
    return privateDnsName;
  }

  public void setPrivateDnsName( String privateDnsName ) {
    this.privateDnsName = privateDnsName;
  }

  public String getPublicDnsName( ) {
    return publicDnsName;
  }

  public void setPublicDnsName( String publicDnsName ) {
    this.publicDnsName = publicDnsName;
  }

  public String getPrivateIp( ) {
    return privateIp;
  }

  public void setPrivateIp( String privateIp ) {
    this.privateIp = privateIp;
  }

  public String getPublicIp( ) {
    return publicIp;
  }

  public void setPublicIp( String publicIp ) {
    this.publicIp = publicIp;
  }

  public String getEucaCreateStartTime( ) {
    return eucaCreateStartTime;
  }

  public void setEucaCreateStartTime( String eucaCreateStartTime ) {
    this.eucaCreateStartTime = eucaCreateStartTime;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "availabilityZone", availabilityZone )
        .add( "privateDnsName", privateDnsName )
        .add( "publicDnsName", publicDnsName )
        .add( "privateIp", privateIp )
        .add( "publicIp", publicIp )
        .add( "eucaCreateStartTime", eucaCreateStartTime )
        .toString( );
  }
}
