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

public class DescribePrincipalType extends IdentityMessage {

  private String accessKeyId;
  private String certificateId;
  private String userId;
  private String username;
  private String roleId;
  private String accountId;
  private String canonicalId;
  private String nonce;
  private String ptag;

  public String getAccessKeyId( ) {
    return accessKeyId;
  }

  public void setAccessKeyId( String accessKeyId ) {
    this.accessKeyId = accessKeyId;
  }

  public String getCertificateId( ) {
    return certificateId;
  }

  public void setCertificateId( String certificateId ) {
    this.certificateId = certificateId;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getUsername( ) {
    return username;
  }

  public void setUsername( String username ) {
    this.username = username;
  }

  public String getRoleId( ) {
    return roleId;
  }

  public void setRoleId( String roleId ) {
    this.roleId = roleId;
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
  }

  public String getCanonicalId( ) {
    return canonicalId;
  }

  public void setCanonicalId( String canonicalId ) {
    this.canonicalId = canonicalId;
  }

  public String getNonce( ) {
    return nonce;
  }

  public void setNonce( String nonce ) {
    this.nonce = nonce;
  }

  public String getPtag( ) {
    return ptag;
  }

  public void setPtag( String ptag ) {
    this.ptag = ptag;
  }
}
