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

public class DescribeAccountsType extends IdentityMessage {

  private String alias;
  private String aliasLike;
  private String canonicalId;
  private String email;

  public String getAlias( ) {
    return alias;
  }

  public void setAlias( String alias ) {
    this.alias = alias;
  }

  public String getAliasLike( ) {
    return aliasLike;
  }

  public void setAliasLike( String aliasLike ) {
    this.aliasLike = aliasLike;
  }

  public String getCanonicalId( ) {
    return canonicalId;
  }

  public void setCanonicalId( String canonicalId ) {
    this.canonicalId = canonicalId;
  }

  public String getEmail( ) {
    return email;
  }

  public void setEmail( String email ) {
    this.email = email;
  }
}
