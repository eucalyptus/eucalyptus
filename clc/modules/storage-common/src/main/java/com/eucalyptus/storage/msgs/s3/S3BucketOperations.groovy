/*************************************************************************
* Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.storage.msgs.s3


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

public class BucketCreationConfiguration {
	LocationConstraint bucketLocationConstraint;
}

public class LocationConstraint {
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

public class VersioningConfiguration {
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
 */

/*
 * GET /bucket/?cors
 */
public class GetBucketCorsRequest extends S3Request {}
public class GetBucketCorsResponse extends S3Response {
	CORSConfiguration corsConfig;
}

/*
 * PUT /bucket/?cors
 */
public class SetBucketCorsRequest extends S3Request {
	CORSConfiguration corsConfig;
}
public class SetBucketCorsResponse extends S3Response {}

public class CORSConfiguration {
	ArrayList<CORSRule> rules;
}

public class CORSRule {
	String id;
	ArrayList<String> allowedMethods;
	ArrayList<String> allowedOrigins;
	ArrayList<String> allowedHeaders;
	int maxAgeSeconds;
	ArrayList<String> exposeHeaders;
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

public class WebsiteConfiguration {
	WebsiteRedirectConfiguration redirectAllConfiguration;
	IndexDocumentConfiguration indexDocument;	
	ErrorDocumentConfiguration errorDocument;
	ArrayList<RoutingRule> routingRules;
}

public class RoutingRule {
	RoutingCondition condition;
	RoutingRedirectConfiguration redirect;
}

public class RoutingCondition {
	String keyPrefixEquals;
	String httpErrorCodeReturnedEquals;
}

public class RoutingRedirectConfiguration {
	String replaceKeyPrefixWith;
	String replaceKeyWith;
	String hostName;
	String protocol;
	String httpRedirectCode;	
}

public class IndexDocumentConfiguration {
	String suffix;
}

public class ErrorDocumentConfiguration {
	String key; //Object key to use when 4xx error occurs. page returned will be this object
}

public class WebsiteRedirectConfiguration {
	String hostName;
	String protocol;
}

/*
 * --------------------
 * BUCKET TAGGING
 * --------------------
 */

/*
 * GET /bucket/?tagging
 */
public class GetBucketTaggingRequest extends S3Request {}
public class Tag {
	String key;
	String value;
}

public class GetBucketTaggingResponse extends S3Response {
	ArrayList<Tag> tagSet;
}

/*
 * PUT /bucket/?tagging
 */
public class SetBucketTaggingRequest extends S3Request {
	ArrayList<Tag> tagSet;
}
public class SetBucketTaggingResponse extends S3Response {}

/*
 * DELETE /bucket/?tagging
 */
public class DeleteBucketTaggingRequest extends S3Request {
	ArrayList<Tag> tagSet;
}

public class DeleteBucketTaggingResponse extends S3Response {}

/*
 * --------------------
 * BUCKET LIFECYCLE
 * --------------------
 */

/*
 * GET /bucket/?lifecycle
 */
//public class GetBucketLifecycleRequest extends S3Request {}

public class LifecycleConfiguration {
	List<LifecycleRule> rules;
}

public class LifecycleRule {
	String id;
	String prefix;
	String status;
	Expiration expiration;
    Transition transition;
}

public class Expiration {
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
 */
public class DeleteBucketLifecycleRequest extends S3Request {}
public class DeleteBucketLifecycleResponse extends S3Response {}


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

