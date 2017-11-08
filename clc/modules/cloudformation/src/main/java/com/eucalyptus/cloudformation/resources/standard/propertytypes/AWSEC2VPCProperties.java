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
