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
