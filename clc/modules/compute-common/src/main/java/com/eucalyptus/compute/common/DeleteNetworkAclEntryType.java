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

import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;

public class DeleteNetworkAclEntryType extends VpcMessage {

  private String networkAclId;
  @ComputeMessageValidation.FieldRange( min = 1l, max = 32766l )
  private Integer ruleNumber;
  private Boolean egress = false;

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

  public Boolean getEgress( ) {
    return egress;
  }

  public void setEgress( Boolean egress ) {
    this.egress = egress;
  }
}
