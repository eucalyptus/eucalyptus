/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;

/**
 *
 */
public class TestUser implements User {
  private static final long serialVersionUID = 1L;

  private Account account;
  private String accountNumber;
  private String userId;
  private String name;
  private String path;
  private RegistrationStatus registrationStatus;
  private Boolean enabled;
  private String token;
  private String confirmationCode;
  private String password;
  private Long passwordExpires;
  private List<AccessKey> keys;
  private List<Certificate> certificates;
  private List<Group> groups;
  private List<Policy> policies;
  private boolean systemAdmin;
  private boolean systemUser;
  private boolean accountAdmin;

  /**
   * Set any properties required for this user to be considered valid.
   */
  public TestUser activate( ) {
    setEnabled( Boolean.TRUE );
    setRegistrationStatus( RegistrationStatus.CONFIRMED );
    return this;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount( final Account account ) {
    this.account = account;
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

  public RegistrationStatus getRegistrationStatus() {
    return registrationStatus;
  }

  public void setRegistrationStatus( final RegistrationStatus registrationStatus ) {
    this.registrationStatus = registrationStatus;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  public void setEnabled( final Boolean enabled ) {
    this.enabled = enabled;
  }

  public String getToken() {
    return token;
  }

  public void setToken( final String token ) {
    this.token = token;
  }

  public String getConfirmationCode() {
    return confirmationCode;
  }

  public void setConfirmationCode( final String confirmationCode ) {
    this.confirmationCode = confirmationCode;
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

  public List<Group> getGroups() {
    return groups;
  }

  public void setGroups( final List<Group> groups ) {
    this.groups = groups;
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

  @Override
  public String resetToken() throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void createConfirmationCode() throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public String getInfo( final String key ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public Map<String, String> getInfo() throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void setInfo( final String key, final String value ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void setInfo( final Map<String, String> newInfo ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void removeInfo( final String key ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public AccessKey getKey( final String keyId ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void removeKey( final String keyId ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public AccessKey createKey() throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public Certificate getCertificate( final String certificateId ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public Certificate addCertificate( final X509Certificate certificate ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void removeCertificate( final String certificateId ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public Policy addPolicy( final String name, final String policy ) throws AuthException, PolicyParseException {
    throw new AuthException("Not implemented");
  }

  @Override
  public Policy putPolicy( final String name, final String policy ) throws AuthException, PolicyParseException {
    throw new AuthException("Not implemented");
  }

  @Override
  public void removePolicy( final String name ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public List<Authorization> lookupAuthorizations( final String resourceType ) throws AuthException {
    throw new AuthException("Not implemented");
  }

  @Override
  public List<Authorization> lookupQuotas( final String resourceType ) throws AuthException {
    throw new AuthException("Not implemented");
  }
}
