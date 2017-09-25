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
