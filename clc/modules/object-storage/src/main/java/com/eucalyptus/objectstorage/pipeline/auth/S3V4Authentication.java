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
import com.eucalyptus.auth.login.Hmacv4LoginModule;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.ws.util.HmacUtils.SignatureCredential;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eucalyptus.auth.login.Hmacv4LoginModule.digestUTF8;

/**
 * S3 V4 specific authentication utilities.
 */
public final class S3V4Authentication {
  private static final Splitter CSV_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();
  private static final Splitter NVP_SPLITTER = Splitter.on('=').limit(2).trimResults().omitEmptyStrings();
  private static final String AWS_V4_TERMINATOR = "aws4_request";
  public static final String AWS_V4_AUTH_TYPE = "AWS4-HMAC-SHA256";
  static final String CONTENT_SHA_HEADER = "x-amz-content-sha256";
  static final String STREAMING_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
  static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

  enum V4AuthComponent {
    Credential, SignedHeaders, Signature
  }

  private S3V4Authentication() {
  }

  static void login(MappingHttpRequest request, Date date, SignatureCredential credential, String signedHeaders, String signature, String
      securityToken, String payloadHash) throws S3Exception {
    S3Authentication.login(request, credential.getAccessKeyId(), excludePath -> {
      String stringToSign = buildStringToSign(request, date, credential, signedHeaders, payloadHash, excludePath);
      return new ObjectStorageWrappedCredentials(request.getCorrelationId(), stringToSign, credential, signedHeaders, signature,
          securityToken);
    });
  }

  /**
   * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">Creating an S3 v4 string to sign</a>
   */
  private static String buildStringToSign(MappingHttpRequest request, Date date, SignatureCredential credential, String signedHeaders,
                                          String payloadHash, boolean excludePath) throws S3Exception {
    try {
      StringBuilder canonicalRequest = buildCanonicalRequest(request, signedHeaders, payloadHash, excludePath);
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

  /**
   * @see <a href="http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html">Creating a canonical S3 v4 request</a>
   */
  static StringBuilder buildCanonicalRequest(MappingHttpRequest request, String signedHeaders, String payloadHash, boolean excludePath) {
    StringBuilder sb = new StringBuilder(512);

    // Request method
    sb.append(request.getMethod().getName());
    sb.append('\n');

    // Resource path
    sb.append(buildCanonicalResourcePath(request.getServicePath(), excludePath));
    sb.append('\n');

    // Query parameters
    buildCanonicalQueryString(request, sb);
    sb.append('\n');

    // Headers
    buildCanonicalHeaders(request, signedHeaders, sb);
    sb.append('\n');

    // Signed headers
    sb.append(signedHeaders);
    sb.append('\n');

    // Payload
    sb.append(payloadHash);
    return sb;
  }

  /**
   * Returns the canonicalized resource path for the service endpoint.
   */
  static String buildCanonicalResourcePath(String path, boolean excludePath) {
    if (path == null || path.isEmpty() || excludePath)
      return "/";

    String encodedPath = S3Authentication.urlEncode(path, true);
    if (encodedPath.startsWith("/"))
      return encodedPath;
    else
      return "/".concat(encodedPath);
  }

  static void buildCanonicalQueryString(MappingHttpRequest request, StringBuilder sb) {
    boolean firstParam = true;
    for (String parameter : Ordering.natural().sortedCopy(request.getParameters().keySet())) {
      // Ignore signature parameters
      if (SecurityParameter.X_Amz_Signature.parameter().equals(parameter))
        continue;

      if (!firstParam)
        sb.append('&');
      String value = request.getParameters().get(parameter);
      sb.append(S3Authentication.urlEncode(parameter, false));
      sb.append('=');

      try {
        // Sub-resource values are appended as ""
        ObjectStorageProperties.SubResource.valueOf(parameter);
        sb.append("");
      } catch (IllegalArgumentException ignore) {
        sb.append(S3Authentication.urlEncode(value, false));
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

  static String buildAndVerifyPayloadHash(MappingHttpRequest request) throws AccessDeniedException {
    String contentShaHeader = request.getHeader(S3V4Authentication.CONTENT_SHA_HEADER);
    if (STREAMING_PAYLOAD.equals(contentShaHeader))
      return STREAMING_PAYLOAD;
    else if (UNSIGNED_PAYLOAD.equals(contentShaHeader))
      return UNSIGNED_PAYLOAD;
    else if (!Strings.isNullOrEmpty(contentShaHeader)) {
      String hashedPayload = Hmacv4LoginModule.digestUTF8(request.getContentAsString());
      if (!contentShaHeader.equals(hashedPayload))
        throw new AccessDeniedException("x-amz-content-sha256 header is invalid.");
      return hashedPayload;
    } else
      throw new AccessDeniedException("x-amz-content-sha256 header is missing.");
  }

  static SignatureCredential getAndVerifyCredential(Date date, String credentialStr) throws AccessDeniedException {
    try {
      SignatureCredential credential = new SignatureCredential(credentialStr);
      credential.verify(date, null, null, AWS_V4_TERMINATOR);
      return credential;
    } catch (AuthenticationException e) {
      throw new AccessDeniedException("Credential header is invalid.");
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