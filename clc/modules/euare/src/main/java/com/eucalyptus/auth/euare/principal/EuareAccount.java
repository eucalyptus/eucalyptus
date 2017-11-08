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
