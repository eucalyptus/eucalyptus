/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
