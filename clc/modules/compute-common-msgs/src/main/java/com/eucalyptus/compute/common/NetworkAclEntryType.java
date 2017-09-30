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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class NetworkAclEntryType extends EucalyptusData {

  private Integer ruleNumber;
  private String protocol;
  private String ruleAction;
  private Boolean egress;
  private String cidrBlock;
  private IcmpTypeCodeType icmpTypeCode;
  private PortRangeType portRange;

  public NetworkAclEntryType( ) {
  }

  public NetworkAclEntryType( final Integer ruleNumber, final String protocol, final String ruleAction, final Boolean egress, final String cidrBlock, final Integer icmpCode, final Integer icmpType, final Integer portRangeFrom, final Integer portRangeTo ) {
    this.ruleNumber = ruleNumber;
    this.protocol = protocol;
    this.ruleAction = ruleAction;
    this.egress = egress;
    this.cidrBlock = cidrBlock;
    if ( icmpCode != null && icmpCode > 0 ) {
      this.icmpTypeCode = new IcmpTypeCodeType( icmpCode, icmpType );
    }
    if ( portRangeFrom != null && portRangeFrom > 0 ) {
      this.portRange = new PortRangeType( portRangeFrom, portRangeTo );
    }

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
