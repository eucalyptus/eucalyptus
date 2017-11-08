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

import java.util.ArrayList;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class AWSEC2SecurityGroupProperties implements ResourceProperties {

  @Property
  @Required
  private String groupDescription;

  @Property
  private ArrayList<EC2SecurityGroupRule> securityGroupEgress = Lists.newArrayList( );

  @Property
  private ArrayList<EC2SecurityGroupRule> securityGroupIngress = Lists.newArrayList( );

  @Property
  private String vpcId;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  public String getGroupDescription( ) {
    return groupDescription;
  }

  public void setGroupDescription( String groupDescription ) {
    this.groupDescription = groupDescription;
  }

  public ArrayList<EC2SecurityGroupRule> getSecurityGroupEgress( ) {
    return securityGroupEgress;
  }

  public void setSecurityGroupEgress( ArrayList<EC2SecurityGroupRule> securityGroupEgress ) {
    this.securityGroupEgress = securityGroupEgress;
  }

  public ArrayList<EC2SecurityGroupRule> getSecurityGroupIngress( ) {
    return securityGroupIngress;
  }

  public void setSecurityGroupIngress( ArrayList<EC2SecurityGroupRule> securityGroupIngress ) {
    this.securityGroupIngress = securityGroupIngress;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  public String getVpcId( ) {
    return vpcId;
  }

  public void setVpcId( String vpcId ) {
    this.vpcId = vpcId;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "groupDescription", groupDescription )
        .add( "securityGroupEgress", securityGroupEgress )
        .add( "securityGroupIngress", securityGroupIngress )
        .add( "vpcId", vpcId )
        .add( "tags", tags )
        .toString( );
  }
}
