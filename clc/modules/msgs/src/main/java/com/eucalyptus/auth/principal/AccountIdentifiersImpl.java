/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.principal;

/**
 *
 */
public class AccountIdentifiersImpl implements AccountIdentifiers {

  private final String accountNumber;
  private final String accountAlias;
  private final String canonicalId;

  public AccountIdentifiersImpl(
      final String accountNumber,
      final String accountAlias,
      final String canonicalId
  ) {
    this.accountNumber = accountNumber;
    this.accountAlias = accountAlias;
    this.canonicalId = canonicalId;
  }

  @Override
  public String getAccountNumber() {
    return accountNumber;
  }

  @Override
  public String getAccountAlias() {
    return accountAlias;
  }

  @Override
  public String getCanonicalId() {
    return canonicalId;
  }
}
