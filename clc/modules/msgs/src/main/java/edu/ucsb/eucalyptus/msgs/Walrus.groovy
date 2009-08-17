package edu.ucsb.eucalyptus.msgs
import org.jboss.netty.handler.codec.http.HttpResponseStatusimport org.jboss.netty.channel.Channel;
import edu.ucsb.eucalyptus.constants.IsData;
/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
public class WalrusResponseType extends EucalyptusMessage {
	def WalrusResponseType() {}
}

public class WalrusRequestType extends EucalyptusMessage {
	protected String accessKeyID;
	protected Date timeStamp;
	protected String signature;
	protected String credential;
	def WalrusRequestType() {}
	
	def WalrusRequestType(String accessKeyID, Date timeStamp, String signature, String credential) {
		this.accessKeyID = accessKeyID;
		this.timeStamp = timeStamp;
		this.signature = signature;
		this.credential = credential;
	}
	
	public String getAccessKeyID() {
		return accessKeyID;
	}
	
	public void setAccessKeyID(String accessKeyID) {
		this.accessKeyID = accessKeyID;
	}
	
	public String getCredential() {
		return credential;
	}
	
	public void setCredential(String credential) {
		this.credential = credential;
	}
}

public class WalrusDeleteType extends WalrusRequestType {
}

public class WalrusDeleteResponseType extends WalrusResponseType {
}

public class InitializeWalrusType extends WalrusRequestType {
}

public class InitializeWalrusResponseType extends WalrusResponseType {
}

public class CanonicalUserType extends EucalyptusData {
	String ID;
	String DisplayName;
	
	public CanonicalUserType() {}
	
	public CanonicalUserType (String ID, String DisplayName) {
		this.ID = ID;
		this.DisplayName = DisplayName;
	}
}

public class Group extends EucalyptusData {
	String uri;
	
	public Group() {}
	
	public Group(String uri) {
		this.uri = uri;
	}
}

public class AccessControlPolicyType extends EucalyptusData {
	AccessControlPolicyType() {}
	AccessControlPolicyType(CanonicalUserType owner, AccessControlListType acl) {
		this.owner = owner; this.accessControlList = acl;
	}
	
	CanonicalUserType owner;
	AccessControlListType accessControlList;
}

public class Grantee extends EucalyptusData {
	CanonicalUserType canonicalUser;
	Group group;
	String type;
	
	public Grantee() {}
	
	public Grantee(CanonicalUserType canonicalUser) {
		this.canonicalUser = canonicalUser;
		type = "CanonicalUser";
	}
	
	public Grantee(Group group) {
		this.group = group;
		type = "Group";
	}
}

public class Grant extends EucalyptusData {
	Grantee grantee;
	String permission;
	
	public Grant() {}
	
	public Grant(Grantee grantee, String permission) {
		this.grantee = grantee;
		this.permission = permission;
	}
}

public class AccessControlListType extends EucalyptusData {
	ArrayList<Grant> grants = new ArrayList<Grant>();
}

public class GetBucketAccessControlPolicyResponseType extends EucalyptusMessage {
	AccessControlPolicyType accessControlPolicy;
}

public class GetBucketAccessControlPolicyType extends WalrusRequestType {
	String bucket;
}

public class GetObjectAccessControlPolicyResponseType extends EucalyptusMessage {
	AccessControlPolicyType accessControlPolicy;
}

public class GetObjectAccessControlPolicyType extends WalrusRequestType {
	String bucket;
	String key;
}

public class WalrusErrorMessageType extends EucalyptusMessage {
	protected String code;
	protected String message;
	protected String requestId;
	protected String hostId;
	protected Integer httpCode;
	protected HttpResponseStatus status;
	
	public Integer getHttpCode() {
		return httpCode;
	}
	
	public HttpResponseStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}

public class WalrusBucketErrorMessageType extends WalrusErrorMessageType {
	protected String bucketName;
	
	public WalrusBucketErrorMessageType() {
	}
	
	public WalrusBucketErrorMessageType(String bukkit, String code, String message, Integer httpCode, String requestId, String hostId) {
		bucketName = bukkit;
		this.code = code;
		this.message = message;
		this.requestId = requestId;
		this.hostId = hostId;
		this.httpCode = httpCode;
	}
	public WalrusBucketErrorMessageType(String bukkit, String code, String message, HttpResponseStatus status, String requestId, String hostId) {
		bucketName = bukkit;
		this.code = code;
		this.message = message;
		this.requestId = requestId;
		this.hostId = hostId;
		this.status = status;
	}
	
	public String toString() {
		return "BucketErrorMessage:" + message + bucketName;
	}
}

public class WalrusRedirectMessageType extends WalrusErrorMessageType {
	private String redirectUrl;
	
	def WalrusRedirectMessageType() {
		this.code = 301;
	}
	
