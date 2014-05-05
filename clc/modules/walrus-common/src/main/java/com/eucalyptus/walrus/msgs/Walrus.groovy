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

@GroovyAddClassUUID
package com.eucalyptus.walrus.msgs

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.channel.Channel;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.walrus.WalrusBackend;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.StreamedBaseMessage;
import edu.ucsb.eucalyptus.msgs.ComponentMessageResponseType;
import edu.ucsb.eucalyptus.msgs.ComponentMessageType;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.StatEventRecord
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Status;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.KeyEntry;
import com.eucalyptus.storage.msgs.s3.Part
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.Principals
import org.jboss.netty.handler.stream.ChunkedInput;

@ComponentMessage(WalrusBackend.class)
public class WalrusResponseType extends BaseMessage {
	BucketLogData logData;
	HttpResponseStatus status;
	String statusMessage;
	
	def WalrusResponseType() {
	}
	
	public User getUser() {
	    return Principals.nobodyUser();
	}
	
	public BaseMessage getReply() {
		return null;
	}
}

@ComponentMessage(WalrusBackend.class)
public class WalrusStreamingResponseType extends StreamedBaseMessage {
	BucketLogData logData;
	def WalrusStreamingResponseType() {
	}
    public BaseMessage getReply() {
        return null;
    }
}

@ComponentMessage(WalrusBackend.class)
public class WalrusRequestType extends BaseMessage {
	protected String accessKeyID;
	protected Date timeStamp;
	protected String signature;
	protected String credential;
	BucketLogData logData;
	protected String bucket;
	protected String key;

	public WalrusRequestType() {
	}

	public WalrusRequestType( String bucket, String key ) {
		this.bucket = bucket;
		this.key = key;
	}

	public WalrusRequestType(String accessKeyID, Date timeStamp, String signature, String credential) {
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

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}

public class WalrusDeleteType extends WalrusRequestType {
}

public class WalrusDeleteResponseType extends WalrusResponseType {
}

public class GetBucketAccessControlPolicyResponseType extends WalrusResponseType {
	AccessControlPolicy accessControlPolicy;
}

public class GetBucketAccessControlPolicyType extends WalrusRequestType { }

public class GetObjectAccessControlPolicyResponseType extends WalrusResponseType {
	AccessControlPolicy accessControlPolicy;
}

public class GetObjectAccessControlPolicyType extends WalrusRequestType {
	String versionId;
}

@ComponentMessage(WalrusBackend.class)
public class WalrusErrorMessageType extends BaseMessage {
	protected String message;
	String code;
	protected HttpResponseStatus status;
	protected String resourceType;
	protected String resource;
	protected String requestId;
	protected String hostId;
	BucketLogData logData;

	def WalrusErrorMessageType() {}

