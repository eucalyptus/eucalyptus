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

import java.util.Date;
import java.util.List;

/**
 *
 */
public class TestUser implements User {
  private static final long serialVersionUID = 1L;

  private String accountNumber;
  private String userId;
  private String name;
  private String path;
  private boolean enabled;
  private String token;
  private String password;
  private Long passwordExpires;
  private List<AccessKey> keys;
  private List<Certificate> certificates;
  private List<Policy> policies;
  private boolean systemAdmin;
  private boolean systemUser;
  private boolean accountAdmin;

  /**
   * Set any properties required for this user to be considered valid.
   */
  public TestUser activate( ) {
    setEnabled( true );
    return this;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber( final String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath( final String path ) {
    this.path = path;
  }

  public Date getCreateDate( ){ return null; }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled( final boolean enabled ) {
    this.enabled = enabled;
  }

  public String getToken() {
    return token;
  }

  public void setToken( final String token ) {
    this.token = token;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword( final String password ) {
    this.password = password;
  }

  public Long getPasswordExpires() {
    return passwordExpires;
  }

  public void setPasswordExpires( final Long passwordExpires ) {
    this.passwordExpires = passwordExpires;
  }

  public List<AccessKey> getKeys() {
    return keys;
  }

  public void setKeys( final List<AccessKey> keys ) {
    this.keys = keys;
  }

  public List<Certificate> getCertificates() {
    return certificates;
  }

  public void setCertificates( final List<Certificate> certificates ) {
    this.certificates = certificates;
  }

  public List<Policy> getPolicies() {
    return policies;
  }

  public void setPolicies( final List<Policy> policies ) {
    this.policies = policies;
  }

  public boolean isSystemAdmin() {
    return systemAdmin;
  }

  public void setSystemAdmin( final boolean systemAdmin ) {
    this.systemAdmin = systemAdmin;
  }

  public boolean isSystemUser() {
    return systemUser;
  }

  public void setSystemUser( final boolean systemUser ) {
    this.systemUser = systemUser;
  }

  public boolean isAccountAdmin() {
    return accountAdmin;
  }

  public void setAccountAdmin( final boolean accountAdmin ) {
    this.accountAdmin = accountAdmin;
  }

}
