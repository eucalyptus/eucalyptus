package edu.ucsb.eucalyptus.msgs
/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
import java.util.ArrayList;
import java.util.Date;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.channel.Channel;
import edu.ucsb.eucalyptus.cloud.BucketLogData;

/*
 *
 * Author: Neil Soman <neil@eucalyptus.com>
 */
public class WalrusResponseType extends EucalyptusMessage {
	BucketLogData logData;
	def WalrusResponseType() {}
}

public class WalrusRequestType extends EucalyptusMessage {
	protected String accessKeyID;
	protected Date timeStamp;
	protected String signature;
	protected String credential;
	BucketLogData logData;
	protected String bucket;
	protected String key;

	def WalrusRequestType() {}

  def WalrusRequestType( String bucket, String key ) {
    this.bucket = bucket;
    this.key = key;
  }

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

public class GetBucketAccessControlPolicyResponseType extends WalrusResponseType {
	AccessControlPolicyType accessControlPolicy;
}

public class GetBucketAccessControlPolicyType extends WalrusRequestType {
}

public class GetObjectAccessControlPolicyResponseType extends WalrusResponseType {
	AccessControlPolicyType accessControlPolicy;
}

public class GetObjectAccessControlPolicyType extends WalrusRequestType {
	String versionId;
}

public class WalrusErrorMessageType extends EucalyptusMessage {
	protected String message;
	protected String code;
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

	public String getCode() {
		return code;
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
}

public class DeleteBucketResponseType extends WalrusDeleteResponseType {
	Status status;
}

public class Status extends EucalyptusData {
	int code;
	String description;
}

public class WalrusDataRequestType extends WalrusRequestType {
	String randomKey;
	Boolean isCompressed;

	def WalrusDataRequestType() {
	}

  def WalrusDataRequestType( String bucket, String key ) {
    super( bucket, key );
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
	String versionId;
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
	AccessControlListType accessControlList = new AccessControlListType();
	String copySourceIfMatch;
	String copySourceIfNoneMatch;
	Date copySourceIfModifiedSince;
	Date copySourceIfUnmodifiedSince;
}

public class CopyObjectResponseType extends WalrusDataResponseType {
	String copySourceVersionId;
	String versionId;
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
}

public class DeleteObjectResponseType extends WalrusDeleteResponseType {
	String code;
	String description;
}

public class DeleteVersionType extends WalrusDeleteType {
	String versionid;
}

public class DeleteVersionResponseType extends WalrusDeleteResponseType {
	String code;
	String description;
}

public class ListBucketType extends WalrusRequestType {
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
	ArrayList<PrefixEntry> commonPrefixes;
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

public class ListVersionsType extends WalrusRequestType {
	String prefix;
	String keyMarker;
	String versionIdMarker;
	String maxKeys;
	String delimiter;
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
	ArrayList<VersionEntry> versions;
	ArrayList<DeleteMarkerEntry> deleteMarkers;
	ArrayList<PrefixEntry> commonPrefixes;
}

public class VersionEntry extends EucalyptusData {
	String key;
	String versionId;
	Boolean isLatest;
	String lastModified;
	String etag;
	long size;
	String storageClass;
	CanonicalUserType owner;
}

public class DeleteMarkerEntry extends EucalyptusData {
	String key;
	String versionId;
	Boolean isLatest;
	String lastModified;
	CanonicalUserType owner;
}

public class SetBucketAccessControlPolicyType extends WalrusRequestType {
	AccessControlListType accessControlList;
}

public class SetBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetObjectAccessControlPolicyType extends WalrusRequestType {
	AccessControlListType accessControlList;
	String versionId;
}

public class SetObjectAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTBucketAccessControlPolicyType extends WalrusRequestType {
	AccessControlPolicyType accessControlPolicy;
}

public class SetRESTBucketAccessControlPolicyResponseType extends WalrusResponseType {
	String code;
	String description;
}

public class SetRESTObjectAccessControlPolicyType extends WalrusRequestType {
	AccessControlPolicyType accessControlPolicy;
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

public class GetObjectExtendedResponseType extends WalrusDataResponseType {
	Status status;
}

public class GetBucketLocationType extends WalrusRequestType {
}

public class GetBucketLocationResponseType extends WalrusResponseType {
	String locationConstraint;
}

public class TargetGrants extends EucalyptusData {
	ArrayList<Grant> grants = new ArrayList<Grant>();

	def TargetGrants() {}
	def TargetGrants(List<Grant> grants) {
		this.grants = grants;
	}
}

public class LoggingEnabled extends EucalyptusData {
	String targetBucket;
	String targetPrefix;
	TargetGrants targetGrants;

	def LoggingEnabled() {}
	def LoggingEnabled(TargetGrants grants) {
		targetGrants = grants;
	}
	def LoggingEnabled(String bucket, String prefix, TargetGrants grants) {
		targetBucket = bucket;
		targetPrefix = prefix;
		targetGrants = grants;
	}
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
	AccessControlListType accessControlList = new AccessControlListType();	
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
		super("Walrus", System.getProperty("euca.version"));
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
