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
package com.eucalyptus.auth.euare.common.identity.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Role extends EucalyptusData {

  private String roleId;
  private String roleArn;
  private String secret;
  private Policy assumeRolePolicy;

  public String getRoleId( ) {
    return roleId;
  }

  public void setRoleId( String roleId ) {
    this.roleId = roleId;
  }

  public String getRoleArn( ) {
    return roleArn;
  }

  public void setRoleArn( String roleArn ) {
    this.roleArn = roleArn;
  }

  public String getSecret( ) {
    return secret;
  }

  public void setSecret( String secret ) {
    this.secret = secret;
  }

  public Policy getAssumeRolePolicy( ) {
    return assumeRolePolicy;
  }

  public void setAssumeRolePolicy( Policy assumeRolePolicy ) {
    this.assumeRolePolicy = assumeRolePolicy;
  }
}
