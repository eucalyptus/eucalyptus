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

package com.eucalyptus.objectstorage.pipeline.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.security.auth.login.LoginException;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.xbill.DNS.Name;

import com.eucalyptus.auth.login.SecurityContext;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAccessKeyIdException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAddressingHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidSecurityException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.exceptions.s3.SignatureDoesNotMatchException;
import com.eucalyptus.objectstorage.pipeline.auth.ObjectStorageWrappedCredentials;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * Primary implementation of S3 authentication. Both REST and Query.
 */
public class S3Authentication {
  private static final Logger LOG = Logger.getLogger(S3Authentication.class);
  private static final Splitter hostSplitter = Splitter.on(':').limit(2);

  public static enum SecurityParameter {
    AWSAccessKeyId, Timestamp, Expires, Signature, Authorization, Date, Content_MD5, Content_Type, SecurityToken,
  }

  /* The possible fields in an authorization header */
  protected static enum AuthorizationField {
    Type, AccessKeyId, Signature
  }

  /**
   * Authenticate using S3-spec REST authentication
   *
   * @param httpRequest
   * @param authMap
   * @throws com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
   */

  static void authenticateVersion2(MappingHttpRequest httpRequest, Map<AuthorizationField, String> authMap) throws S3Exception {
    // Standard S3 authentication signed by SecretKeyID
    String verb = httpRequest.getMethod().getName();
    String date = getDate(httpRequest);
    String addrString = getS3AddressString(httpRequest, true);
    String content_md5 = httpRequest.getHeader("Content-MD5");
    content_md5 = content_md5 == null ? "" : content_md5;
    String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    content_type = content_type == null ? "" : content_type;
    String securityToken = httpRequest.getHeader(ObjectStorageProperties.X_AMZ_SECURITY_TOKEN);
    String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest, false);
    String data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + addrString;
    String accessKeyId = authMap.get(AuthorizationField.AccessKeyId);
    String signature = authMap.get(AuthorizationField.Signature);