	def WalrusRedirectMessageType(String redirectUrl) {
		this.redirectUrl = redirectUrl;
		this.code = 301;
	}
	
	public String toString() {
		return "WalrusRedirectMessage:" +  redirectUrl;
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
	}
}

public class ListAllMyBucketsType extends WalrusRequestType {
}


public class ListAllMyBucketsResponseType extends EucalyptusMessage {
	CanonicalUserType owner;
	ListAllMyBucketsList bucketList;
}

public class BucketListEntry extends EucalyptusData {
	String name;
	String creationDate;
	
	public BucketListEntry() {
	}
	
	public BucketListEntry(String name, String creationDate) {
		this.name = name;
		this.creationDate = creationDate;
	}
}

public class ListAllMyBucketsList extends EucalyptusData {
	ArrayList<BucketListEntry> buckets = new ArrayList<BucketListEntry>();
}

public class CreateBucketType extends WalrusRequestType {
	String bucket;
	AccessControlListType accessControlList;
	String locationConstraint;
	
	//For unit testing
	public CreateBucketType() {
	}
	
	public CreateBucketType(String bucket) {
		this.bucket = bucket;
	}
}

public class CreateBucketResponseType extends WalrusResponseType {
	String bucket;
}

public class DeleteBucketType extends WalrusDeleteType {
	String bucket;
}

public class DeleteBucketResponseType extends WalrusDeleteResponseType {
	Status status;
}

public class Status extends EucalyptusData {
	int code;
	String description;
}

public class WalrusDataRequestType extends WalrusRequestType {
	String bucket;
	String key;
	String randomKey;
	Boolean isCompressed;
	
	def WalrusDataRequestType() {
	}
	
	def WalrusDataRequestType(String bucket, String key) {
		this.bucket = bucket;
		this.key = key;
	}
}

public class WalrusDataResponseType extends WalrusResponseType {
	String etag;
	String lastModified;
	Long size;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	Integer errorCode;
	String contentType;
	String contentDisposition;  
}

public class WalrusDataGetRequestType extends WalrusDataRequestType {
	protected Channel channel;

	public Channel getChannel() {
		return channel;
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	def WalrusDataGetRequestType() {}
	
	def WalrusDataGetRequestType(String bucket, String key) {
		super(bucket, key);
	}
}

public class WalrusDataGetResponseType extends WalrusDataResponseType {

	def WalrusDataGetResponseType() {}
}

public class PutObjectResponseType extends WalrusDataResponseType {
}

public class PostObjectResponseType extends WalrusDataResponseType {
	String redirectUrl;
	Integer successCode;
	String location;
	String bucket;
	String key;
}

public class PutObjectInlineResponseType extends WalrusDataResponseType {
}

public class PutObjectType extends WalrusDataRequestType {
	String contentLength;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	AccessControlListType accessControlList = new AccessControlListType();
	String storageClass;
	String contentType;
	String contentDisposition;
	
	def PutObjectType() {}
}

public class PostObjectType extends WalrusDataRequestType {
	String contentLength;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	AccessControlListType accessControlList = new AccessControlListType();
	String storageClass;
	String successActionRedirect;
	Integer successActionStatus;
}

public class CopyObjectType extends WalrusRequestType {
	String sourceBucket;
	String sourceObject;
	String destinationBucket;
	String destinationObject;
	String metadataDirective;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	AccessControlListType accessControlList = new AccessControlListType();
	String copySourceIfMatch;
	String copySourceIfNoneMatch;
	Date copySourceIfModifiedSince;
	Date copySourceIfUnmodifiedSince;
}

public class CopyObjectResponseType extends WalrusDataResponseType {
}

public class MetaDataEntry extends EucalyptusData {
	String name;
	String value;
}

public class PutObjectInlineType extends WalrusDataRequestType {
	String contentLength;
	ArrayList<MetaDataEntry> metaData  = new ArrayList<MetaDataEntry>();
	AccessControlListType accessControlList = new AccessControlListType();
	String storageClass;
	String base64Data;
	String contentType;
	String contentDisposition;  
}

public class DeleteObjectType extends WalrusDeleteType {
	String bucket;
	String key;
}

public class DeleteObjectResponseType extends WalrusDeleteResponseType {
	String code;
	String description;
}

public class ListBucketType extends WalrusRequestType {
	String bucket;
	String prefix;
	String marker;
	String maxKeys;
	String delimiter;
}

public class ListBucketResponseType extends WalrusResponseType {
	String name;
	String prefix;
	String marker;
	String nextMarker;
	int maxKeys;
	String delimiter;
	boolean isTruncated;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	ArrayList<ListEntry> contents = new ArrayList<ListEntry>();
	ArrayList<PrefixEntry> commonPrefixes = new ArrayList<PrefixEntry>();
}

public class ListEntry extends EucalyptusData {
	String key;
	String lastModified;
	String etag;
	long size;
	CanonicalUserType owner;
	String storageClass;
}

public class PrefixEntry extends EucalyptusData {
	String prefix;
	
