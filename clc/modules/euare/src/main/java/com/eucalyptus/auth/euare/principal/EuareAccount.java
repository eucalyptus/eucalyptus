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
  public List<EuareOpenIdConnectProvider> listOpenIdConnectProviders() throws AuthException;

  public void addClientIdToOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  public void removeClientIdFromOpenIdConnectProvider(String clientId, String arn) throws AuthException;
  public void updateOpenIdConnectProviderThumbprint(String arn, List<String> thumbprintList) throws AuthException;
}