    try {
      SecurityContext.getLoginContext(
          new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
    } catch (LoginException ex) {
      if (ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records")) {
        throw new InvalidAccessKeyIdException(accessKeyId);
      }

      // Try using the '/services/ObjectStorage' portion of the addrString and retry the signature calc
      if (httpRequest.getUri().startsWith(ComponentIds.lookup(ObjectStorage.class).getServicePath())
          || httpRequest.getUri().startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
        try {
          String modifiedAddrString = getS3AddressString(httpRequest, false);
          data = verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + modifiedAddrString;
          SecurityContext.getLoginContext(
              new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), data, accessKeyId, signature, securityToken)).login();
        } catch (S3Exception ex2) {
          LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
          throw ex2;
        } catch (Exception ex2) {
          LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature match issue:", ex2);
          throw new SignatureDoesNotMatchException(data);
        }
      } else {
        LOG.debug("CorrelationId: " + httpRequest.getCorrelationId() + " Authentication failed due to signature mismatch:", ex);
        throw new SignatureDoesNotMatchException(data);
      }
    } catch (Exception e) {
      LOG.warn("CorrelationId: " + httpRequest.getCorrelationId() + " Unexpected failure trying to authenticateVersion2 request", e);
      throw new InternalErrorException(e);
    }
  }

  static String buildStringToSignFromHeaders(MappingHttpRequest httpRequest) throws Exception {
    // Standard S3 authentication signed by SecretKeyID
    String verb = httpRequest.getMethod().getName();
    String date = getDate(httpRequest);
    String addrString = getS3AddressString(httpRequest, true);
    String content_md5 = httpRequest.getHeader("Content-MD5");
    content_md5 = content_md5 == null ? "" : content_md5;
    String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    content_type = content_type == null ? "" : content_type;
    String securityToken = httpRequest.getHeader(ObjectStorageProperties.X_AMZ_SECURITY_TOKEN);
    String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest, false);
    return verb + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n" + canonicalizedAmzHeaders + addrString;
  }

  static String buildStringToSignFromQueryParams(MappingHttpRequest httpRequest) throws Exception {
    // Standard S3 query string authentication
    Map<String, String> parameters = httpRequest.getParameters();
    String verb = httpRequest.getMethod().getName();
    String content_md5 = httpRequest.getHeader("Content-MD5");
    content_md5 = content_md5 == null ? "" : content_md5;
    String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    content_type = content_type == null ? "" : content_type;
    String addrString = getS3AddressString(httpRequest, true);
    String accesskeyid = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());

    String expires = parameters.remove(SecurityParameter.Expires.toString());
    if (expires == null) {
      throw new InvalidSecurityException("Expiration parameter must be specified.");
    }
    String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest, true);
    return verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + canonicalizedAmzHeaders + addrString;
  }

  /**
   * Authenticate using S3-spec query string authentication
   *
   * @param httpRequest
   * @throws com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
   */
  static void authenticateQueryString(MappingHttpRequest httpRequest) throws S3Exception {
    // Standard S3 query string authentication
    Map<String, String> parameters = httpRequest.getParameters();
    String verb = httpRequest.getMethod().getName();
    String content_md5 = httpRequest.getHeader("Content-MD5");
    content_md5 = content_md5 == null ? "" : content_md5;
    String content_type = httpRequest.getHeader(HttpHeaders.Names.CONTENT_TYPE);
    content_type = content_type == null ? "" : content_type;
    String addrString = getS3AddressString(httpRequest, true);
    String accessKeyId = parameters.remove(SecurityParameter.AWSAccessKeyId.toString());

    try {
      // Parameter url decode happens during MappingHttpRequest construction.
      String signature = parameters.remove(SecurityParameter.Signature.toString());
      if (signature == null) {
        throw new InvalidSecurityException("No signature found");
      }
      String expires = parameters.remove(SecurityParameter.Expires.toString());
      if (expires == null) {
        throw new InvalidSecurityException("Expiration parameter must be specified.");
      }
      String securityToken = parameters.get(ObjectStorageProperties.X_AMZ_SECURITY_TOKEN);

      if (checkExpires(expires)) {
        String canonicalizedAmzHeaders = getCanonicalizedAmzHeaders(httpRequest, true);
        String stringToSign =
            verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + canonicalizedAmzHeaders + addrString;
        try {
          SecurityContext.getLoginContext(
              new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accessKeyId, signature, securityToken)).login();
        } catch (Exception ex) {
          // Try adding back the '/services/objectStorage' portion of the addrString and retry the signature calc
          if (httpRequest.getUri().startsWith(ComponentIds.lookup(ObjectStorage.class).getServicePath())
              || httpRequest.getUri().startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
            try {
              String modifiedAddrString = getS3AddressString(httpRequest, false);
              stringToSign =
                  verb + "\n" + content_md5 + "\n" + content_type + "\n" + Long.parseLong(expires) + "\n" + canonicalizedAmzHeaders
                      + modifiedAddrString;
              SecurityContext.getLoginContext(
                  new ObjectStorageWrappedCredentials(httpRequest.getCorrelationId(), stringToSign, accessKeyId, signature, securityToken)).login();
            } catch (Exception ex2) {
              LOG.error("CorrelationId: " + httpRequest.getCorrelationId() + " authentication failed due to signature mismatch:", ex2);
              throw new SignatureDoesNotMatchException(stringToSign);
            }
          } else {
            LOG.error("CorrelationId: " + httpRequest.getCorrelationId() + " authentication failed due to signature mismatch:", ex);
            throw new SignatureDoesNotMatchException(stringToSign);
          }
        }
      } else {
        throw new AccessDeniedException("Cannot process request. Expired.");
      }
    } catch (Exception ex) {
      throw new AccessDeniedException("Could not verify request " + ex.getMessage());
    }
  }

  /**
   * See if the expires string indicates the message is expired.
   *
   * @param expires
   * @return
   */
  static boolean checkExpires(String expires) {
    Long expireTime = Long.parseLong(expires);
    Long currentTime = new Date().getTime() / 1000;
    if (currentTime > expireTime)
      return false;
    return true;
  }

  /**
   * Gets the date for S3-spec authentication
   *
   * @param httpRequest
   * @return
   * @throws com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
   */
  static String getDate(MappingHttpRequest httpRequest) throws AccessDeniedException {
    String date;
    String verifyDate;
    if (httpRequest.containsHeader("x-amz-date")) {
      date = "";
      verifyDate = httpRequest.getHeader("x-amz-date");
    } else {
      date = httpRequest.getAndRemoveHeader(SecurityParameter.Date.toString());
      verifyDate = date;
      if (date == null || date.length() <= 0)
        throw new AccessDeniedException("User authentication failed. Date must be specified.");
    }

    try {
      Date dateToVerify = DateUtil.parseDate(verifyDate);
      Date currentDate = new Date();
      if (Math.abs(currentDate.getTime() - dateToVerify.getTime()) > ObjectStorageProperties.EXPIRATION_LIMIT) {
        LOG.error("Incoming ObjectStorage message is expired. Current date: " + currentDate.toString() + " Message's Verification Date: "
            + dateToVerify.toString());
        throw new AccessDeniedException("Message expired. Sorry.");
      }
    } catch (Exception ex) {
      LOG.error("Cannot parse date: " + verifyDate);
      throw new AccessDeniedException("Unable to parse date.");
    }

    return date;
  }

  private static void processHeaderValue(String name, String value, Map<String, String> aggregatingMap) {
    String headerNameString = name.toLowerCase().trim();
    if (headerNameString.startsWith("x-amz-")) {
      value = value.trim();
      String[] parts = value.split("\n");
      value = "";
      for (String part : parts) {
        part = part.trim();
        value += part + " ";
      }
      value = value.trim();
      if (aggregatingMap.containsKey(headerNameString)) {
        String oldValue = (String) aggregatingMap.remove(headerNameString);
        oldValue += "," + value;
        aggregatingMap.put(headerNameString, oldValue);
      } else {
        aggregatingMap.put(headerNameString, value);
      }
    }
  }

  /**
   * Query params are included in cases of Query-String/Presigned-url auth where they are considered just like headers
   * 
   * @param httpRequest
   * @param includeQueryParams
   * @return
   */
  private static String getCanonicalizedAmzHeaders(MappingHttpRequest httpRequest, boolean includeQueryParams) {
    String result = "";
    Set<String> headerNames = httpRequest.getHeaderNames();
    TreeMap<String, String> amzHeaders = new TreeMap<String, String>();
    for (String headerName : headerNames) {
      String headerNameString = headerName.toLowerCase().trim();
      if (headerNameString.startsWith("x-amz-")) {
        String value = httpRequest.getHeader(headerName).trim();
        String[] parts = value.split("\n");
        value = "";
        for (String part : parts) {
          part = part.trim();
          value += part + " ";
        }
        value = value.trim();
        if (amzHeaders.containsKey(headerNameString)) {
          String oldValue = (String) amzHeaders.remove(headerNameString);
          oldValue += "," + value;
          amzHeaders.put(headerNameString, oldValue);
        } else {
          amzHeaders.put(headerNameString, value);
        }
      }
    }

    if (includeQueryParams) {
      // For query-string auth, header values may include 'x-amz-*' that need to be signed
      for (String paramName : httpRequest.getParameters().keySet()) {
        processHeaderValue(paramName, httpRequest.getParameters().get(paramName), amzHeaders);
      }
    }

    // Build the canonical string
    Iterator<String> iterator = amzHeaders.keySet().iterator();
    while (iterator.hasNext()) {
      String key = iterator.next();
      String value = (String) amzHeaders.get(key);
      result += key + ":" + value + "\n";
    }
    return result;
  }

  // Old method for getting signature info from Auth header
  static String[] getSigInfo(String auth_part) {
    int index = auth_part.lastIndexOf(" ");
    String sigString = auth_part.substring(index + 1);
    return sigString.split(":");
  }

  /**
   * AWS S3-spec address string, which includes the query parameters
   *
   * @param httpRequest
   * @param removeServicePath if true, removes the service path from the address string if found and if the request is path-style
   * @return
   * @throws com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException
   */
  static String getS3AddressString(MappingHttpRequest httpRequest, boolean removeServicePath) throws S3Exception {
    /*
     * There are two modes: dns-style and path-style. dns-style has the bucket name in the HOST header path-style has the bucket name in the request
     * path.
     * 
     * If using DNS-style, we assume the key is the path, no service path necessary or allowed If using path-style, there may be service path as well
     * that prefixes the bucket name (e.g. /services/objectstorage/bucket/key)
     */
    try {
      String addr = httpRequest.getUri();
      String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
      String osgServicePath = ComponentIds.lookup(ObjectStorage.class).getServicePath();
      String bucket, key;

      StringBuilder addrString = new StringBuilder();

      // Normalize the URI
      boolean foundName = false;
      if ( !Strings.isNullOrEmpty( targetHost ) ) {
        final String host = Iterables.getFirst( hostSplitter.split( targetHost ), targetHost );
        final Name hostDnsName = DomainNames.absolute( Name.fromString( host ) );
        final Optional<Name> systemDomain = DomainNames.systemDomainFor( ObjectStorage.class, hostDnsName );
        if ( systemDomain.isPresent( ) ) {
          foundName = true;
          // dns-style request
          final String hostBucket = hostDnsName.relativize( systemDomain.get( ) ).toString( );
          if ( hostBucket.length( ) == 0 ) {
            throw new InvalidAddressingHeaderException( "Invalid Host header: " + targetHost );
          } else {
            addrString.append("/" + hostBucket);
          }
        }
      }
      if ( !foundName ) {
        // path-style request (or service request that won't have a bucket anyway)
        if (removeServicePath) {
          if (addr.startsWith(osgServicePath)) {
            addr = addr.substring(osgServicePath.length(), addr.length());
          } else if (addr.startsWith(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH)) {
            addr = addr.substring(ObjectStorageProperties.LEGACY_WALRUS_SERVICE_PATH.length(), addr.length());
          }
        }
      }

      // Get the path part, up to the ?
      key = addr.split("\\?", 2)[0];
      if (!Strings.isNullOrEmpty(key)) {
        addrString.append(key);
      } else {
        addrString.append("/");
      }

      List<String> canonicalSubresources = new ArrayList<>();
      for (String queryParam : httpRequest.getParameters().keySet()) {
        try {
          if (ObjectStorageProperties.SubResource.valueOf(queryParam) != null) {
            canonicalSubresources.add(queryParam);
          }
        } catch (IllegalArgumentException e) {
          // Skip. Not in the set.
        }
        try {
          if (ObjectStorageProperties.ResponseHeaderOverrides.fromString(queryParam) != null) {
            canonicalSubresources.add(queryParam);
          }
        } catch (IllegalArgumentException e) {
          // Skip. Not in the set.
        }
      }

      if (canonicalSubresources.size() > 0) {
        Collections.sort(canonicalSubresources);
        String value;
        addrString.append("?");
        // Add resources to canonical string
        for (String subResource : canonicalSubresources) {
          value = httpRequest.getParameters().get(subResource);
          addrString.append(subResource);
          // Query values are not URL-decoded, the signature should have them exactly as in the URI
          if (!Strings.isNullOrEmpty(value)) {
            addrString.append("=").append(value);
          }
          addrString.append("&");
        }

        // Remove trailng '&' if found
        if (addrString.charAt(addrString.length() - 1) == '&') {
          addrString.deleteCharAt(addrString.length() - 1);
        }
      }

      return addrString.toString();
    } catch (S3Exception e) {
      throw e;
    } catch (Exception e) {
      // Anything unexpected...
      throw new InternalErrorException(e);
    }
  }

} // End class S3Authentication
