/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.storage.msgs.s3

import edu.ucsb.eucalyptus.msgs.EucalyptusData

import java.util.ArrayList
import java.util.List;

import org.jboss.netty.handler.codec.http.HttpMethod;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID


/*
 * NOTE: not used by either Walrus or OSG, yet.
 * NOTE: lifecycle types used by OSG now
 */

/*
 * Bucket operation messages and types for the S3 API
 */

/*
 * HEAD /bucket
 */
public class HeadBucketRequest extends S3Request {}
public class HeadBucketResponse extends S3Response {}

/*
 * GET /bucket
 */
public class ListBucketRequest extends S3Request {
  String prefix;
  String marker;
  String maxKeys;
  String delimiter;

  def ListBucketRequest() {
    prefix = "";
    marker = "";
    delimiter = "";
  }
}

public class ListBucketResponse extends S3Response {
  String name;
  String prefix;
  String marker;
  String nextMarker;
  int maxKeys;
  String delimiter;
  boolean isTruncated;
  ArrayList<MetaDataEntry> metaData;
  ArrayList<ListEntry> contents;
  ArrayList<CommonPrefixesEntry> commonPrefixes = new ArrayList<CommonPrefixesEntry>();

  /**
   * This is used for properly marshalling the response XML, needed to determine
   * empty element vs no element
   * @return
   */
  private boolean isCommonPrefixesPresent() {
    return commonPrefixes.size() > 0 ? true : false;
  }
}

/*
 * GET /bucket/?versions
 */
public class ListVersionsRequest extends S3Request {
  String prefix;
  String keyMarker;
  String versionIdMarker;
  String maxKeys;
  String delimiter;

  def ListVersionsRequest() {
    prefix = "";
  }
}

public class ListVersionsResponse extends S3Response {
  String name;
  String prefix;
  String keyMarker;
  String versionIdMarker;
  String nextKeyMarker;
  String nextVersionIdMarker;
  int maxKeys;
  String delimiter;
  boolean isTruncated;
  ArrayList<KeyEntry> keyEntries;
  ArrayList<CommonPrefixesEntry> commonPrefixes;
}

/*
 * PUT /bucket
 */
public class CreateBucketRequest extends S3Request {
  AccessControlList accessControlList;
  BucketCreationConfiguration bucketConfiguration;

  //For unit testing
  public CreateBucketRequest() {}

  public CreateBucketRequest(String bucket) {
    this.bucket = bucket;
  }

  public CreateBucketRequest(String bucket, BucketCreationConfiguration config) {
    this.bucket = bucket;
    this.bucketConfiguration = config;
  }
}

public class CreateBucketResponse extends S3Response {
  String bucket;
}

public class BucketCreationConfiguration extends EucalyptusData {
  LocationConstraint bucketLocationConstraint;
}

public class LocationConstraint extends EucalyptusData {
  String location;
}

/*
 * GET /bucket/?location
 */
public class GetBucketLocation extends S3Request {}
public class GetBucketLocationResponse extends S3Response {
  String locationConstraint;
}

/*
 * DELETE /bucket
 */
public class DeleteBucketRequest extends S3Request {}
public class DeleteBucketResponse extends S3Response {}


/*
 * --------------------
 * BUCKET LOGGING
 * --------------------
 */

/*
 * GET /bucket/?logging
 */
public class GetBucketLoggingStatusRequest extends S3Request {}
public class GetBucketLoggingStatusResponse extends S3Response {
  LoggingEnabled loggingEnabled;
}

/*
 * PUT /bucket/?logging
 */
public class SetBucketLoggingStatusRequest extends S3Request {
  LoggingEnabled loggingEnabled;
}
public class SetBucketLoggingStatusResponse extends S3Response {}

/*
 * --------------------
 * BUCKET VERSIONING
 * --------------------
 */

/*
 * GET /bucket/?versioning
 */
public class GetBucketVersioningStatusRequest extends S3Request {}
public class GetBucketVersioningStatusResponse extends S3Response {
  VersioningConfiguration configuration;
}

public class VersioningConfiguration extends EucalyptusData {
  String status;
  String mfaDelete; //Unused currently
}

/*
 * PUT /bucket/?versioning
 */
public class SetBucketVersioningStatusRequest extends S3Request {
  VersioningConfiguration configuration;
}

public class SetBucketVersioningStatusResponse extends S3Response {}

/*
 * --------------------
 * BUCKET ACLs
 * --------------------
 */

/*
 * SOAP-based
 */
