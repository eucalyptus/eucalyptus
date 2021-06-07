/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
