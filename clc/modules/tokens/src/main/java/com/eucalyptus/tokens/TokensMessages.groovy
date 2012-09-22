/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tokens

import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import com.eucalyptus.auth.policy.PolicyAction
import com.eucalyptus.auth.policy.PolicySpec
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import com.eucalyptus.component.ComponentId.ComponentMessage;
import com.eucalyptus.component.id.Tokens

@ComponentMessage(Tokens.class)
class TokenMessage extends BaseMessage {
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GET_SESSION_TOKEN )
class GetSessionTokenType extends TokenMessage {
  int durationSeconds
}

class GetSessionTokenResponseType extends TokenMessage  {
  GetSessionTokenResultType result
  TokensResponseMetadataType responseMetadata = new TokensResponseMetadataType( );
}

@PolicyAction( vendor = PolicySpec.VENDOR_STS, action = PolicySpec.STS_GET_FEDERATION_TOKEN )
class GetFederationTokenType extends TokenMessage {
  String name
  String policy
  int durationSeconds
}

class GetFederationTokenResponseType extends TokenMessage {
  GetFederationTokenResultType result
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

class TokensErrorResponseType extends TokenMessage {
  String requestId
  HttpResponseStatus httpStatus;
  ArrayList<TokensErrorType> errors = new ArrayList<TokensErrorType>()
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

class CredentialsType extends EucalyptusData {
  String accessKeyId
  String secretAccessKey
  String sessionToken
  Date expiration
}

class FederatedUserType extends EucalyptusData {
  String federatedUserId
  String arn
}
