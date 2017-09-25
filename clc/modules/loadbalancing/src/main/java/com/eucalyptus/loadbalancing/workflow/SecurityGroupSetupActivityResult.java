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
public class SecurityGroupSetupActivityResult {

  private String createdGroupName = null;
  private String createdGroupId = null;
  private String groupName = null;
  private String groupId = null;
  private String groupOwnerAccountId = null;
  private boolean shouldRollback = true;

  public String getCreatedGroupName( ) {
    return createdGroupName;
  }

  public void setCreatedGroupName( String createdGroupName ) {
    this.createdGroupName = createdGroupName;
  }

  public String getCreatedGroupId( ) {
    return createdGroupId;
  }

  public void setCreatedGroupId( String createdGroupId ) {
    this.createdGroupId = createdGroupId;
  }

  public String getGroupName( ) {
    return groupName;
  }

  public void setGroupName( String groupName ) {
    this.groupName = groupName;
  }

  public String getGroupId( ) {
    return groupId;
  }

  public void setGroupId( String groupId ) {
    this.groupId = groupId;
  }

  public String getGroupOwnerAccountId( ) {
    return groupOwnerAccountId;
  }

  public void setGroupOwnerAccountId( String groupOwnerAccountId ) {
    this.groupOwnerAccountId = groupOwnerAccountId;
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
