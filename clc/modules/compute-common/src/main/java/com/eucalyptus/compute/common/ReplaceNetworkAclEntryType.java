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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;

public class ReplaceNetworkAclEntryType extends VpcMessage {

  private String networkAclId;
  @ComputeMessageValidation.FieldRange( min = 1l, max = 32766l )
  private Integer ruleNumber;
  @ComputeMessageValidation.FieldRegex( ComputeMessageValidation.FieldRegexValue.EC2_PROTOCOL )
  private String protocol;
  @ComputeMessageValidation.FieldRegex( ComputeMessageValidation.FieldRegexValue.EC2_ACL_ACTION )
  private String ruleAction;
  private Boolean egress = false;
  @ComputeMessageValidation.FieldRegex( ComputeMessageValidation.FieldRegexValue.CIDR )
  private String cidrBlock;
  @HttpParameterMapping( parameter = "Icmp" )
  private IcmpTypeCodeType icmpTypeCode;
  private PortRangeType portRange;

  public String getNetworkAclId( ) {
    return networkAclId;
  }

  public void setNetworkAclId( String networkAclId ) {
    this.networkAclId = networkAclId;
  }

  public Integer getRuleNumber( ) {
    return ruleNumber;
  }

  public void setRuleNumber( Integer ruleNumber ) {
    this.ruleNumber = ruleNumber;
  }

  public String getProtocol( ) {
    return protocol;
  }

  public void setProtocol( String protocol ) {
    this.protocol = protocol;
  }

  public String getRuleAction( ) {
    return ruleAction;
  }

  public void setRuleAction( String ruleAction ) {
    this.ruleAction = ruleAction;
  }

  public Boolean getEgress( ) {
    return egress;
  }

  public void setEgress( Boolean egress ) {
    this.egress = egress;
  }

  public String getCidrBlock( ) {
    return cidrBlock;
  }

  public void setCidrBlock( String cidrBlock ) {
    this.cidrBlock = cidrBlock;
  }

  public IcmpTypeCodeType getIcmpTypeCode( ) {
    return icmpTypeCode;
  }

  public void setIcmpTypeCode( IcmpTypeCodeType icmpTypeCode ) {
    this.icmpTypeCode = icmpTypeCode;
  }

  public PortRangeType getPortRange( ) {
    return portRange;
  }

  public void setPortRange( PortRangeType portRange ) {
    this.portRange = portRange;
  }
}
