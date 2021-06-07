/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare.common.identity.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Principal extends EucalyptusData {

  private Boolean enabled;
  private String arn;
  private String userId;
  private String roleId;
  private String canonicalId;
  private String accountAlias;
  private String token;
  private String passwordHash;
  private Long passwordExpiry;
  private ArrayList<AccessKey> accessKeys;
  private ArrayList<Certificate> certificates;
  private ArrayList<Policy> policies;
  private String ptag;

  public Boolean getEnabled( ) {
    return enabled;
  }

  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public String getUserId( ) {
    return userId;
  }

  public void setUserId( String userId ) {
    this.userId = userId;
  }

  public String getRoleId( ) {
    return roleId;
  }

  public void setRoleId( String roleId ) {
    this.roleId = roleId;
  }

  public String getCanonicalId( ) {
    return canonicalId;
  }

  public void setCanonicalId( String canonicalId ) {
    this.canonicalId = canonicalId;
  }

  public String getAccountAlias( ) {
    return accountAlias;
  }

  public void setAccountAlias( String accountAlias ) {
    this.accountAlias = accountAlias;
  }

  public String getToken( ) {
    return token;
  }

  public void setToken( String token ) {
    this.token = token;
  }

  public String getPasswordHash( ) {
    return passwordHash;
  }

  public void setPasswordHash( String passwordHash ) {
    this.passwordHash = passwordHash;
  }

  public Long getPasswordExpiry( ) {
    return passwordExpiry;
  }

  public void setPasswordExpiry( Long passwordExpiry ) {
    this.passwordExpiry = passwordExpiry;
  }

  public ArrayList<AccessKey> getAccessKeys( ) {
    return accessKeys;
  }

  public void setAccessKeys( ArrayList<AccessKey> accessKeys ) {
    this.accessKeys = accessKeys;
  }

  public ArrayList<Certificate> getCertificates( ) {
    return certificates;
  }

  public void setCertificates( ArrayList<Certificate> certificates ) {
    this.certificates = certificates;
  }

  public ArrayList<Policy> getPolicies( ) {
    return policies;
  }

  public void setPolicies( ArrayList<Policy> policies ) {
    this.policies = policies;
  }

  public String getPtag( ) {
    return ptag;
  }

  public void setPtag( String ptag ) {
    this.ptag = ptag;
  }
}