	def PrefixEntry() {}
	
	def PrefixEntry(String prefix) {
		this.prefix = prefix;
	}
}

public class SetBucketAccessControlPolicyType extends WalrusRequestType {
	String bucket;
	AccessControlListType accessControlList;
}

public class SetBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetObjectAccessControlPolicyType extends WalrusRequestType {
	String bucket;
	String key;
	AccessControlListType accessControlList;
}

public class SetObjectAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTBucketAccessControlPolicyType extends WalrusRequestType {
	String bucket;
	AccessControlPolicyType accessControlPolicy;
}

public class SetRESTBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTObjectAccessControlPolicyType extends WalrusRequestType {
	String bucket;
	String key;
	AccessControlPolicyType accessControlPolicy;
}

public class SetRESTObjectAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}


public class GetObjectType extends WalrusDataGetRequestType {
	Boolean getMetaData;
	Boolean getData;
	Boolean inlineData;
	Boolean deleteAfterGet;
	Boolean getTorrent;
	
	def GetObjectType() {
	}
	
	def GetObjectType(final String bucketName, final String key, final Boolean getData, final Boolean getMetaData, final Boolean inlineData) {
		super( bucketName, key );
		this.getData = getData;
		this.getMetaData = getMetaData;
		this.inlineData = inlineData;
	}
}

public class GetObjectResponseType extends WalrusDataGetResponseType {
	Status status;
	String base64Data;
}

public class GetObjectExtendedType extends WalrusDataGetRequestType {
	Boolean getData;
	Boolean getMetaData;
	Boolean inlineData;
	Long byteRangeStart;
	Long byteRangeEnd;
	Date ifModifiedSince;
	Date ifUnmodifiedSince;
	String ifMatch;
	String ifNoneMatch;
	Boolean returnCompleteObjectOnConditionFailure;
}

public class GetObjectExtendedResponseType extends WalrusDataResponseType {
	Status status;
}

public class GetBucketLocationType extends WalrusRequestType {
	String bucket;
}

public class GetBucketLocationResponseType extends WalrusResponseType {
	String locationConstraint;
}

public class GetBucketLoggingStatusType extends WalrusRequestType {
	String bucket;
}

public class GetBucketLoggingStatusResponseType extends WalrusResponseType {
}

public class SetBucketLoggingStatusType extends WalrusRequestType {
	String bucket;
}

public class SetBucketLoggingStatusResponseType extends WalrusResponseType {
}

public class UpdateWalrusConfigurationType extends WalrusRequestType {
	String bucketRootDirectory;
	Integer maxBucketsPerUser;
	Long maxBucketSize;
	Long imageCacheSize;
	Integer totalSnapshotSize;

	def UpdateWalrusConfigurationType() {}

	def UpdateWalrusConfigurationType(WalrusStateType walrusState) {
		this.bucketRootDirectory = walrusState.getBucketsRootDirectory();
		this.maxBucketsPerUser = walrusState.getMaxBucketsPerUser();
		this.maxBucketSize = walrusState.getMaxBucketSizeInMB();
		this.imageCacheSize = walrusState.getMaxCacheSizeInMB();
		this.totalSnapshotSize = walrusState.getSnapshotsTotalInGB();
	}
}

public class UpdateWalrusConfigurationResponseType extends WalrusResponseType {
}

public class GetDecryptedImageType extends WalrusDataGetRequestType {
}

public class GetDecryptedImageResponseType extends WalrusDataGetResponseType {
}

public class CheckImageType extends WalrusDataRequestType {
}

public class CheckImageResponseType extends WalrusDataResponseType {
	Boolean success;
}

public class CacheImageType extends WalrusDataRequestType {
}

public class CacheImageResponseType extends WalrusDataResponseType {
	Boolean success;
}

public class FlushCachedImageType extends WalrusDataRequestType {
	
	def FlushCachedImageType(final String bucket, final String key) {
		super(bucket, key);
	}
	
	def FlushCachedImageType() {}
}
public class FlushCachedImageResponseType extends WalrusDataResponseType {
}

public class StoreSnapshotType extends WalrusDataRequestType {
	String contentLength;
	String snapshotvgname;
	String snapshotlvname;
}

public class StoreSnapshotResponseType extends WalrusDataResponseType {
}

public class DeleteWalrusSnapshotType extends WalrusRequestType {
	String bucket;
	String key;
}

public class DeleteWalrusSnapshotResponseType extends WalrusResponseType {
}

public class GetWalrusSnapshotType extends WalrusDataRequestType {
}

public class GetWalrusSnapshotResponseType extends WalrusDataResponseType {
}
