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
import com.eucalyptus.binding.HttpParameterMapping;

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETIMPERSONATIONTOKEN )
public class GetImpersonationTokenType extends TokenMessage {
  @HttpParameterMapping( parameter = "UserId" )
  private String impersonatedUserId;
  private String accountAlias;
  private String userName;
  private int durationSeconds;

  public String getImpersonatedUserId() {
    return impersonatedUserId;
  }

  public void setImpersonatedUserId( String impersonatedUserId ) {
    this.impersonatedUserId = impersonatedUserId;
  }

  public String getAccountAlias() {
    return accountAlias;
  }

  public void setAccountAlias( String accountAlias ) {
    this.accountAlias = accountAlias;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds( int durationSeconds ) {
    this.durationSeconds = durationSeconds;
  }
}
