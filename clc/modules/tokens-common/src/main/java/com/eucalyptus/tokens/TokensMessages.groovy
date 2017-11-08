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
@GroovyAddClassUUID
package com.eucalyptus.tokens

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.ws.WebServiceError
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.auth.policy.annotation.PolicyAction
import com.eucalyptus.auth.policy.PolicySpec
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.component.id.Tokens

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(Tokens.class)
class TokenMessage extends BaseMessage {
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETCALLERIDENTITY )
class GetCallerIdentityType extends TokenMessage {
}

class GetCallerIdentityResponseType extends TokenMessage {
  GetCallerIdentityResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETSESSIONTOKEN )
class GetSessionTokenType extends TokenMessage {
  int durationSeconds
}

class GetSessionTokenResponseType extends TokenMessage  {
  GetSessionTokenResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETFEDERATIONTOKEN )
class GetFederationTokenType extends TokenMessage {
  String name
  String policy
  int durationSeconds
}

class GetFederationTokenResponseType extends TokenMessage {
  GetFederationTokenResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETACCESSTOKEN )
class GetAccessTokenType extends TokenMessage {
  int durationSeconds
}

class GetAccessTokenResponseType extends TokenMessage  {
  GetAccessTokenResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GETIMPERSONATIONTOKEN )
class GetImpersonationTokenType extends TokenMessage {
  @HttpParameterMapping(parameter="UserId")
  String impersonatedUserId
  String accountAlias
  String userName
  int durationSeconds
}

class GetImpersonationTokenResponseType extends TokenMessage  {
  GetImpersonationTokenResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

class TokensResponseMetadataType extends EucalyptusData {
  String requestId
}

class TokensErrorType extends EucalyptusData {
  String type
  String code
  String message
  TokensErrorDetailType detail = new TokensErrorDetailType( );
}

class TokensErrorDetailType extends EucalyptusData {
}

class TokensErrorResponseType extends TokenMessage implements WebServiceError {
  String requestId
  HttpResponseStatus httpStatus;
  ArrayList<TokensErrorType> errors = new ArrayList<TokensErrorType>( )

  TokensErrorResponseType( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${errors?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    errors?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    errors?.getAt(0)?.message
  }
}

class GetCallerIdentityResultType extends EucalyptusData {
  String arn
  String userId
  String account

  GetCallerIdentityResultType( ) { }

  GetCallerIdentityResultType( final String arn, final String userId, final String account ) {
    this.arn = arn
    this.userId = userId
    this.account = account
  }
}


class GetSessionTokenResultType extends EucalyptusData {
  CredentialsType credentials

  static GetSessionTokenResultType forCredentials( final String accessKeyId,
                                                   final String secretAccessKey,
                                                   final String sessionToken,
                                                   final long expiryTime ) {
    return new GetSessionTokenResultType( credentials:
      new CredentialsType(
          accessKeyId: accessKeyId,
          secretAccessKey: secretAccessKey,
          sessionToken: sessionToken,
          expiration: new Date( expiryTime )
      )
    )
  }
}

class GetFederationTokenResultType extends EucalyptusData {
  CredentialsType credentials
  FederatedUserType federatedUser
  int packedPolicySize
}

class GetAccessTokenResultType extends EucalyptusData {
  CredentialsType credentials

  static GetAccessTokenResultType forCredentials( final String accessKeyId,
                                                  final String secretAccessKey,
                                                  final String sessionToken,
                                                  final long expiryTime ) {
    return new GetAccessTokenResultType( credentials:
        new CredentialsType(
            accessKeyId: accessKeyId,
            secretAccessKey: secretAccessKey,
            sessionToken: sessionToken,
            expiration: new Date( expiryTime )
        )
    )
  }
}

class GetImpersonationTokenResultType extends EucalyptusData {
  CredentialsType credentials

  static GetImpersonationTokenResultType forCredentials( final String accessKeyId,
                                                         final String secretAccessKey,
                                                         final String sessionToken,
                                                         final long expiryTime ) {
    return new GetImpersonationTokenResultType( credentials:
        new CredentialsType(
            accessKeyId: accessKeyId,
            secretAccessKey: secretAccessKey,
            sessionToken: sessionToken,
            expiration: new Date( expiryTime )
        )
    )
  }
}

class CredentialsType extends EucalyptusData {
  String accessKeyId
  String secretAccessKey
  String sessionToken
  Date expiration

  public CredentialsType() {}

  CredentialsType( String accessKeyId,
                   String secretAccessKey,
                   String sessionToken,
                   long expiration) {
    this.accessKeyId = accessKeyId
    this.secretAccessKey = secretAccessKey
    this.sessionToken = sessionToken
    this.expiration = new Date(expiration)
  }
}

class FederatedUserType extends EucalyptusData {
  String federatedUserId
  String arn
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_ASSUMEROLE )
public class AssumeRoleType extends TokenMessage {
  String roleArn;
  String roleSessionName;
  String policy;
  Integer durationSeconds;
  String externalId;
  public AssumeRoleType() {  }
}

public class AssumeRoleResponseType extends TokenMessage {
  public AssumeRoleResponseType() {  }
  AssumeRoleResultType assumeRoleResult = new AssumeRoleResultType();
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType();
}

public class AssumeRoleResultType extends EucalyptusData {
  CredentialsType credentials;
  AssumedRoleUserType assumedRoleUser;
  Integer packedPolicySize;
  public AssumeRoleResultType() {  }
}

public class AssumedRoleUserType extends EucalyptusData {
  String assumedRoleId;
  String arn;
  public AssumedRoleUserType() {  }

  AssumedRoleUserType( String assumedRoleId,
                       String arn) {
    this.assumedRoleId = assumedRoleId
    this.arn = arn
  }
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_ASSUMEROLEWITHWEBIDENTITY )
public class AssumeRoleWithWebIdentityType extends TokenMessage {
  String roleArn;
  String roleSessionName;
  String policy;
  String providerId;
  Integer durationSeconds;
  String webIdentityToken;
  public AssumeRoleWithWebIdentityType() {  }
}

public class AssumeRoleWithWebIdentityResponseType extends TokenMessage {
  public AssumeRoleWithWebIdentityResponseType() {  }
  AssumeRoleWithWebIdentityResultType assumeRoleWithWebIdentityResult = new AssumeRoleWithWebIdentityResultType();
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType();
}

public class AssumeRoleWithWebIdentityResultType extends EucalyptusData {
  CredentialsType credentials;
  AssumedRoleUserType assumedRoleUser;
  Integer packedPolicySize;
  String audience;
  String provider;
  String subjectFromWebIdentityToken;
  public AssumeRoleWithWebIdentityResultType() {  }
}

