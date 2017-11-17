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

public class AWSEC2VPCProperties implements ResourceProperties {

  @Required
  @Property
  private String cidrBlock;

  @Property
  private Boolean enableDnsSupport;

  @Property
  private Boolean enableDnsHostnames;

  @Property
  private String instanceTenancy;

  @Property
  private ArrayList<EC2Tag> tags = Lists.newArrayList( );

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public Boolean getEnableDnsHostnames( ) {
    return enableDnsHostnames;
  }

  public void setEnableDnsHostnames( Boolean enableDnsHostnames ) {
    this.enableDnsHostnames = enableDnsHostnames;
  }

  public Boolean getEnableDnsSupport( ) {
    return enableDnsSupport;
  }

  public void setEnableDnsSupport( Boolean enableDnsSupport ) {
    this.enableDnsSupport = enableDnsSupport;
  }

  public String getInstanceTenancy( ) {
    return instanceTenancy;
  }

  public void setInstanceTenancy( String instanceTenancy ) {
    this.instanceTenancy = instanceTenancy;
  }

  public ArrayList<EC2Tag> getTags( ) {
    return tags;
  }

  public void setTags( ArrayList<EC2Tag> tags ) {
    this.tags = tags;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrBlock", cidrBlock )
        .add( "enableDnsSupport", enableDnsSupport )
        .add( "enableDnsHostnames", enableDnsHostnames )
        .add( "instanceTenancy", instanceTenancy )
        .add( "tags", tags )
        .toString( );
  }
}
