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
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.BasePrincipal;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.auth.type.RestrictedType;

/**
 *
 */
@PolicyVendor( IamPolicySpec.VENDOR_IAM )
@PolicyResourceType( IamPolicySpec.IAM_RESOURCE_ACCOUNT )
public interface EuareAccount extends AccountIdentifiers, BasePrincipal, RestrictedType, Serializable {

  boolean hasAccountAlias( );

  void setName( String name ) throws AuthException;

  /**
   * Set name without performing syntax validation
   */
  void setNameUnsafe( String name ) throws AuthException;

  List<EuareUser> getUsers( ) throws AuthException;

  List<EuareGroup> getGroups( ) throws AuthException;

  List<EuareRole> getRoles( ) throws AuthException;

  List<EuareInstanceProfile> getInstanceProfiles( ) throws AuthException;

  List<EuareManagedPolicy> getPolicies( Boolean attached ) throws AuthException;

  /**
   * Get count for managed policies
   */
  long countPolicies( ) throws AuthException;

  EuareUser addUser( String userName, String path, boolean enabled, Map<String, String> info ) throws AuthException;
  void deleteUser( String userName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException;

  EuareRole addRole( String roleName, String path, String assumeRolePolicy ) throws AuthException, PolicyParseException;
  void deleteRole( String roleName ) throws AuthException;

  EuareGroup addGroup( String groupName, String path ) throws AuthException;
  void deleteGroup( String groupName, boolean recursive ) throws AuthException;

  EuareInstanceProfile addInstanceProfile( String instanceProfileName, String path ) throws AuthException;
  void deleteInstanceProfile( String instanceProfileName ) throws AuthException;

  ServerCertificate addServerCertificate(String certName, String certBody, String certChain, String path, String pk) throws AuthException;
  ServerCertificate deleteServerCertificate(String certName) throws AuthException;

  EuareManagedPolicy addPolicy( String policyName, String path, String description, String policy ) throws AuthException;
  void deletePolicy( String policyName ) throws AuthException;

  EuareGroup lookupGroupByName( String groupName ) throws AuthException;

  EuareUser lookupUserByName( String userName ) throws AuthException;

  EuareRole lookupRoleByName( String roleName ) throws AuthException;

  EuareInstanceProfile lookupInstanceProfileByName( String instanceProfileName ) throws AuthException;

  EuareManagedPolicy lookupPolicyByName( String policyName ) throws AuthException;

  EuareUser lookupAdmin() throws AuthException;

  EuareOpenIdConnectProvider lookupOpenIdConnectProvider( String url ) throws AuthException;

  ServerCertificate lookupServerCertificate(String certName) throws AuthException;
  List<ServerCertificate> listServerCertificates(String pathPrefix) throws AuthException;
  void updateServerCeritificate(String certName, String newCertName, String newPath) throws AuthException;

  String getAccountNumber( );
  String getCanonicalId( );

  EuareOpenIdConnectProvider createOpenIdConnectProvider(String url, List<String> clientIDList, List<String> thumbprintList) throws AuthException;
  void deleteOpenIdConnectProvider(String openIDConnectProviderArn) throws AuthException;
  EuareOpenIdConnectProvider getOpenIdConnectProvider(String arn) throws AuthException;
  List<EuareOpenIdConnectProvider> listOpenIdConnectProviders() throws AuthException;

  void addClientIdToOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  void removeClientIdFromOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  void updateOpenIdConnectProviderThumbprint(String arn, List<String> thumbprintList) throws AuthException;
}
