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

package com.eucalyptus.objectstorage.pipeline.auth;

import com.eucalyptus.auth.euare.DelegatingUserPrincipal;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.crypto.util.Timestamps.Type;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.*;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.V4AuthComponent;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.AwsChunk;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.joda.time.DateTime;


import javax.annotation.Nonnull;
import javax.security.auth.Subject;
import java.util.*;

import static com.eucalyptus.objectstorage.pipeline.auth.S3V2Authentication.AWS_V2_AUTH_TYPE;

/**
 * REST and query string based V2 and V4 authentication for S3.
 *
 * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-auth-using-authorization-header.html">AWS S3 Sigv4 docs</a>
 * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html">AWS S3 Sigv2 docs</a>
 */
public final class S3Authentication {
  private static final Logger LOG = Logger.getLogger(S3Authentication.class);

  private S3Authentication() {
  }

  public enum S3Authenticator {
    V2_HEADER {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        String authHeader = request.getHeader(HttpHeaders.Names.AUTHORIZATION).replaceFirst(AWS_V2_AUTH_TYPE, "").trim();
        String[] signatureElements = authHeader.split(":");
        String accessKeyId = signatureElements[0];
        String signature = signatureElements[1];
        String dateStr = getDateFromHeaders(request);
        Date date = parseDate(dateStr);
        assertDateNotSkewed(date);
        if (request.getHeader(SecurityHeader.X_Amz_Date.header()) != null)
          dateStr = "";
        String canonicalizedAmzHeaders = S3V2Authentication.buildCanonicalHeaders(request, false);
        String securityToken = request.getHeader(SecurityParameter.X_Amz_Security_Token.parameter());
        S3V2Authentication.login(request, date, dateStr, canonicalizedAmzHeaders, accessKeyId, signature, securityToken);
      }
    },

