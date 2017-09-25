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
package com.eucalyptus.loadbalancing.workflow;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect( isGetterVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY )
public class AccessLogPolicyActivityResult {

  private String roleName = null;
  private String policyName = null;
  private boolean shouldRollback = true;

  public String getRoleName( ) {
    return roleName;
  }

  public void setRoleName( String roleName ) {
    this.roleName = roleName;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  public boolean getShouldRollback( ) {
    return shouldRollback;
  }

  public boolean isShouldRollback( ) {
    return shouldRollback;
  }

  public void setShouldRollback( boolean shouldRollback ) {
    this.shouldRollback = shouldRollback;
  }
}
