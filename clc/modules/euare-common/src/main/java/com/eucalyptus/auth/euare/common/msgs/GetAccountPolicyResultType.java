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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class GetAccountPolicyResultType extends EucalyptusData {

  private String accountName;
  private String policyName;
  private String policyDocument;

  public String getAccountName( ) {
    return accountName;
  }

  public void setAccountName( String accountName ) {
    this.accountName = accountName;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  public String getPolicyDocument( ) {
    return policyDocument;
  }

  public void setPolicyDocument( String policyDocument ) {
    this.policyDocument = policyDocument;
  }
}
