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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAddressingHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3ExtendedException;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageErrorMessageExtendedType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageErrorMessageType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.storage.msgs.s3.CorsHeader;
import com.eucalyptus.storage.msgs.s3.CorsMatchResult;
import com.eucalyptus.storage.msgs.s3.CorsRule;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class OSGUtil {
  private static final Splitter hostSplitter = Splitter.on(':').limit(2);

  public static BaseMessage convertErrorMessage(ExceptionResponseType errorMessage) {
    Throwable ex = errorMessage.getException();
    String correlationId = errorMessage.getCorrelationId();
    BaseMessage errMsg = null;
    if ((errMsg = convertException(correlationId, ex)) == null) {
      errMsg = errorMessage;
    }
    return errMsg;
  }

  public static BaseMessage convertErrorMessage(EucalyptusErrorMessageType errorMessage) {
    Throwable ex = errorMessage.getException();
    String correlationId = errorMessage.getCorrelationId();
    BaseMessage errMsg = null;
    if ((errMsg = convertException(correlationId, ex)) == null) {
      errMsg = errorMessage;
    }
    return errMsg;
  }

  private static BaseMessage convertException(String correlationId, Throwable ex) {
    BaseMessage errMsg;
    if (ex instanceof S3ExtendedException){
      S3ExtendedException e = (S3ExtendedException) ex;
      errMsg =
          new ObjectStorageErrorMessageExtendedType(e.getMessage(), e.getCode(), e.getStatus(), e.getResourceType(), e.getResource(), correlationId,
              Internets.localHostAddress(), e.getLogData(), e.getRequestMethod());
    } else if (ex instanceof ObjectStorageException) {
      ObjectStorageException e = (ObjectStorageException) ex;
      errMsg =
          new ObjectStorageErrorMessageType(e.getMessage(), e.getCode(), e.getStatus(), e.getResourceType(), e.getResource(), correlationId,
              Internets.localHostAddress(), e.getLogData());
    } else {
      return null;
    }
    errMsg.setCorrelationId(correlationId);
    return errMsg;
  }

  public static String URLdecode(String objectKey) throws UnsupportedEncodingException {
    return URLDecoder.decode(objectKey, "UTF-8");
  }

  public static String[] getTarget(String operationPath) {
    operationPath = operationPath.replaceAll("^/{2,}", "/"); // If its in the form "/////bucket/key", change it to "/bucket/key"
    if (operationPath.startsWith("/")) { // If its in the form "/bucket/key", change it to "bucket/key"
      operationPath = operationPath.substring(1);
    }
    String[] parts = operationPath.split("/", 2); // Split into a maximum of two parts [bucket, key]
    if (parts != null) {
      if (parts.length == 1 && Strings.isNullOrEmpty(parts[0])) { // Splitting empty string will lead one part, check if the part is empty
        return null;
      } else if (parts.length == 2 && Strings.isNullOrEmpty(parts[1])) { // Splitting "bucket/" will lead to two parts where the second one is empty,
                                                                         // send only bucket
        return new String[] {parts[0]};
      }
    }
    return parts;
  }

  /**
   * Returns index where bytesToFind begins in the buffer
   */
  public static class ByteMatcherBeginningIndexFinder implements ChannelBufferIndexFinder {
    private byte[] toMatch;

    public ByteMatcherBeginningIndexFinder(byte[] bytesToFind) {
      this.toMatch = bytesToFind;
    }

    @Override
    public boolean find(ChannelBuffer channelBuffer, int i) {
      channelBuffer.markReaderIndex();
      try {
        int matchedCount = 0;
        // Check basic params, like length
        if (i + this.toMatch.length > channelBuffer.readableBytes()) {
          return false;
        }

        // Match byte for byte
        for (int j = i; j - i < this.toMatch.length; j++) {
          if (channelBuffer.getByte(j) != this.toMatch[j - i]) {
            return false;
          }
          matchedCount++;
        }
        return matchedCount == toMatch.length;
      } catch (IndexOutOfBoundsException e) {
        return false;
      } finally {
        channelBuffer.resetReaderIndex();
      }
    }
  }

  public static int findFirstMatchInBuffer(ChannelBuffer buffer, int start, byte[] bytesToFind) {
    return buffer.indexOf(start, buffer.readableBytes(), new ByteMatcherBeginningIndexFinder(bytesToFind));
  }

  public static int findLastMatchInBuffer(ChannelBuffer buffer, int start, byte[] bytesToFind) {
    return buffer.indexOf(buffer.readableBytes(), start, new ByteMatcherBeginningIndexFinder(bytesToFind));
  }

  // Encrypt data using the cloud public key
  public static String encryptWithComponentPublicKey(Class<? extends ComponentId> componentClass, String data) throws EucalyptusCloudException {
    try {
      PublicKey clcPublicKey = SystemCredentials.lookup(componentClass).getCertificate().getPublicKey();
      Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, clcPublicKey, Crypto.getSecureRandomSupplier().get());
      return new String(Base64.encode(cipher.doFinal(data.getBytes("UTF-8"))));
    } catch (Exception e) {
      throw new EucalyptusCloudException("Unable to encrypt data: " + e.getMessage(), e);
    }
  }

  // Decrypt data encrypted with the Cloud public key
  public static String decryptWithComponentPrivateKey(Class<? extends ComponentId> componentClass, String data) throws EucalyptusCloudException {
    PrivateKey clcPrivateKey = SystemCredentials.lookup(componentClass).getPrivateKey();
    try {
      Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.DECRYPT_MODE, clcPrivateKey, Crypto.getSecureRandomSupplier().get());
      return new String(cipher.doFinal(Base64.decode(data)));
    } catch (Exception ex) {
      throw new EucalyptusCloudException("Unable to decrypt data with cloud private key", ex);
    }
  }

  public static void addCopiedHeadersToResponse(HttpResponse httpResponse, ObjectStorageDataResponseType osgResponse) {
    if (osgResponse instanceof HeadObjectResponseType && osgResponse.getContentDisposition() != null
        && !"".equals(osgResponse.getContentDisposition())) {
      httpResponse.addHeader("Content-Disposition", osgResponse.getContentDisposition());
    }
    if (osgResponse.getContentEncoding() != null && !"".equals(osgResponse.getContentEncoding())) {
      httpResponse.addHeader(HttpHeaders.Names.CONTENT_ENCODING, osgResponse.getContentEncoding());
    }
    if (osgResponse.getCacheControl() != null && !"".equals(osgResponse.getCacheControl())) {
      httpResponse.addHeader(HttpHeaders.Names.CACHE_CONTROL, osgResponse.getCacheControl());
    }
    if (osgResponse.getExpires() != null && !"".equals(osgResponse.getExpires())) {
      httpResponse.addHeader(HttpHeaders.Names.EXPIRES, osgResponse.getExpires());
    }
  }

  /**
   * Utility method to determine if the inbound request is for uploading data using the OSG PUT mechanism
   * 
   * @param request
   * @return true if the request is of type S3 PUT object or upload part. false for all other requests
   */
  public static boolean isPUTDataRequest(HttpRequest request) {
    MappingHttpRequest mappingRequest = null;

    // put data and upload part only
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.PUT.toString())
        && request instanceof MappingHttpRequest
        && ((mappingRequest = ((MappingHttpRequest) request)).getParameters() == null || mappingRequest.getParameters().isEmpty()
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString())
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString())
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString().toLowerCase()) || mappingRequest
            .getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString().toLowerCase()))) {
      return true;
    }

    return false;
  }

  /**
   * Utility method to determine if the inbound request is for updating the metadata of buckets or objects
   * 
   * @param request
   * @return true if the request is of type S3 PUT acl, versioning, lifecycle, tagging, notification, initiate mpu - POST uploads, complete mpu - POST
   *         uploadId, mult delete - POST delete. false for all other requests
   */
  public static boolean isPUTMetadataRequest(HttpRequest request) {
    MappingHttpRequest mappingRequest = null;
    String contentType = null;

    // put metadata only
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && request instanceof MappingHttpRequest
        && (mappingRequest = ((MappingHttpRequest) request)).getParameters() != null && !mappingRequest.getParameters().isEmpty()
        && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString())
        && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString())
        && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString().toLowerCase())
        && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString().toLowerCase())) {
      return true;
    }

    // post for initiate mpu, complete mpu and multi delete
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.POST.toString())
        && ((contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)) == null || !contentType.startsWith("multipart/form-data;"))
        && request instanceof MappingHttpRequest && (mappingRequest = ((MappingHttpRequest) request)).getParameters() != null
        && !mappingRequest.getParameters().isEmpty()) {
      return true;
    }

    return false;
  }

  /**
   * Utility method to determine if the inbound request is for uploading form data using the OSG POST mechanism
   * 
   * @param request
   * @return true if the request of type S3 form POST. false for all other requests
   */
  public static boolean isFormPOSTRequest(HttpRequest request) {
    String contentType = null;

    // post form upload only
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.POST.toString())
        && (contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)) != null && contentType.startsWith("multipart/form-data;")) {
      return true;
    }

    return false;
  }

  public static String getBucketFromHostHeader(MappingHttpRequest httpRequest) throws InvalidAddressingHeaderException, TextParseException {
    String hostBucket = null;
    String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
    if (!Strings.isNullOrEmpty(targetHost)) {
      final String host = Iterables.getFirst(hostSplitter.split(targetHost), targetHost);
      final Name hostDnsName = DomainNames.absolute(Name.fromString(host));
      final Optional<Name> systemDomain = DomainNames.systemDomainFor(ObjectStorage.class, hostDnsName);
      if (systemDomain.isPresent()) {
        // dns-style request
        hostBucket = hostDnsName.relativize(systemDomain.get()).toString();
        if (hostBucket.length() == 0) {
          throw new InvalidAddressingHeaderException("Invalid Host header: " + targetHost);
        }
      }
    }
    return hostBucket;
  }
  
  public static CorsMatchResult matchCorsRules (List<CorsRule> corsRules, String requestOrigin, 
      String requestMethod, List<CorsHeader> requestHeaders) {
    CorsMatchResult corsMatchResult = new CorsMatchResult();
    boolean found = false;
    boolean anyOrigin = false;
    CorsRule corsRuleMatch = null;
    
    for (CorsRule corsRule : corsRules ) {

      if (corsRule == null) {
        continue;
      }
      
      corsRuleMatch = corsRule; // will only be used if we find a match
      
      // Does the origin match any origin's regular expression in the rule?
      String[] allowedOrigins = corsRule.getAllowedOrigins();
      found = false;
      for (int idx = 0; idx < allowedOrigins.length; idx++) {
        String allowedOriginRegex = "\\Q" + allowedOrigins[idx].replace("*", "\\E.*?\\Q") + "\\E";
        Pattern p = Pattern.compile(allowedOriginRegex);
        Matcher m = p.matcher(requestOrigin);
        boolean match = m.matches();
        if (match) {
          anyOrigin = (allowedOrigins[idx].equals("*"));
          found = true;
          break;  // stop looking through the origins for this rule
        }
      }
      if (!found) {
        continue;  // go to the next CORS rule
      }
      
      // Does the HTTP verb match any verb in the rule?
      String[] allowedMethods = corsRule.getAllowedMethods();
      found = false;
      for (int idx = 0; idx < allowedMethods.length; idx++) {
        if (requestMethod.equals(allowedMethods[idx])) {
          found = true;
          break;  // stop looking through the methods for this rule
        }
      }
      if (!found) {
        continue;  // go to the next CORS rule
      }
      
      // If there are no Access-Control-Request-Headers, or if there are
      // no AllowedHeaders in the CORS rule, then skip this check.
      // We have matched the current CORS rule. Stop looking through them.
      if (requestHeaders == null || requestHeaders.size() == 0) {
        break;
      }
      String[] allowedHeaders = corsRule.getAllowedHeaders();
      if (allowedHeaders == null || allowedHeaders.length == 0) {
        break;
      }

      // Does every request header in the comma-delimited list in 
      // Access-Control-Request-Headers have a matching entry in the 
      // allowed headers in the rule?
      found = false;
      Pattern[] allowedHeaderRegexPattern = new Pattern[allowedHeaders.length];
      for (CorsHeader requestHeader : requestHeaders) {
        found = false;
        for (int idx = 0; idx < allowedHeaders.length; idx++) {
          if (allowedHeaderRegexPattern[idx] == null) {
            String allowedHeaderRegex = "\\Q" + allowedHeaders[idx].replace("*", "\\E.*?\\Q") + "\\E";
            allowedHeaderRegexPattern[idx] = Pattern.compile(allowedHeaderRegex);
          }
          Matcher matcher = allowedHeaderRegexPattern[idx].matcher(requestHeader.getCorsHeader());
          boolean match = matcher.matches();
          if (match) {
            found = true;
            break;  // stop looking through the allowed headers for this request header
          }
        }
        if (!found) {
          // No allowed header matches this request header, so this rule fails to match
          break;  // stop looking through the request headers
        }
      }  // end for each request header
      if (found) {
        // Request headers are OK too, so everything in the request matches 
        // this rule. Stop looking through CORS rules.
        break;
      }
    }  // end for each CORS rule

    if (found) {
      corsMatchResult.setCorsRuleMatch(corsRuleMatch);
      corsMatchResult.setAnyOrigin(anyOrigin);
    }
    return corsMatchResult;
  }
  
  public static void setCorsInfo(ObjectStorageRequestType request, BaseMessage msg, String bucketUuid) {
    // NOTE: The getBucket() might be the bucket name, or might be the UUID, depending on how it's used by the caller.
    // So, we don't depend on it to be either one. We just copy it to the reply so it shows up in 
    // cases that normally use it, like exception messages.
    // The getBucketUuid() is the field we depend on to look up bucket entities in the DB.
    // 
    // Don't change any response fields that are already set.
    
    if (request != null && msg != null) {
      if (msg instanceof ObjectStorageResponseType) {
        ObjectStorageResponseType response = (ObjectStorageResponseType) msg;
        if (response.getOrigin() == null) {
          response.setOrigin(request.getOrigin());
        }
        if (response.getHttpMethod() == null) {
          response.setHttpMethod(request.getHttpMethod());
        }
        if (response.getBucket() == null) {
          response.setBucket(request.getBucket());
        }
        if (response.getBucketUuid() == null) {
          response.setBucketUuid(bucketUuid);
        }
      } else if (msg instanceof ObjectStorageDataResponseType) {
        ObjectStorageDataResponseType response = (ObjectStorageDataResponseType) msg;
        if (response.getOrigin() == null) {
          response.setOrigin(request.getOrigin());
        }
        if (response.getHttpMethod() == null) {
          response.setHttpMethod(request.getHttpMethod());
        }
        if (response.getBucket() == null) {
          response.setBucket(request.getBucket());
        }
        if (response.getBucketUuid() == null) {
          response.setBucketUuid(bucketUuid);
        }
      }
    }
  }

}
