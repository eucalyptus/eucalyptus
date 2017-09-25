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

@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = IamPolicySpec.IAM_DELETEINSTANCEPROFILE )
public class DeleteInstanceProfileType extends EuareMessage implements EuareMessageWithDelegate {

  private String delegateAccount;
  private String instanceProfileName;

  public String getDelegateAccount( ) {
    return delegateAccount;
  }

  public void setDelegateAccount( String delegateAccount ) {
    this.delegateAccount = delegateAccount;
  }

  public String getInstanceProfileName( ) {
    return instanceProfileName;
  }

  public void setInstanceProfileName( String instanceProfileName ) {
    this.instanceProfileName = instanceProfileName;
  }
}
