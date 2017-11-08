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

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class AWSEC2NetworkAclEntryProperties implements ResourceProperties {

  @Required
  @Property
  private String cidrBlock;

  @Property
  private Boolean egress;

  @Property
  private EC2ICMP icmp;

  @Required
  @Property
  private String networkAclId;

  @Property
  private EC2PortRange portRange;

  @Required
  @Property
  private Integer protocol;

  @Required
  @Property
  private String ruleAction;

  @Required
  @Property
  private Integer ruleNumber;

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public Boolean getEgress( ) {
    return egress;
  }

  public void setEgress( Boolean egress ) {
    this.egress = egress;
  }

  public EC2ICMP getIcmp( ) {
    return icmp;
  }

  public void setIcmp( EC2ICMP icmp ) {
    this.icmp = icmp;
  }

  public String getNetworkAclId( ) {
    return networkAclId;
  }

  public void setNetworkAclId( String networkAclId ) {
    this.networkAclId = networkAclId;
  }

  public EC2PortRange getPortRange( ) {
    return portRange;
  }

  public void setPortRange( EC2PortRange portRange ) {
    this.portRange = portRange;
  }

  public Integer getProtocol( ) {
    return protocol;
  }

  public void setProtocol( Integer protocol ) {
    this.protocol = protocol;
  }

  public String getRuleAction( ) {
    return ruleAction;
  }

  public void setRuleAction( String ruleAction ) {
    this.ruleAction = ruleAction;
  }

  public Integer getRuleNumber( ) {
    return ruleNumber;
  }

  public void setRuleNumber( Integer ruleNumber ) {
    this.ruleNumber = ruleNumber;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "cidrBlock", cidrBlock )
        .add( "egress", egress )
        .add( "icmp", icmp )
        .add( "networkAclId", networkAclId )
        .add( "portRange", portRange )
        .add( "protocol", protocol )
        .add( "ruleAction", ruleAction )
        .add( "ruleNumber", ruleNumber )
        .toString( );
  }
}
