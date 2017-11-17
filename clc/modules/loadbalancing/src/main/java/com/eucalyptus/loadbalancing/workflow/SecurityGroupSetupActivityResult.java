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
