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
package com.eucalyptus.auth.euare.principal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.ServerCertificate;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.BasePrincipal;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;

/**
 *
 */
@PolicyVendor( PolicySpec.VENDOR_IAM )
@PolicyResourceType( PolicySpec.IAM_RESOURCE_ACCOUNT )
public interface EuareAccount extends AccountIdentifiers, BasePrincipal, RestrictedType, Serializable {

  public boolean hasAccountAlias( );

  public void setName( String name ) throws AuthException;

  /**
   * Set name without performing syntax validation
   */
  public void setNameUnsafe( String name ) throws AuthException;

  public List<EuareUser> getUsers( ) throws AuthException;

  public List<EuareGroup> getGroups( ) throws AuthException;

  public List<EuareRole> getRoles( ) throws AuthException;

  public List<EuareInstanceProfile> getInstanceProfiles( ) throws AuthException;

  public EuareUser addUser( String userName, String path, boolean enabled, Map<String, String> info ) throws AuthException;
  public void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException;

  public EuareRole addRole( String roleName, String path, String assumeRolePolicy ) throws AuthException, PolicyParseException;
  public void deleteRole( String roleName ) throws AuthException;

  public EuareGroup addGroup( String groupName, String path ) throws AuthException;
  public void deleteGroup( String groupName, boolean recursive ) throws AuthException;

  public EuareInstanceProfile addInstanceProfile( String instanceProfileName, String path ) throws AuthException;
  public void deleteInstanceProfile( String instanceProfileName ) throws AuthException;

  public ServerCertificate addServerCertificate(String certName, String certBody, String certChain, String path, String pk) throws AuthException;
  public ServerCertificate deleteServerCertificate(String certName) throws AuthException;

  public EuareGroup lookupGroupByName( String groupName ) throws AuthException;

  public EuareUser lookupUserByName( String userName ) throws AuthException;

  public EuareRole lookupRoleByName( String roleName ) throws AuthException;

  public EuareInstanceProfile lookupInstanceProfileByName( String instanceProfileName ) throws AuthException;

  public EuareUser lookupAdmin() throws AuthException;

  public EuareOpenIdConnectProvider lookupOpenIdConnectProvider( String url ) throws AuthException;

  public ServerCertificate lookupServerCertificate(String certName) throws AuthException;
  public List<ServerCertificate> listServerCertificates(String pathPrefix) throws AuthException;
  public void updateServerCeritificate(String certName, String newCertName, String newPath) throws AuthException;

  public String getAccountNumber( );
  public String getCanonicalId( );

  public EuareOpenIdConnectProvider createOpenIdConnectProvider(String url, List<String> clientIDList, List<String> thumbprintList) throws AuthException;
  public void deleteOpenIdConnectProvider(String openIDConnectProviderArn) throws AuthException;
  public EuareOpenIdConnectProvider getOpenIdConnectProvider(String arn) throws AuthException;
  public List<String> listOpenIdConnectProviders() throws AuthException;

  public void addClientIdToOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  public void removeClientIdFromOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  public void updateOpenIdConnectProviderThumbprint(String arn, List<String> thumbprintList) throws AuthException;
}
