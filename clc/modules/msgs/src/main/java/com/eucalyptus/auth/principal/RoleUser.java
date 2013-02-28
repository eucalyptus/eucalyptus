/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * User with permissions defined by a role.
 */
public final class RoleUser implements User {
  private static final long serialVersionUID = 1L;
  private final Role role;
  private final User user;

  public RoleUser( final Role role,
                   final User user ) {
    this.role = role;
    this.user = user;
  }

  @Override
  public Account getAccount() throws AuthException {
    return role.getAccount();
  }

  @Override
  public List<Policy> getPolicies() throws AuthException {
    return role.getPolicies();
  }

  @Override
  public Policy addPolicy( final String name, final String policy ) throws AuthException, PolicyParseException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void removePolicy( final String name ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public List<Authorization> lookupAuthorizations( final String resourceType ) throws AuthException {
    return role.lookupAuthorizations( resourceType );
  }

  @Override
  public List<Authorization> lookupQuotas( final String resourceType ) throws AuthException {
    return role.lookupQuotas( resourceType );
  }

  @Override
  public String getName() {
    return user.getName();
  }

  @Override
  public String getUserId() {
    return user.getUserId();
  }

  @Override
  public void setName( final String name ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public String getPath() {
    return user.getPath();
  }

  @Override
  public void setPath( final String path ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public RegistrationStatus getRegistrationStatus() {
    return user.getRegistrationStatus();
  }

  @Override
  public void setRegistrationStatus( final RegistrationStatus stat ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public Boolean isEnabled() {
    return true;
  }

  @Override
  public void setEnabled( final Boolean enabled ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public String getToken() {
    return null;
  }

  @Override
  public void setToken( final String token ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public String resetToken() throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public String getConfirmationCode() {
    return null;
  }

  @Override
  public void setConfirmationCode( final String code ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void createConfirmationCode() throws AuthException {
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public void setPassword( final String password ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void createPassword() throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public Long getPasswordExpires() {
    return null;
  }

  @Override
  public void setPasswordExpires( final Long time ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public String getInfo( final String key ) throws AuthException {
    return null;
  }

  @Override
  public Map<String, String> getInfo() throws AuthException {
    return Maps.newHashMap();
  }

  @Override
  public void setInfo( final String key, final String value ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void setInfo( final Map<String, String> newInfo ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void removeInfo( final String key ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public List<AccessKey> getKeys() throws AuthException {
    return Lists.newArrayList();
  }

  @Override
  public AccessKey getKey( final String keyId ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void removeKey( final String keyId ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public AccessKey createKey() throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public List<Certificate> getCertificates() throws AuthException {
    return Lists.newArrayList();
  }

  @Override
  public Certificate getCertificate( final String certificateId ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public Certificate addCertificate( final X509Certificate certificate ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public void removeCertificate( final String certficateId ) throws AuthException {
    throw new AuthException( "Not supported" );
  }

  @Override
  public List<Group> getGroups() throws AuthException {
    return Lists.newArrayList();
  }

  @Override
  public boolean isSystemAdmin() {
    return false;
  }

  @Override
  public boolean isAccountAdmin() {
    return false;
  }
}