public class SetBucketAccessControlPolicyRequest extends S3Request {
  AccessControlList accessControlList;
}
public class SetBucketAccessControlPolicyResponse extends S3Response {
  String code;
  String description;
}

/*
 * GET /bucket/?acl
 */
public class GetBucketAccessControlPolicyRequest extends S3Request {}
public class GetObjectAccessControlPolicyResponse extends S3Response {
  AccessControlPolicy accessControlPolicy;
}


/*
 * PUT /bucket/?acl
 */
public class SetRESTBucketAccessControlPolicyRequest extends S3Request {
  AccessControlPolicy accessControlPolicy;
}

public class SetRESTBucketAccessControlPolicyResponse extends S3Response {
  String code;
  String description;
}

/*
 * --------------------
 * BUCKET CORS
 * --------------------
 * 
 * See ObjectStorageGroovy.java for get/set/delete handlers
 */

/*
 * Data types
 */

public class AllowedCorsMethods extends EucalyptusData {

  // Valid methods for a CORS rule are: GET, HEAD, PUT, POST, DELETE
  public static List<HttpMethod> methodList = new ArrayList<HttpMethod>();

  static {
    methodList.add(HttpMethod.GET);
    methodList.add(HttpMethod.HEAD);
    methodList.add(HttpMethod.PUT);
    methodList.add(HttpMethod.POST);
    methodList.add(HttpMethod.DELETE);
  }

}

public class CorsConfiguration extends EucalyptusData {
  List<CorsRule> rules;
}

public class CorsRule extends EucalyptusData {
  String id;
  int sequence;
  List<String> allowedOrigins;
  List<String> allowedMethods;
  List<String> allowedHeaders;
  int maxAgeSeconds;
  List<String> exposeHeaders;
}

public class CorsMatchResult extends EucalyptusData {
  CorsRule CorsRuleMatch = null;
  boolean anyOrigin = false;
}

public class PreflightRequest extends EucalyptusData {
  String origin;
  String method;
  List<String> requestHeaders;

  public String toString() {
    StringBuffer output = new StringBuffer();
    output.append(
        "Origin: " + origin +
        "\nMethod: " + method +
        "\nRequest Headers:");
    if (requestHeaders == null || requestHeaders.size() == 0) {
      output.append(" null");
    } else {
      for (String requestHeader : requestHeaders) {
        output.append("\n  " + requestHeader);
      }
    }
    return output;
  }
}

public class PreflightResponse extends EucalyptusData {
  String origin;
  List<String> methods;
  int maxAgeSeconds;
  List<String> allowedHeaders;
  List<String> exposeHeaders;

  public String toString() {
    StringBuffer output = new StringBuffer();
    output.append(
        "Origin: " + origin +
        //"\nMethod: " + method +
        "\nMax Age, Seconds: " + maxAgeSeconds +
        "\nAllowed Headers:");
    if (allowedHeaders == null || allowedHeaders.size() == 0) {
      output.append(" null");
    } else {
      for (String allowedHeader : allowedHeaders) {
        output.append("\n  " + allowedHeader);
      }
    }
    output.append(
        "\nExpose Headers:");
    if (exposeHeaders == null || exposeHeaders.size() == 0) {
      output.append(" null");
    } else {
      for (String exposeHeader : exposeHeaders) {
        output.append("\n  " + exposeHeader);
      }
    }
    return output;
  }
}

/*
 * --------------------
 * BUCKET WEBSITE
 * --------------------
 */

/*
 * GET /bucket/?website
 */
public class GetBucketWebsiteRequest extends S3Request {}
public class GetBucketWebsiteResponse extends S3Response {
  WebsiteConfiguration config;
}

/*
 * PUT /bucket/?website
 */
public class SetBucketWebsiteRequest extends S3Request {
  WebsiteConfiguration config;
}
public class SetBucketWebsiteResponse extends S3Response {}

public class WebsiteConfiguration extends EucalyptusData {
  WebsiteRedirectConfiguration redirectAllConfiguration;
  IndexDocumentConfiguration indexDocument;
  ErrorDocumentConfiguration errorDocument;
  ArrayList<RoutingRule> routingRules;
}

public class RoutingRule extends EucalyptusData {
  RoutingCondition condition;
  RoutingRedirectConfiguration redirect;
}

public class RoutingCondition extends EucalyptusData {
  String keyPrefixEquals;
  String httpErrorCodeReturnedEquals;
}

public class RoutingRedirectConfiguration extends EucalyptusData {
  String replaceKeyPrefixWith;
  String replaceKeyWith;
  String hostName;
  String protocol;
  String httpRedirectCode;
}