    V2_PARAMS {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        String expiresStr = S3V2Authentication.getAndValidateExpiresFromParameters(lowercaseParams);
        String canonicalizedAmzHeaders = S3V2Authentication.buildCanonicalHeaders(request, true);
        String accessKeyId = request.getParameters().remove(SecurityParameter.AWSAccessKeyId.toString());
        String signature = getSignatureFromParameters(lowercaseParams);
        String securityToken = lowercaseParams.get(SecurityParameter.X_Amz_Security_Token.parameter().toLowerCase());
        S3V2Authentication.login(request, null, expiresStr, canonicalizedAmzHeaders, accessKeyId, signature, securityToken);
      }
    },

    V4_HEADER {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        Map<V4AuthComponent, String> authComponents = S3V4Authentication.getV4AuthComponents(request.getHeader(HttpHeaders.Names
            .AUTHORIZATION));
        String dateStr = getDateFromHeaders(request);
        Date date = parseDate(dateStr);
        assertDateNotSkewed(date);
        SignatureCredential credential = S3V4Authentication.getAndVerifyCredential(date, authComponents.get(V4AuthComponent.Credential));
        String signedHeaders = authComponents.get(V4AuthComponent.SignedHeaders);
        String signature = authComponents.get(V4AuthComponent.Signature);
        String securityToken = request.getHeader(SecurityParameter.X_Amz_Security_Token.parameter());
        String payloadHash = S3V4Authentication.getUnverifiedPayloadHash(request);
        Long decodedContentLength = S3V4Authentication.getAndVerifyDecodedContentLength(request, payloadHash);
        S3V4Authentication.login(request, date, credential, signedHeaders, signature, securityToken, payloadHash);

        // Convert content length from V4 to V2
        if (decodedContentLength != null)
          HttpHeaders.setContentLength(request, decodedContentLength);
      }
    },

    V4_PARAMS {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        String dateStr = S3V4Authentication.getDateFromParams(lowercaseParams);
        Date date = parseDate(dateStr);
        S3V4Authentication.validateExpiresFromParams(lowercaseParams, date);
        String credentialStr = lowercaseParams.get(SecurityParameter.X_Amz_Credential.parameter().toLowerCase());
        SignatureCredential credential = S3V4Authentication.getAndVerifyCredential(date, credentialStr);
        String signedHeaders = lowercaseParams.get(SecurityParameter.X_Amz_SignedHeaders.parameter().toLowerCase());
        String signature = lowercaseParams.get(SecurityParameter.X_Amz_Signature.parameter().toLowerCase());
        String securityToken = lowercaseParams.get(SecurityParameter.X_Amz_Security_Token.parameter().toLowerCase());
        S3V4Authentication.login(request, date, credential, signedHeaders, signature, securityToken, S3V4Authentication.UNSIGNED_PAYLOAD);
      }
    },

    ANONYMOUS {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        try {
          final Context context = Contexts.lookup( request.getCorrelationId( ) );
          final Subject subject = new Subject( );
          final UserPrincipal principal = new DelegatingUserPrincipal( Principals.nobodyUser( ) ) {
            @Nonnull
            @Override
            public List<PolicyVersion> getPrincipalPolicies( ) {
              return ImmutableList.of( PolicyVersions.getAdministratorPolicy( ) );
            }
          };
          subject.getPrincipals( ).add( principal );
          context.setUser( principal );
          context.setSubject( subject );
        } catch (NoSuchContextException e) {
          LOG.error(e, e);
          throw new AccessDeniedException();
        }
      }
    };

    /**
     * Authenticates the request.
     *
     * @throws AccessDeniedException          if the auth info is invalid
     * @throws SignatureDoesNotMatchException if the signature is invalid
     * @throws InvalidAccessKeyIdException    if the contextual AWS key is is invalid
     * @throws InternalErrorException         if something unexpected occurs
     */
    public abstract void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception;

    /**
     * Returns the S3Authenticator for the request.
     *
     * @throws MissingSecurityHeaderException if an Authorization header is present, but is invalid
     */
    public static S3Authenticator of(MappingHttpRequest request, Map<String, String> lowercaseParams) throws
        MissingSecurityHeaderException {
      // Handle headers request
      String authHeader = request.getHeader(SecurityParameter.Authorization.toString());
      if (!Strings.isNullOrEmpty(authHeader)) {
        if (authHeader.startsWith(S3V4Authentication.AWS_V4_AUTH_TYPE))
          return S3Authenticator.V4_HEADER;
        else if (authHeader.startsWith(S3V2Authentication.AWS_V2_AUTH_TYPE))
          return S3Authenticator.V2_HEADER;
        else
          throw new MissingSecurityHeaderException(null, "Malformed or unexpected format for Authentication header");
      }

      // Handle param request
      if (lowercaseParams.containsKey(SecurityParameter.X_Amz_Algorithm.parameter().toLowerCase()) || lowercaseParams.containsKey
          (SecurityParameter.X_Amz_Date.parameter().toLowerCase()))
        return S3Authenticator.V4_PARAMS;
      else if (request.getParameters().containsKey(SecurityParameter.AWSAccessKeyId.parameter()))
        return S3Authenticator.V2_PARAMS;

      // Handle anonymous request
      return S3Authenticator.ANONYMOUS;
    }
  }

  public static void authenticateV4Streaming(MappingHttpRequest request, List<AwsChunk> chunks) throws S3Exception {
    Map<V4AuthComponent, String> authComponents = S3V4Authentication.getV4AuthComponents(request.getHeader(HttpHeaders.Names
        .AUTHORIZATION));
    String dateStr = getDateFromHeaders(request);
    Date date = parseDate(dateStr);
    SignatureCredential credential = S3V4Authentication.getAndVerifyCredential(date, authComponents.get(V4AuthComponent.Credential));
    String signedHeaders = authComponents.get(V4AuthComponent.SignedHeaders);
    String securityToken = request.getHeader(SecurityParameter.X_Amz_Security_Token.parameter());
    String seedSignature = authComponents.get(V4AuthComponent.Signature);

    for (AwsChunk chunk : chunks) {
      String previousSignature = chunk.previousSignature == null ? seedSignature : chunk.previousSignature;
      S3V4Authentication.loginChunk(request, date, credential, signedHeaders, chunk.chunkSignature, securityToken, previousSignature,
          chunk.getPayload());
    }
  }

  private static String getDateFromHeaders(MappingHttpRequest request) throws AccessDeniedException {
    String result = request.getHeader(SecurityHeader.X_Amz_Date.header());
    if (result == null)
      result = request.getHeader(SecurityHeader.Date.header());
    if (result == null)
      throw new AccessDeniedException(null, "X-Amz-Date header must be specified.");
    return result;
  }

  static Date parseDate(String dateStr) throws AccessDeniedException {
    Date date = null;

    try {
      date = Timestamps.parseTimestamp(dateStr, Type.ISO_8601);
    } catch (Exception ignore) {
    }

    try {
      if (date == null)
        date = Timestamps.parseTimestamp(dateStr, Type.RFC_2616);
    } catch (Exception ex) {
      LOG.error("Cannot parse date: " + dateStr);
      throw new AccessDeniedException(null, "Unable to parse date.");
    }

    return date;
  }

  static void assertDateNotSkewed(final Date date) throws RequestTimeTooSkewedException {
    DateTime currentTime = DateTime.now();
    DateTime dt = new DateTime(date);
    if (dt.isBefore(currentTime.minusMillis((int) ObjectStorageProperties.EXPIRATION_LIMIT)))
      throw new RequestTimeTooSkewedException();
    if (dt.isAfter(currentTime.plusMillis((int) ObjectStorageProperties.EXPIRATION_LIMIT)))
      throw new RequestTimeTooSkewedException();
  }

  private static String getSignatureFromParameters(Map<String, String> parameters) throws InvalidSecurityException {
    String signature = parameters.remove(SecurityParameter.Signature.toString().toLowerCase());
    if (signature == null)
      throw new InvalidSecurityException("No signature found");
    return signature;
  }
}