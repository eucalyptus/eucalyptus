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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class GetCallerIdentityResultType extends EucalyptusData {
  private String arn;
  private String userId;
  private String account;

  public GetCallerIdentityResultType() {
  }

  public GetCallerIdentityResultType( final String arn, final String userId, final String account ) {
    this.arn = arn;
    this.userId = userId;
    this.account = account;
  }

  public String getArn() {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount( String account ) {
    this.account = account;
  }
}
