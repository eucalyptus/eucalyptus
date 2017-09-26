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
package com.eucalyptus.empyrean;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/**
 * For better mapping account ARNs to user-readable names
 */
public class ServiceAccount extends EucalyptusData {

  private String accountName;
  private String accountNumber;
  private String accountCanonicalId;

  public ServiceAccount( ) {
  }

  public ServiceAccount( String name, String number, String canonicalId ) {
    this.accountName = name;
    this.accountNumber = number;
    this.accountCanonicalId = canonicalId;
  }

  public String getAccountName( ) {
    return accountName;
  }

  public void setAccountName( String accountName ) {
    this.accountName = accountName;
  }

  public String getAccountNumber( ) {
    return accountNumber;
  }

  public void setAccountNumber( String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getAccountCanonicalId( ) {
    return accountCanonicalId;
  }

  public void setAccountCanonicalId( String accountCanonicalId ) {
    this.accountCanonicalId = accountCanonicalId;
  }
}
