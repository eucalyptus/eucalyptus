/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
@GroovyAddClassUUID
package com.eucalyptus.auth.euare.common.identity

import com.eucalyptus.component.annotation.ComponentMessage
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(Identity.class)
class IdentityMessage extends BaseMessage {
}

class DescribePrincipalType extends IdentityMessage {
  String accessKeyId
  String certificateId
  String userId
  String username // optionally used with accountId
  String roleId
  String accountId
  String canonicalId
  String nonce
  String ptag
}

class DescribePrincipalResponseType extends IdentityMessage {
  DescribePrincipalResult describePrincipalResult
}

class DescribePrincipalResult extends EucalyptusData {
  Principal principal
}

class Principal extends EucalyptusData {
  Boolean enabled
  String arn
  String userId
  String roleId // the role identifier includes the principals session name suffix
  String canonicalId
  String accountAlias
  String token
  String passwordHash
  Long passwordExpiry
  ArrayList<AccessKey> accessKeys
  ArrayList<Certificate> certificates
  ArrayList<Policy> policies
  String ptag
}

class AccessKey extends EucalyptusData {
  String accessKeyId
  String secretAccessKey
}

class Certificate extends EucalyptusData {
  String certificateId
  String certificateBody
}

class Policy extends EucalyptusData {
  String versionId
  String name
  String scope
  String policy
  String hash
}

class DescribeInstanceProfileType extends IdentityMessage {
  String accountId
  String instanceProfileName
}

class DescribeInstanceProfileResponseType extends IdentityMessage {
  DescribeInstanceProfileResult describeInstanceProfileResult
}

class DescribeInstanceProfileResult extends EucalyptusData {
  InstanceProfile instanceProfile
  Role role
}

class InstanceProfile extends EucalyptusData {
  String instanceProfileId
  String instanceProfileArn
}

class Role extends EucalyptusData {
  String roleId
  String roleArn
  String secret
  Policy assumeRolePolicy
}

class DescribeRoleType extends IdentityMessage {
  String accountId
  String roleName
}

class DescribeRoleResponseType extends IdentityMessage {
  DescribeRoleResult describeRoleResult
}

class DescribeRoleResult extends EucalyptusData {
  Role role
}

class OidcProvider extends EucalyptusData {
  String providerArn
  Integer port
  ArrayList<String> clientIds = Lists.newArrayList( )
  ArrayList<String> thumbprints = Lists.newArrayList( )
}

class DescribeOidcProviderType extends IdentityMessage {
  String accountId
  String providerUrl
}

class DescribeOidcProviderResponseType extends IdentityMessage {
  DescribeOidcProviderResult describeOidcProviderResult
}

class DescribeOidcProviderResult extends EucalyptusData {
  OidcProvider oidcProvider
}

class DescribeAccountsType extends IdentityMessage {
  String alias
  String aliasLike
  String canonicalId
  String email
}

class DescribeAccountsResponseType extends IdentityMessage {
  DescribeAccountsResult describeAccountsResult
}

class DescribeAccountsResult extends EucalyptusData {
  ArrayList<Account> accounts
}

class Account extends EucalyptusData {
  String accountNumber
  String alias
  String canonicalId
}

class DecodeSecurityTokenType extends IdentityMessage {
  String accessKeyId
  String securityToken
}

class DecodeSecurityTokenResponseType extends IdentityMessage {
  DecodeSecurityTokenResult decodeSecurityTokenResult
}

class DecodeSecurityTokenResult extends EucalyptusData {
  SecurityToken securityToken
}

class SecurityToken extends EucalyptusData {
  String originatingAccessKeyId
  String originatingUserId
  String originatingRoleId
  String nonce
  Long created
  Long expires
  ArrayList<SecurityTokenAttribute> attributes = Lists.newArrayList( )
}

class SecurityTokenAttribute extends EucalyptusData {
  String key
  String value

  SecurityTokenAttribute( ) { }

  SecurityTokenAttribute( final String key, final String value ) {
    this.key = key
    this.value = value
  }
}

class ReserveNameType extends IdentityMessage {
  String namespace
  String name
  Integer duration // seconds
  String clientToken // callers token identifying the request
}

class ReserveNameResponseType extends IdentityMessage {
  ReserveNameResult reserveNameResult
}

class ReserveNameResult extends EucalyptusData {
}

class DescribeCertificateType extends IdentityMessage {
}

class DescribeCertificateResponseType extends IdentityMessage {
  DescribeCertificateResult describeCertificateResult
}

class DescribeCertificateResult extends EucalyptusData {
  String pem
}

class SignCertificateType extends IdentityMessage {
  String key
  String principal
  Integer expirationDays
}

class SignCertificateResponseType extends IdentityMessage {
  SignCertificateResult signCertificateResult
}

class SignCertificateResult extends EucalyptusData {
  String pem
}

class TunnelActionType extends IdentityMessage {
  String content // internal XML format
}

class TunnelActionResponseType extends IdentityMessage {
  TunnelActionResult tunnelActionResult
}

class TunnelActionResult extends EucalyptusData {
  String content
}

