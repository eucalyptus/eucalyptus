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

import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.ServerCertificate;

/**
 *
 */
public class TestAccount implements Account {

  private String name;
  private String accountNumber;
  private String canonicalId;

  public String getName() {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber( final String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getCanonicalId() {
    return canonicalId;
  }

  public void setCanonicalId( final String canonicalId ) {
    this.canonicalId = canonicalId;
  }

  @Override
  public List<User> getUsers() throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<Group> getGroups() throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<Role> getRoles() throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<InstanceProfile> getInstanceProfiles() throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public User addUser( final String userName, final String path, final boolean skipRegistration, final boolean enabled, final Map<String, String> info ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public void deleteUser( final String userName, final boolean forceDeleteAdmin, final boolean recursive ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public Role addRole( final String roleName, final String path, final String assumeRolePolicy ) throws AuthException, PolicyParseException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public void deleteRole( final String roleName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public Group addGroup( final String groupName, final String path ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public void deleteGroup( final String groupName, final boolean recursive ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public InstanceProfile addInstanceProfile( final String instanceProfileName, final String path ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public void deleteInstanceProfile( final String instanceProfileName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public Group lookupGroupByName( final String groupName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public User lookupUserByName( final String userName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public Role lookupRoleByName( final String roleName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String instanceProfileName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public User lookupAdmin() throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<Authorization> lookupAccountGlobalAuthorizations( final String resourceType ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<Authorization> lookupAccountGlobalQuotas( final String resourceType ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public ServerCertificate addServerCertificate( final String certName, final String certBody, final String certChain, final String path, final String pk ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public ServerCertificate deleteServerCertificate( final String certName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public ServerCertificate lookupServerCertificate( final String certName ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public List<ServerCertificate> listServerCertificates( final String pathPrefix ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }

  @Override
  public void updateServerCeritificate( final String certName, final String newCertName, final String newPath ) throws AuthException {
    throw new AuthException( "Not implemented" );
  }
}
