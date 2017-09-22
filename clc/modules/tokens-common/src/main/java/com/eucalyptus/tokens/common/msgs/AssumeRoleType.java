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
package com.eucalyptus.tokens.common.msgs;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyAction;

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_ASSUMEROLE )
public class AssumeRoleType extends TokenMessage {
  private String roleArn;
  private String roleSessionName;
  private String policy;
  private Integer durationSeconds;
  private String externalId;

  public AssumeRoleType() {
  }

  public String getRoleArn() {
    return roleArn;
  }

  public void setRoleArn( String roleArn ) {
    this.roleArn = roleArn;
  }

  public String getRoleSessionName() {
    return roleSessionName;
  }

  public void setRoleSessionName( String roleSessionName ) {
    this.roleSessionName = roleSessionName;
  }

  public String getPolicy() {
    return policy;
  }

  public void setPolicy( String policy ) {
    this.policy = policy;
  }

  public Integer getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds( Integer durationSeconds ) {
    this.durationSeconds = durationSeconds;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId( String externalId ) {
    this.externalId = externalId;
  }
}