	def WalrusErrorMessageType(String message,
	String code,
	HttpResponseStatus status,
	String resourceType,
	String resource,
	String requestId,
	String hostId,
	BucketLogData logData) {
		this.message = message;
		this.code = code;
		this.status = status;
		this.resourceType = resourceType;
		this.resource = resource;
		this.requestId = requestId;
		this.hostId = hostId;
		this.logData = logData;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getResourceType() {
		return resourceType;
	}

	public String getResource() {
		return resource;
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


public class ListAllMyBucketsResponseType extends WalrusResponseType {
	CanonicalUser owner;
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

public class WalrusHeadRequestType extends WalrusRequestType {}
public class WalrusHeadResponseType extends WalrusResponseType {}

public class HeadBucketType extends WalrusHeadRequestType {}
public class HeadBucketResponseType extends WalrusHeadResponseType{}

public class CreateBucketType extends WalrusRequestType {
	AccessControlList accessControlList;
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

public class DeleteBucketType extends WalrusDeleteType {}

public class DeleteBucketResponseType extends WalrusDeleteResponseType {}

public class WalrusDataRequestType extends WalrusRequestType {
	String randomKey;
	Boolean isCompressed;

	def WalrusDataRequestType() {
	}

	def WalrusDataRequestType( String bucket, String key ) {
		super( bucket, key );
	}

}

public class WalrusDataResponseType extends WalrusStreamingResponseType {
	String etag;
	Date lastModified;
	Long size;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	Integer errorCode;
	String contentType;
	String contentDisposition;
	String versionId;

    public User getUser() {
        return Principals.nobodyUser();
    }
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
    List<ChunkedInput> dataInputStream;
    Long byteRangeStart;
    Long byteRangeEnd;

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
	AccessControlList accessControlList = new AccessControlList();
	String storageClass;
	String contentType;
	String contentDisposition;
	String contentMD5;

	def PutObjectType() {}
}

public class PostObjectType extends WalrusDataRequestType {
	String contentLength;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	AccessControlList accessControlList = new AccessControlList();
	String storageClass;
	String successActionRedirect;
	Integer successActionStatus;
	String contentType;
}
public class CopyObjectType extends WalrusRequestType {
	String sourceBucket;
	String sourceObject;
	String sourceVersionId;
	String destinationBucket;
	String destinationObject;
	String metadataDirective;
	ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
	AccessControlList accessControlList = new AccessControlList();
	String copySourceIfMatch;
	String copySourceIfNoneMatch;
	Date copySourceIfModifiedSince;
	Date copySourceIfUnmodifiedSince;
}

public class CopyObjectResponseType extends WalrusResponseType {
        String etag;
        String lastModified;
        Long size;
        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
        Integer errorCode;
        String contentType;
        String contentDisposition;
        String versionId;
        String copySourceVersionId;
}

public class PutObjectInlineType extends WalrusDataRequestType {
	String contentLength;
	ArrayList<MetaDataEntry> metaData  = new ArrayList<MetaDataEntry>();
	AccessControlList accessControlList = new AccessControlList();
	String storageClass;
	String base64Data;
	String contentType;
	String contentDisposition;
}

public class DeleteObjectType extends WalrusDeleteType {}

public class DeleteObjectResponseType extends WalrusDeleteResponseType {}

public class DeleteVersionType extends WalrusDeleteType {
	String versionid;
}

public class DeleteVersionResponseType extends WalrusDeleteResponseType {}

public class ListBucketType extends WalrusRequestType {
	String prefix;
	String marker;
	String maxKeys;
	String delimiter;

	def ListBucketType() {
		prefix = "";
		marker = "";
	}
}

public class ListBucketResponseType extends WalrusResponseType {
        String name;
        String prefix;
        String marker;
        String nextMarker;
        int maxKeys;
        String delimiter;
        boolean isTruncated;
        ArrayList<MetaDataEntry> metaData;
        ArrayList<ListEntry> contents;
        ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
}

public class ListVersionsType extends WalrusRequestType {
	String prefix;
	String keyMarker;
	String versionIdMarker;
	String maxKeys;
	String delimiter;

	def ListVersionsType() {
		prefix = "";
		keyMarker = "";
		versionIdMarker = "";
	}
}

public class ListVersionsResponseType extends WalrusResponseType {
	String name;
	String prefix;
	String keyMarker;
	String versionIdMarker;
	String nextKeyMarker;
	String nextVersionIdMarker;
	int maxKeys;
	String delimiter;
	boolean isTruncated;
	ArrayList<KeyEntry> keyEntries = new ArrayList<KeyEntry>();
	ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
}

public class SetBucketAccessControlPolicyType extends WalrusRequestType {
	AccessControlList accessControlList;
}

public class SetBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetObjectAccessControlPolicyType extends WalrusRequestType {
	AccessControlList accessControlList;
	String versionId;
}

public class SetObjectAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTBucketAccessControlPolicyType extends WalrusRequestType {
	AccessControlPolicy accessControlPolicy;
}

public class SetRESTBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTObjectAccessControlPolicyType extends WalrusRequestType {
	AccessControlPolicy accessControlPolicy;
	String versionId;
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
	String versionId;

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

public class GetObjectExtendedResponseType extends WalrusDataGetResponseType {
	Status status;
}

public class GetBucketLocationType extends WalrusRequestType {
}

public class GetBucketLocationResponseType extends WalrusResponseType {
	String locationConstraint;
}

public class GetBucketLoggingStatusType extends WalrusRequestType {
}

public class GetBucketLoggingStatusResponseType extends WalrusResponseType {
	LoggingEnabled loggingEnabled;
}

public class SetBucketLoggingStatusType extends WalrusRequestType {
	LoggingEnabled loggingEnabled;
}

public class SetBucketLoggingStatusResponseType extends WalrusResponseType {
}

public class GetBucketVersioningStatusType extends WalrusRequestType {
}

public class GetBucketVersioningStatusResponseType extends WalrusResponseType {
	String versioningStatus;
}

public class SetBucketVersioningStatusType extends WalrusRequestType {
	String versioningStatus;
}

public class SetBucketVersioningStatusResponseType extends WalrusResponseType {
}

public class AddObjectResponseType extends WalrusDataResponseType {
	
}

public class AddObjectType extends WalrusDataRequestType {
	String objectName;
	String etag;
	AccessControlList accessControlList = new AccessControlList();
}

public class UpdateWalrusConfigurationType extends WalrusRequestType {
	String name;
	ArrayList<ComponentProperty> properties;

	def UpdateWalrusConfigurationType() {}
}

public class UpdateWalrusConfigurationResponseType extends WalrusResponseType {
}

public class GetWalrusConfigurationType extends WalrusRequestType {
	String name;

	def GetWalrusConfigurationType() {}

	def GetWalrusConfigurationType(String name) {
		this.name = name;
	}
}

public class GetWalrusConfigurationResponseType extends WalrusRequestType {
	String name;
	ArrayList<ComponentProperty> properties;

	def GetWalrusConfigurationResponseType() {}
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

public class ValidateImageType extends WalrusRequestType {
}

public class ValidateImageResponseType extends WalrusResponseType {
}

public class StoreSnapshotType extends WalrusDataRequestType {
	String snapshotSize;
}

public class StoreSnapshotResponseType extends WalrusDataResponseType {
}

public class DeleteWalrusSnapshotType extends WalrusRequestType {
}

public class DeleteWalrusSnapshotResponseType extends WalrusResponseType {
}

public class GetWalrusSnapshotType extends WalrusDataGetRequestType {
}

public class GetWalrusSnapshotResponseType extends WalrusDataGetResponseType {
}

public class GetWalrusSnapshotSizeType extends WalrusDataGetRequestType {
}

public class WalrusComponentMessageType extends ComponentMessageType {
	@Override
	public String getComponent( ) {
		return "walrus";
	}
}

public class GetWalrusSnapshotSizeResponseType extends WalrusDataGetResponseType {
}

public class WalrusComponentMessageResponseType extends ComponentMessageResponseType {
}

public class WalrusUsageStatsRecord extends StatEventRecord {
	Long bytesIn;
	Long bytesOut;
	Integer numberOfBuckets;
	Long totalSpaceUsed;

	def WalrusUsageStatsRecord() {}

	def WalrusUsageStatsRecord(Long bytesIn,
	Long bytesOut,
	Integer numberOfBuckets,
	Long totalSpaceUsed) {
		super("WalrusBackend", System.getProperty("euca.version"));
		this.bytesIn = bytesIn;
		this.bytesOut = bytesOut;
		this.numberOfBuckets = numberOfBuckets;
		this.totalSpaceUsed = totalSpaceUsed;
	}

	public String toString() {
		return String.format("Service: %s Version: %s Bytes In: %s Bytes Out: %s Buckets: %d Space Used: %s",
		service,
		version,
		bytesIn,
		bytesOut,
		numberOfBuckets,
		totalSpaceUsed);
	}

	public static WalrusUsageStatsRecord create(Long bytesIn, Long bytesOut, Integer numberOfBuckets, Long totalSpaceUsed) {
		return new WalrusUsageStatsRecord(bytesIn, bytesOut, numberOfBuckets, totalSpaceUsed);
	}
}

public class InitiateMultipartUploadType extends WalrusDataRequestType {
    String cacheControl;
    String contentEncoding;
    String expires;
    ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String storageClass;
    String contentType;
}

public class InitiateMultipartUploadResponseType extends WalrusDataResponseType {
	String bucket;
	String key;
	String uploadId;
}

public class UploadPartType extends WalrusDataRequestType {
    String contentLength;
    String contentMD5
    String contentType;
    String expect;
    String uploadId;
    String partNumber;
}

public class UploadPartResponseType extends WalrusDataResponseType {
}

public class CompleteMultipartUploadType extends WalrusDataRequestType {
	ArrayList<Part> parts = new ArrayList<Part>();
	String uploadId; //Not in S3
}

public class CompleteMultipartUploadResponseType extends WalrusDataResponseType {
	String location;
	String bucket;
	String key;
	String etag;
}

public class AbortMultipartUploadType extends WalrusDataRequestType {
	String uploadId; //Not in S3
}

public class AbortMultipartUploadResponseType extends WalrusDataResponseType {
}

