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
package com.eucalyptus.auth.euare.common.msgs;

import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyAction;

@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = IamPolicySpec.IAM_UPDATEUSER )
public class UpdateUserType extends EuareMessage implements EuareMessageWithDelegate {

  private String delegateAccount;
  private String userName;
  private String newPath;
  private String newUserName;
  private String enabled;
  private String passwordExpiration;

  public String getDelegateAccount( ) {
    return delegateAccount;
  }

  public void setDelegateAccount( String delegateAccount ) {
    this.delegateAccount = delegateAccount;
  }

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getNewPath( ) {
    return newPath;
  }

  public void setNewPath( String newPath ) {
    this.newPath = newPath;
  }

  public String getNewUserName( ) {
    return newUserName;
  }

  public void setNewUserName( String newUserName ) {
    this.newUserName = newUserName;
  }

  public String getEnabled( ) {
    return enabled;
  }

  public void setEnabled( String enabled ) {
    this.enabled = enabled;
  }

  public String getPasswordExpiration( ) {
    return passwordExpiration;
  }

  public void setPasswordExpiration( String passwordExpiration ) {
    this.passwordExpiration = passwordExpiration;
  }
}
