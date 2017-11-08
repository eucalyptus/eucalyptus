/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidAddressingHeaderException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageCommonResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageErrorMessageType;
import com.eucalyptus.storage.config.ConfigurationCache;
import com.eucalyptus.storage.msgs.s3.CorsMatchResult;
import com.eucalyptus.storage.msgs.s3.CorsRule;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.net.InetAddresses;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class OSGUtil {
  private static Logger LOG = Logger.getLogger(OSGUtil.class);

  private static final Splitter hostSplitter = Splitter.on(':').limit(2);

  private static final Function<String,Pattern> cnamePatternBuilder = new Function<String, Pattern>( ) {
    @Override
    public Pattern apply( @Nullable final String list ) {
      return buildPatternFromWildcardList( Strings.nullToEmpty( list ) );
    }

    private Pattern buildPatternFromWildcardList( @Nonnull final String list )  {
      final Splitter splitter =
          Splitter.on( CharMatcher.WHITESPACE.or( CharMatcher.anyOf( ",;|" ) ) ).trimResults( ).omitEmptyStrings( );
      final Splitter wildSplitter = Splitter.on( '*' );
      final Joiner wildJoiner = Joiner.on( ".*");
      final StringBuilder builder = new StringBuilder( );
      builder.append( "(?i)(-|" );
      for ( final String cnameWildcard : splitter.split( list ) ) {
        builder.append( wildJoiner.join( Iterables.transform( wildSplitter.split( cnameWildcard ), Pattern::quote ) ) );
        builder.append( '|' );
      }
      builder.append( "-)" );
      return Pattern.compile( builder.toString( ) );
    }
  };

  private static final Function<String,Pattern> memoizedCnamePatternBuilder = FUtils.memoizeLast( cnamePatternBuilder );

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
      if (ex instanceof S3Exception){
        S3Exception e = (S3Exception) ex;
        errMsg =
            new ObjectStorageErrorMessageType(e.getMessage(), e.getCode(), e.getStatus(), e.getResourceType(), e.getResource(), correlationId,
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

  public static CorsMatchResult matchCorsRules (List<CorsRule> corsRules, String requestOrigin, 
      String requestMethod) {
    // All S3 operations except Preflight (OPTIONS) requests call matchCorsRules with this signature,
    // because "Access-Control-Request-Headers" are only present (if at all) in preflight requests.
    return matchCorsRules(corsRules, requestOrigin, requestMethod, null);
  }

  public static CorsMatchResult matchCorsRules (List<CorsRule> corsRules, String requestOrigin, 
      String requestMethod, List<String> requestHeaders) {
    // Only Preflight (OPTIONS) requests call matchCorsRules with this signature.
    CorsMatchResult corsMatchResult = new CorsMatchResult();
    boolean found = false;
    boolean anyOrigin = false;
    CorsRule corsRuleMatch = null;
    
    // Predicate for matching origin
    Predicate<String> originMatch = new Predicate<String>() {
      @Override
      public boolean apply(String allowedOrigin) {
        String allowedOriginRegex = "\\Q" + allowedOrigin.replace("*", "\\E.*?\\Q") + "\\E";
        return Pattern.matches(allowedOriginRegex, requestOrigin);
      }
    };

    // Predicate for matching method
    Predicate<String> methodMatch = new Predicate<String>() {
      @Override
      public boolean apply(String allowedMethod) {
        return requestMethod.equals(allowedMethod);
      }
    };

    // Function for generating a pattern from an allowed header
    Function<String, Pattern> generatePattern = new Function<String, Pattern>() {
      @Override
      public Pattern apply(String allowedHeader) {
        String allowedHeaderRegex = "\\Q" + allowedHeader.replace("*", "\\E.*?\\Q") + "\\E";
        return Pattern.compile(allowedHeaderRegex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
      }
    };

    for (CorsRule corsRule : corsRules ) {

      if (corsRule == null) {
        continue;
      }
      
      corsRuleMatch = corsRule; // will only be used if we find a match
      
      // Does the origin match any origin's regular expression in the rule?
      // Note: AWS matches origins case-sensitively! Even though URL domains
      // are typically case-insensitive. Follow AWS's behavior.
      List<String> allowedOrigins = corsRule.getAllowedOrigins();
      
      if (allowedOrigins == null || allowedOrigins.isEmpty()) {
        continue;
      }

      String matchingOrigin = Iterables.tryFind(corsRule.getAllowedOrigins(), originMatch).orNull();
      
      // If no matching origin, skip to the next CORS rule
      if (matchingOrigin == null) {
        continue;
      }
      
      // We did find a matching origin. If it's "*", then any origin can access our resources.
      // This is a special case we want to flag for easy access.
      anyOrigin = (matchingOrigin.equals("*"));

      // Does the HTTP verb match any verb in the rule?
      // If not, skip to the next CORS rule
      List<String> allowedMethods = corsRule.getAllowedMethods();

      if (allowedMethods == null || allowedMethods.isEmpty() || 
          !Iterables.any(corsRule.getAllowedMethods(), methodMatch)) {
        continue;
      }

      // Yes, the HTTP verb matches a verb in this rule.
      
      // If there are no Access-Control-Request-Headers, or if there are
      // no AllowedHeaders in the CORS rule, then skip this check.
      // We have matched the current CORS rule. Stop looking through them.
      if (requestHeaders == null || requestHeaders.isEmpty() ||
          corsRule.getAllowedHeaders() == null || corsRule.getAllowedHeaders().isEmpty()) {
        found = true;
        break;
      }

      // Does every request header in the comma-delimited list in 
      // Access-Control-Request-Headers have a matching entry in the 
      // allowed headers in the rule?
      // Headers are matched case-insensitively.

      List<String> allowedHeaders = corsRule.getAllowedHeaders();
      List<Pattern> allowedHeaderPatternList = new ArrayList<Pattern>(allowedHeaders.size());
      
      // Predicate for matching request header with allowed headers
      Predicate<String> headerMatch = new Predicate<String>() {
        @Override
        public boolean apply(String requestHeader) {
          for (int idx = 0; idx < allowedHeaders.size(); idx++) {
            Pattern allowedHeaderPattern;
            // Only generate the pattern if we haven't already
            if (idx >= allowedHeaderPatternList.size()) {
              allowedHeaderPattern = generatePattern.apply(allowedHeaders.get(idx));
              allowedHeaderPatternList.add(allowedHeaderPattern);
            } else {
              allowedHeaderPattern = allowedHeaderPatternList.get(idx);
            }
            Matcher matcher = allowedHeaderPattern.matcher(requestHeader);
            if (matcher.matches()) {
              return true; // stop looking through the allowed headers for this request header
            } else {
              continue; // try matching request header with allowed header until a match is found
            }
          }
          return false; // No allowed header matches this request header, so this rule fails to match
        }
      };

      // If request headers match allowed headers, we have matched the current CORS rule. 
      // Stop looking through them.
      if (Iterables.all(requestHeaders, headerMatch)) {
        found = true;
        break;
      }
    }  // end for each CORS rule

    if (found) {
      corsMatchResult.setCorsRuleMatch(corsRuleMatch);
      corsMatchResult.setAnyOrigin(anyOrigin);
    }
    return corsMatchResult;
  }  

  public static void addCorsResponseHeaders (MappingHttpResponse mappingHttpResponse) throws S3Exception {
    if (mappingHttpResponse != null &&
        mappingHttpResponse.getMessage() instanceof ObjectStorageCommonResponseType) 
    {
      addCorsResponseHeaders((HttpResponse) mappingHttpResponse, (ObjectStorageCommonResponseType) mappingHttpResponse.getMessage());
    }
  }
  
  public static void addCorsResponseHeaders (HttpResponse httpResponse, ObjectStorageCommonResponseType response) throws S3Exception {
    if (response.getAllowedOrigin() == null) {
      // If no allowed origin header then we know there are no CORS headers at all
      return;
    }
    
    httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, response.getAllowedOrigin());
    if (response.getAllowedMethods() != null) {
      httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, response.getAllowedMethods());
    }
    if (response.getExposeHeaders() != null) {
      httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_EXPOSE_HEADERS, response.getExposeHeaders());
    }
    if (response.getMaxAgeSeconds() != null) {
      httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_MAX_AGE, response.getMaxAgeSeconds());
    }
    if (response.getAllowCredentials() != null) {
      httpResponse.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, response.getAllowCredentials());
    }
    if (response.getVary() != null) {
      httpResponse.setHeader(HttpHeaders.Names.VARY, response.getVary());
    }
  }  // end addCorsResponseHeaders()

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
  public static boolean isPUTDataRequest(HttpRequest request, Set<String> servicePaths) {
    MappingHttpRequest mappingRequest = null;

    // put data and upload part only
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && request instanceof MappingHttpRequest
        && !isBucketOp((mappingRequest = (MappingHttpRequest) request), servicePaths)
        && (mappingRequest.getParameters() == null || mappingRequest.getParameters().isEmpty()
            || mappingRequest.getParameters().containsKey("progressbar_label") // See EUCA-13210
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString())
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString())
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString().toLowerCase())
            || mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString().toLowerCase()))) {
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
  public static boolean isPUTMetadataRequest(HttpRequest request, Set<String> servicePaths) {
    MappingHttpRequest mappingRequest = null;
    String contentType = null;

    // put metadata only - any bucket operation should be considered as metadata
    if (request.getMethod().getName().equals(ObjectStorageProperties.HTTPVerb.PUT.toString()) && request instanceof MappingHttpRequest
        && (isBucketOp((mappingRequest = (MappingHttpRequest) request), servicePaths)
            || (mappingRequest.getParameters() != null && !mappingRequest.getParameters().isEmpty()
                && !mappingRequest.getParameters().containsKey("progressbar_label") // See EUCA-13210
                && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString())
                && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString())
                && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.uploadId.toString().toLowerCase())
                && !mappingRequest.getParameters().containsKey(ObjectStorageProperties.SubResource.partNumber.toString().toLowerCase())))) {
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

  /**
   * A name can be a bucket name if it is an object storage subdomain or if it
   * is not a system subdomain (bucket cname)
   */
  public static boolean isBucketName( final Name name, final boolean allowCname ) {
    final Optional<Name> systemDomain = DomainNames.systemDomainFor( ObjectStorage.class, name );
    return systemDomain.isPresent( ) || (allowCname && isAllowedBucketHost( name ));
  }

  public static String getBucketFromHostHeader(
      final MappingHttpRequest httpRequest
  ) throws InvalidAddressingHeaderException, TextParseException {
    String hostBucket = null;
    String targetHost = httpRequest.getHeader(HttpHeaders.Names.HOST);
    if (!Strings.isNullOrEmpty(targetHost)) {
      final String host = Iterables.getFirst(hostSplitter.split(targetHost), targetHost);
      if ( host != null ) {
        final Name hostDnsName = DomainNames.absolute(Name.fromString(host));
        final Optional<Name> systemDomain = DomainNames.systemDomainFor(ObjectStorage.class, hostDnsName);
        if (systemDomain.isPresent()) {
          // dns-style request
          hostBucket = hostDnsName.relativize(systemDomain.get()).toString();
          if (hostBucket.length() == 0) {
            throw new InvalidAddressingHeaderException(null, "Invalid Host header: " + targetHost);
          }
        } else if ( isAllowedBucketHost( hostDnsName, host ) ) {
          hostBucket = host;
        }
      }
    }
    return hostBucket == null ? null : hostBucket.toLowerCase( );
  }

  public static boolean isAllowedBucketHost(
      @Nonnull final Name name
  ) {
    return isAllowedBucketHost( name, name.relativize( Name.root ).toString( ) );
  }

  public static boolean isAllowedBucketHost(
      @Nonnull final Name name,
      @Nonnull final String nameText
  ) {
    return !InetAddresses.isInetAddress( nameText ) &&
        !DomainNames.isSystemSubdomain( name ) &&
        !isReservedBucketCname( nameText );
  }

  public static boolean isReservedBucketCname( @Nonnull final String nameText ) {
    final String reservedCnamePatternList =
        ConfigurationCache.getConfiguration( ObjectStorageGlobalConfiguration.class ).getBucket_reserved_cnames( );
    return memoizedCnamePatternBuilder.apply( reservedCnamePatternList ).matcher( nameText ).matches( );
  }

  public static boolean isBucketOp(MappingHttpRequest httpRequest, Set<String> servicePaths) {
    try {
      String hostBucket = null;
      String[] target = null;
      String path = getOperationPath(httpRequest, servicePaths);
      if ((hostBucket = getBucketFromHostHeader(httpRequest)) != null) {
        path = "/" + hostBucket + path;
      }
      return (path.length() > 0 && (target = getTarget(path)) != null && target.length == 1);
    } catch (Exception e) {
      LOG.warn("Considering the request to be non-bucket op, unable to identify bucket and object in request due to", e);
    }

    return false;
  }

  /**
   * Removes the service path for processing the bucket/key split.
   * 
   * @param httpRequest
   * @return
   */
  public static String getOperationPath(MappingHttpRequest httpRequest, Set<String> servicePaths) {
    for (String pathCandidate : servicePaths) {
      if (httpRequest.getServicePath().startsWith(pathCandidate)) {
        String opPath = httpRequest.getServicePath().replaceFirst(pathCandidate, "");
        if (!Strings.isNullOrEmpty(opPath) && !opPath.startsWith("/")) {
          // The service path was not demarked with a /, e.g. /services/objectstorageblahblah -> blahblah
          // So, don't remove the service path because that changes the semantics.
          break;
        } else {
          return opPath;
        }
      }
    }

    return httpRequest.getServicePath();
  }
}
