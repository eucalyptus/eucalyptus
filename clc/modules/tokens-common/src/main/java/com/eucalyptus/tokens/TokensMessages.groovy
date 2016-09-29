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

