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
package com.eucalyptus.auth.euare.common.identity.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Account extends EucalyptusData {

  private String accountNumber;
  private String alias;
  private String canonicalId;

  public String getAccountNumber( ) {
    return accountNumber;
  }

  public void setAccountNumber( String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getAlias( ) {
    return alias;
  }

  public void setAlias( String alias ) {
    this.alias = alias;
  }

  public String getCanonicalId( ) {
    return canonicalId;
  }

  public void setCanonicalId( String canonicalId ) {
    this.canonicalId = canonicalId;
  }
}
