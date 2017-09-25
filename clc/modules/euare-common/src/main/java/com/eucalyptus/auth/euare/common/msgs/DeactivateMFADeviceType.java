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

@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = IamPolicySpec.IAM_DEACTIVATEMFADEVICE )
public class DeactivateMFADeviceType extends EuareMessage {

  private String userName;
  private String serialNumber;

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getSerialNumber( ) {
    return serialNumber;
  }

  public void setSerialNumber( String serialNumber ) {
    this.serialNumber = serialNumber;
  }
}
