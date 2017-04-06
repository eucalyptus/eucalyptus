/*************************************************************************
 * Copyright 2016 Hewlett-Packard Enterprise, Inc.
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

import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.login.HmacLoginModuleSupport;
import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.*;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.SubResource;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.security.auth.login.LoginException;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eucalyptus.auth.login.Hmacv4LoginModule.digestUTF8;

/**
 * S3 V4 specific authentication utilities.
 */
public final class S3V4Authentication {
  private static final Logger LOG = Logger.getLogger(S3V4Authentication.class);
  private static final Splitter CSV_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
  private static final Splitter NVP_SPLITTER = Splitter.on('=').limit(2).trimResults().omitEmptyStrings();
  private static final String AWS_V4_TERMINATOR = "aws4_request";
  public static final String AWS_V4_AUTH_TYPE = "AWS4-HMAC-SHA256";
  public static final String AWS_CONTENT_SHA_HEADER = "x-amz-content-sha256";
  public static final String AWS_EXPIRES_PARAM = "x-amz-expires";
  public static final String AWS_DECODED_CONTENT_LEN = "x-amz-decoded-content-length";
  public static final String STREAMING_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
  private static final String STREAMING_PAYLOAD_CHUNK_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";
  public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

  enum V4AuthComponent {
    Credential, SignedHeaders, Signature
  }

  private S3V4Authentication() {
  }

  static void login(MappingHttpRequest request, Date date, SignatureCredential credential, String signedHeaders, String signature, String
      securityToken, String payloadHash) throws S3Exception {
    String stringToSign = buildStringToSign(request, date, credential, signedHeaders, payloadHash);
    ObjectStorageWrappedCredentials creds = new ObjectStorageWrappedCredentials(request.getCorrelationId(),
        date==null?null:date.getTime( ), stringToSign, credential, signedHeaders, signature, securityToken, payloadHash);
    login(request, credential.getAccessKeyId(), creds);
  }

  static void loginChunk(MappingHttpRequest request, Date date, SignatureCredential credential, String signedHeaders, String signature,
                         String securityToken, String previousSignature, ByteBuffer payload) throws S3Exception {
    String stringToSign = buildChunkStringToSign(date, credential, previousSignature, payload);
    ObjectStorageWrappedCredentials creds = new ObjectStorageWrappedCredentials(request.getCorrelationId(),
        date==null?null:date.getTime( ), stringToSign, credential, signedHeaders, signature, securityToken, null);
    login(request, credential.getAccessKeyId(), creds);
  }

