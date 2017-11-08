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

public class AWSEC2VPCResourceInfo extends ResourceInfo {

  @AttributeJson
  private String cidrBlock;
  @AttributeJson
  private String defaultNetworkAcl;
  @AttributeJson
  private String defaultSecurityGroup;

  public AWSEC2VPCResourceInfo( ) {
    setType( "AWS::EC2::VPC" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public String getDefaultNetworkAcl( ) {
    return defaultNetworkAcl;
  }

  public void setDefaultNetworkAcl( String defaultNetworkAcl ) {
    this.defaultNetworkAcl = defaultNetworkAcl;
  }

  public String getDefaultSecurityGroup( ) {
    return defaultSecurityGroup;
  }

  public void setDefaultSecurityGroup( String defaultSecurityGroup ) {
    this.defaultSecurityGroup = defaultSecurityGroup;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrBlock", cidrBlock )
        .add( "defaultNetworkAcl", defaultNetworkAcl )
        .add( "defaultSecurityGroup", defaultSecurityGroup )
        .toString( );
  }
}
