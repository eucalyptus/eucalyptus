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

package com.eucalyptus.objectstorage.pipeline.auth;

import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.crypto.util.Timestamps.Type;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.s3.*;
import com.eucalyptus.objectstorage.pipeline.auth.S3V4Authentication.V4AuthComponent;
import com.eucalyptus.objectstorage.pipeline.handlers.AwsChunkStream.AwsChunk;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import com.google.common.base.Strings;
import javaslang.control.Try.CheckedFunction;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import javax.security.auth.login.LoginException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
        parseDateAndAssertNotExpired(dateStr);
        String canonicalizedAmzHeaders = S3V2Authentication.buildCanonicalHeaders(request, false);
        String securityToken = request.getHeader(SecurityParameter.X_Amz_Security_Token.parameter());
        S3V2Authentication.login(request, dateStr, canonicalizedAmzHeaders, accessKeyId, signature, securityToken);
      }
    },

    V2_PARAMS {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        String dateStr = S3V2Authentication.getAndValidateDateFromParameters(lowercaseParams);
        String canonicalizedAmzHeaders = S3V2Authentication.buildCanonicalHeaders(request, true);
        String accessKeyId = request.getParameters().remove(SecurityParameter.AWSAccessKeyId.toString());
        String signature = getSignatureFromParameters(lowercaseParams);
        String securityToken = lowercaseParams.get(SecurityParameter.X_Amz_Security_Token.parameter().toLowerCase());
        S3V2Authentication.login(request, dateStr, canonicalizedAmzHeaders, accessKeyId, signature, securityToken);
      }
    },

    V4_HEADER {
      public void authenticate(MappingHttpRequest request, Map<String, String> lowercaseParams) throws S3Exception {
        Map<V4AuthComponent, String> authComponents = S3V4Authentication.getV4AuthComponents(request.getHeader(HttpHeaders.Names
            .AUTHORIZATION));
        String dateStr = getDateFromHeaders(request);
        Date date = parseDateAndAssertNotExpired(dateStr);
        SignatureCredential credential = S3V4Authentication.getAndVerifyCredential(date, authComponents.get(V4AuthComponent.Credential));
        String signedHeaders = authComponents.get(V4AuthComponent.SignedHeaders);
        String signature = authComponents.get(V4AuthComponent.Signature);
        String securityToken = request.getHeader(SecurityParameter.X_Amz_Security_Token.parameter());
        String payloadHash = S3V4Authentication.buildAndVerifyPayloadHash(request);
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
        Date date = parseDateAndAssertNotExpired(dateStr);
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
          Context ctx = Contexts.lookup(request.getCorrelationId());
          ctx.setUser(Principals.nobodyUser());
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
          throw new MissingSecurityHeaderException("Malformed or unexpected format for Authentication header");
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
    Date date = parseDateAndAssertNotExpired(dateStr);
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

  /**
   * Attempts a login and retries sign a signed string that does not contain a path if the initial attempt fails.
   */
  static void login(MappingHttpRequest request, String accessKeyId, CheckedFunction<Boolean, ObjectStorageWrappedCredentials> credsFn)
      throws S3Exception {
    // Build credentials that excludes path
    ObjectStorageWrappedCredentials creds = credentialsFor(credsFn, false);

    try {
      SecurityContext.getLoginContext(creds).login();
    } catch (LoginException ex) {
      if (ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records"))
        throw new InvalidAccessKeyIdException(accessKeyId);

      if (request.getUri().startsWith(ComponentIds.lookup(ObjectStorage.class).getServicePath()) || request.getUri().startsWith
          (ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
        try {
          // Build credentials for a string to sign that skips the resource path
          creds = credentialsFor(credsFn, true);
          SecurityContext.getLoginContext(creds).login();
        } catch (S3Exception ex2) {
          LOG.debug("CorrelationId: " + request.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
          throw ex2;
        } catch (Exception ex2) {
          LOG.debug("CorrelationId: " + request.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
          throw new SignatureDoesNotMatchException(creds.getLoginData());
        }
      } else {
        LOG.debug("CorrelationId: " + request.getCorrelationId() + " Authentication failed due to signature mismatch:", ex);
        throw new SignatureDoesNotMatchException(creds.getLoginData());
      }
    } catch (Exception e) {
      LOG.warn("CorrelationId: " + request.getCorrelationId() + " Unexpected failure trying to authenticateVersion2 request", e);
      throw new InternalErrorException(e);
    }
  }

  private static ObjectStorageWrappedCredentials credentialsFor(CheckedFunction<Boolean, ObjectStorageWrappedCredentials> credsFn,
                                                                boolean excludePath) throws S3Exception {
    try {
      return credsFn.apply(excludePath);
    } catch (Throwable t) {
      if (t instanceof S3Exception)
        throw (S3Exception) t;
      throw new InternalErrorException(t);
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

  static Date parseDateAndAssertNotExpired(String dateStr) throws AccessDeniedException {
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

    Date currentDate = new Date();
    if (Math.abs(currentDate.getTime() - date.getTime()) > ObjectStorageProperties.EXPIRATION_LIMIT)
      throw new AccessDeniedException(null, "Cannot process request. Expired.");
    return date;
  }

  private static String getSignatureFromParameters(Map<String, String> parameters) throws InvalidSecurityException {
    String signature = parameters.remove(SecurityParameter.Signature.toString().toLowerCase());
    if (signature == null)
      throw new InvalidSecurityException("No signature found");
    return signature;
  }

  static String urlEncode(String url, boolean keepPathSlash) {
    String encoded;
    try {
      encoded = URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 encoding is not supported.", e);
    }
    if (keepPathSlash)
      encoded = encoded.replace("%2F", "/");
    return encoded;
  }
}