  /**
   * Attempts a login and retries sign a signed string that does not contain a path if the initial attempt fails.
   */
  static void login(MappingHttpRequest request, String accessKeyId, ObjectStorageWrappedCredentials creds) throws S3Exception {
    try {
      SecurityContext.getLoginContext(creds).login();
    } catch (LoginException ex) {
      if (ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records"))
        throw new InvalidAccessKeyIdException(accessKeyId);
      LOG.debug("CorrelationId: " + request.getCorrelationId() + " Authentication failed due to signature mismatch:", ex);
      StringBuilder canonicalRequest = buildCanonicalRequest(request, creds.signedHeaders, creds.payloadHash);
      throw new SignatureDoesNotMatchException(creds.accessKeyId, creds.getLoginData(), creds.signature, canonicalRequest.toString());
    } catch (Exception e) {
      LOG.warn("CorrelationId: " + request.getCorrelationId() + " Unexpected failure trying to authenticate request", e);
      throw new InternalErrorException(e);
    }
  }

  /**
   * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">Creating an S3 v4 string to sign</a>
   */
  private static String buildStringToSign(MappingHttpRequest request, Date date, SignatureCredential credential, String signedHeaders,
                                          String payloadHash) throws S3Exception {
    try {
      StringBuilder canonicalRequest = buildCanonicalRequest(request, signedHeaders, payloadHash);
      return buildStringToSign(date, credential, canonicalRequest);
    } catch (Exception e) {
      throw new InternalErrorException(e);
    }
  }

  private static String buildStringToSign(Date date, SignatureCredential credential, CharSequence canonicalRequest) throws Exception {
    StringBuilder sb = new StringBuilder(256);
    sb.append(SecurityHeader.Value.AWS4_HMAC_SHA256.value()).append('\n');
    sb.append(Timestamps.formatShortIso8601Timestamp(date)).append('\n');
    sb.append(credential.getCredentialScope()).append('\n');
    sb.append(digestUTF8(canonicalRequest));
    return sb.toString();
  }

  private static String buildChunkStringToSign(Date date, SignatureCredential credential, String previousSignature, ByteBuffer payload) {
    StringBuilder sb = new StringBuilder(256);
    sb.append(STREAMING_PAYLOAD_CHUNK_PREFIX).append('\n');
    sb.append(Timestamps.formatShortIso8601Timestamp(date)).append('\n');
    sb.append(credential.getCredentialScope()).append('\n');
    sb.append(previousSignature).append('\n');
    sb.append(digestUTF8("")).append('\n');
    sb.append(BaseEncoding.base16().lowerCase().encode(Digest.SHA256.digestBinary(payload)));
    return sb.toString();
  }

  /**
   * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">Creating a canonical S3 v4 request</a>
   */
  static StringBuilder buildCanonicalRequest(MappingHttpRequest request, String signedHeaders, String payloadHash) {
    StringBuilder sb = new StringBuilder(512);

    // Request method
    sb.append(request.getMethod().getName());
    sb.append('\n');

    // Resource path
    sb.append(buildCanonicalResourcePath(request.getServicePath()));
    sb.append('\n');

    // Query parameters
    buildCanonicalQueryString(request.getParameters(), sb);
    sb.append('\n');

    // Headers
    buildCanonicalHeaders(request, signedHeaders, sb);
    sb.append('\n');

    // Signed headers
    sb.append(signedHeaders);
    sb.append('\n');

    // Payload
    if (payloadHash != null)
      sb.append(payloadHash);
    return sb;
  }

  /**
   * Returns the canonicalized resource path for the service endpoint.
   */
  static String buildCanonicalResourcePath(String path) {
    if (path == null || path.isEmpty())
      return "/";

    if (path.startsWith("/"))
      return path;
    else
      return "/".concat(path);
  }

  static void buildCanonicalQueryString(Map<String,String> parameters, StringBuilder sb) {
    boolean firstParam = true;
    for (String parameter : Ordering.natural().sortedCopy(parameters.keySet())) {
      // Ignore signature parameters
      if (SecurityParameter.X_Amz_Signature.parameter().equals(parameter))
        continue;

      if (!firstParam)
        sb.append('&');
      String value = parameters.get(parameter);
      sb.append(HmacLoginModuleSupport.urlencode(parameter));
      sb.append('=');

      if (!Strings.isNullOrEmpty(value)) {
        Optional<SubResource> subResource = Enums.getIfPresent(SubResource.class, parameter);
        if (subResource.isPresent() && subResource.get().isObjectSubResource)
          sb.append("");
        else
          sb.append(HmacLoginModuleSupport.urlencode(value));
      }

      firstParam = false;
    }
  }

  static void buildCanonicalHeaders(MappingHttpRequest request, String signedHeaders, StringBuilder sb) {
    for (String header : signedHeaders.split(";")) {
      List<String> values = Lists.transform(request.getHeaders(header), text -> text != null ? text.trim() : null);
      sb.append(header.toLowerCase());
      sb.append(':');
      sb.append(Joiner.on(',').join(Ordering.<String>natural().sortedCopy(values)));
      sb.append('\n');
    }
  }

  static String getUnverifiedPayloadHash( final MappingHttpRequest request) throws AccessDeniedException {
    final String contentShaHeader = request.getHeader(S3V4Authentication.AWS_CONTENT_SHA_HEADER);
    if ( !Strings.isNullOrEmpty(contentShaHeader) ) {
      if ( !STREAMING_PAYLOAD.equals(contentShaHeader) && !UNSIGNED_PAYLOAD.equals(contentShaHeader) ) {
        final byte[] binSha256 = BaseEncoding.base16( ).lowerCase( ).decode( contentShaHeader );
        if ( binSha256.length != 32 ) {
          throw new AccessDeniedException(null, "x-amz-content-sha256 header is invalid.");
        }
      }
    } else {
      throw new AccessDeniedException( null, "x-amz-content-sha256 header is missing." );
    }
    return contentShaHeader;
  }

  static String getDateFromParams(Map<String, String> parameters) throws AccessDeniedException {
    String result = parameters.get(SecurityHeader.X_Amz_Date.header().toLowerCase());
    if (result == null)
      throw new AccessDeniedException(null, "X-Amz-Date parameter must be specified.");
    return result;
  }

  static void validateExpiresFromParams(Map<String, String> parameters, Date date) throws AccessDeniedException {
    String expires = parameters.get(AWS_EXPIRES_PARAM);
    if (expires == null)
      throw new AccessDeniedException(null, "X-Amz-Expires parameter must be specified.");
    Long expireTime;
    try {
      expireTime = Long.parseLong(expires);
    } catch (NumberFormatException e) {
      throw new AccessDeniedException(null, "Invalid X-Amz-Expires parameter.");
    }

    if (expireTime < 1 || expireTime > 604800)
      throw new AccessDeniedException(null, "Invalid Expires parameter.");

    DateTime currentTime = DateTime.now();
    DateTime dt = new DateTime(date);
    if (currentTime.isBefore(dt.minusMillis((int) ObjectStorageProperties.EXPIRATION_LIMIT)))
      throw new AccessDeniedException(null, "Cannot process request. X-Amz-Date is not yet valid.");
    if (currentTime.isAfter(dt.plusSeconds(expireTime.intValue() + StackConfiguration.CLOCK_SKEW_SEC)))
      throw new AccessDeniedException(null, "Cannot process request. Expired.");
  }

  static Long getAndVerifyDecodedContentLength(MappingHttpRequest request, String contentSha) throws S3Exception {
    if (!STREAMING_PAYLOAD.equals(contentSha))
      return null;
    String decodedContentLength = request.getHeader(AWS_DECODED_CONTENT_LEN);
    if (Strings.isNullOrEmpty(decodedContentLength))
      throw new MissingContentLengthException(null, "Missing x-amz-decoded-content-length header");
    try {
      return Long.valueOf(decodedContentLength);
    } catch (NumberFormatException e) {
      throw new MissingContentLengthException(null, "Invalid x-amz-decoded-content-length header");
    }
  }

  static SignatureCredential getAndVerifyCredential(Date date, String credentialStr) throws AccessDeniedException {
    try {
      SignatureCredential credential = new SignatureCredential(credentialStr);
      credential.verify(date, null, null, AWS_V4_TERMINATOR);
      return credential;
    } catch (AuthenticationException e) {
      throw new AccessDeniedException(null, "Credential header is invalid.");
    }
  }

  /**
   * Returns the auth components for the sigv4 request's Authentication header.
   *
   * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-auth-using-authorization-header.html">S3 Docs</a>
   */
  static Map<V4AuthComponent, String> getV4AuthComponents(String authHeader) {
    authHeader = authHeader.replaceFirst(AWS_V4_AUTH_TYPE, "").trim();
    Iterable<String> authParts = CSV_SPLITTER.split(authHeader);
    Map<V4AuthComponent, String> authParams = new HashMap<>();
    for (String nvp : authParts) {
      Iterable<String> nameAndValue = NVP_SPLITTER.split(nvp);
      try {
        V4AuthComponent name = V4AuthComponent.valueOf(Iterables.get(nameAndValue, 0, ""));
        String value = Iterables.get(nameAndValue, 1, "");
        if (value != null && !value.isEmpty())
          authParams.put(name, value);
      } catch (IllegalArgumentException ignore) {
      }
    }

    return authParams;
  }
}