public class IndexDocumentConfiguration extends EucalyptusData {
  String suffix;
}

public class ErrorDocumentConfiguration extends EucalyptusData {
  String key; //Object key to use when 4xx error occurs. page returned will be this object
}

public class WebsiteRedirectConfiguration extends EucalyptusData {
  String hostName;
  String protocol;
}

/*
 * --------------------
 * BUCKET TAGGING
 * --------------------
 */

public class TaggingConfiguration extends EucalyptusData {
  BucketTagSet bucketTagSet;
}

public class BucketTagSet extends EucalyptusData {
  List<BucketTag> bucketTags;
}

/*
 * GET /bucket/?tagging
 */
/*
 public class GetBucketTaggingRequest extends S3Request {}
 */
public class BucketTag extends EucalyptusData {
  String key;
  String value;
  public BucketTag() {}

  public BucketTag(String k, String v) {
    this.key = k;
    this.value = v;
  }
}

/*
 public class GetBucketTaggingResponse extends S3Response {
 ArrayList<Tag> tagSet;
 }
 */


/*
 * PUT /bucket/?tagging
 */

/*
 public class SetBucketTaggingRequest extends S3Request {
 ArrayList<Tag> tagSet;
 }
 public class SetBucketTaggingResponse extends S3Response {}
 */

/*
 * DELETE /bucket/?tagging
 */

/*
 public class DeleteBucketTaggingRequest extends S3Request {
 ArrayList<Tag> tagSet;
 }
 public class DeleteBucketTaggingResponse extends S3Response {}
 */
/*
 * --------------------
 * BUCKET LIFECYCLE
 * --------------------
 */

/*
 * GET /bucket/?lifecycle
 */
//public class GetBucketLifecycleRequest extends S3Request {}

public class LifecycleConfiguration extends EucalyptusData {
  List<LifecycleRule> rules;
}

public class LifecycleRule extends EucalyptusData {
  String id;
  String prefix;
  String status;
  Expiration expiration;
  Transition transition;
}

public class Expiration extends EucalyptusData {
  int creationDelayDays;
  Date effectiveDate;
}

/*
 * Transition for objects to AWS Glacier, obviously not supported yet. Here for completeness of API
 */
public class Transition extends Expiration {
  String destinationClass; //Only valid value = "GLACIER"
}

/*
 public class GetBucketLifecycleResponse extends S3Response {
 ArrayList<Tag> tagSet;
 }
 */
/*
 * PUT /bucket/?lifecycle
 *//*
 public class SetBucketLifecycleRequest extends S3Request {
 ArrayList<Tag> tagSet;
 }
 public class SetBucketLifecycleResponse extends S3Response {}
 */
/*
 * DELETE /bucket/?lifecycle
 *//*
 public class DeleteBucketLifecycleRequest extends S3Request {}
 public class DeleteBucketLifecycleResponse extends S3Response {}
 */

/* 
 * --------------------
 * BUCKET POLICY
 * --------------------
 */

/*
 * GET /bucket/?policy
 */
public class GetBucketPolicyRequest extends S3Request {}
public class GetBucketPolicyResponse extends S3Response {
  String policyJSON; //The body of the response is the raw JSON
}

/*
 * PUT /bucket/?policy
 */
public class SetBucketPolicyRequest extends S3Request {
  String policyJSON;
}
public class SetBucketPolicyResponse extends S3Response {}

/*
 * DELETE /bucket/?policy
 */
public class DeleteBucketPolicyRequest extends S3Request {}
public class DeleteBucketPolicyResponse extends S3Response {}

/*
 * POST /bucket/?delete
 */
public class DeleteMultipleObjectsMessage extends EucalyptusData {
  Boolean quiet;
  List<DeleteMultipleObjectsEntry> objects;
}
public class DeleteMultipleObjectsMessageReply extends EucalyptusData {
  List<DeleteMultipleObjectsEntryVersioned> deleted;
  List<DeleteMultipleObjectsError> errors;
}

public class DeleteMultipleObjectsEntry extends EucalyptusData {
  String key;
  String versionId;
}

public class DeleteMultipleObjectsEntryVersioned extends DeleteMultipleObjectsEntry {
  Boolean deleteMarker;
  String deleteMarkerVersionId;
}

public class DeleteMultipleObjectsError extends DeleteMultipleObjectsEntry {
  DeleteMultipleObjectsErrorCode code;
  String message;
}

public enum DeleteMultipleObjectsErrorCode {
  AccessDenied,
  InternalError;
}
