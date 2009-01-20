package edu.ucsb.eucalyptus.msgs
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
    String accessKeyID;
    Date timeStamp;
    String signature;
    String credential;
    def WalrusRequestType() {}

    def WalrusRequestType(String accessKeyID, Date timeStamp, String signature, String credential) {
        this.accessKeyID = accessKeyID;
        this.timeStamp = timeStamp;
        this.signature = signature;
        this.credential = credential;
    }
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

public class AccessControlPolicyType extends EucalyptusData {
    AccessControlPolicyType() {}
    AccessControlPolicyType(CanonicalUserType owner, AccessControlListType acl) {
        this.owner = owner; this.accessControlList = acl;
    }

    CanonicalUserType owner;
    AccessControlListType accessControlList;
}

public class Grant extends EucalyptusData {
    //FIXME: handle other types of users
    CanonicalUserType grantee;
    String permission;

    public Grant() {}

    public Grant(CanonicalUserType grantee, String permission) {
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
    String bucket;
}

public class GetObjectAccessControlPolicyResponseType extends WalrusResponseType {
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

    public Integer getHttpCode() {
        return httpCode;
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
    public String toString() {
        return "BucketErrorMessage:" + message + bucketName;
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

public class DeleteBucketType extends WalrusRequestType {
    String bucket;
}

public class DeleteBucketResponseType extends WalrusResponseType {
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
}

public class PutObjectResponseType extends WalrusDataResponseType {
}

public class PutObjectInlineResponseType extends WalrusDataResponseType {
}

public class PutObjectType extends WalrusDataRequestType {
    String contentLength;
    ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlListType accessControlList = new AccessControlListType();
    String storageClass;
}

public class CopyObjectType extends WalrusDataRequestType {
    String sourceBucket;
    String sourceObject;
    String destinationBucket;
    String destinationObject;
    String metaDataDirective;
    ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlListType accessControlList = new AccessControlListType();
    String copyIfMatch;
    String copyIfNoneMatch;
    Date copyIfModifiedSince;
    Date copyIfUnmodifiedSince;
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
}

public class DeleteObjectType extends WalrusDataRequestType {
}

public class DeleteObjectResponseType extends WalrusResponseType {
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
    AccessControlPolicyType accessControlPolicy;
}

public class SetBucketAccessControlPolicyResponseType extends WalrusResponseType {
    String code;
    String description;
}

public class SetObjectAccessControlPolicyType extends WalrusRequestType {
    String bucket;
    String key;
    AccessControlPolicyType accessControlPolicy;
}

public class SetObjectAccessControlPolicyResponseType extends WalrusResponseType {
    String code;
    String description;
}

public class GetObjectType extends WalrusDataRequestType {
    Boolean getMetaData;
    Boolean getData;
    Boolean inlineData;

    def GetObjectType() {
    }

    def GetObjectType(final String bucketName, final String key, final Boolean getData, final Boolean getMetaData, final Boolean inlineData) {
        super( bucketName, key );
        this.getData = getData;
        this.getMetaData = getMetaData;
        this.inlineData = inlineData;
    }
}

public class GetObjectResponseType extends WalrusDataResponseType {
    Status status;
    String base64Data;
}

public class GetObjectExtendedType extends WalrusDataRequestType {
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
}

public class UpdateWalrusConfigurationResponseType extends WalrusResponseType {
}

public class GetDecryptedImageType extends WalrusDataRequestType {
}

public class GetDecryptedImageResponseType extends WalrusDataResponseType {
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

public class DeleteWalrusSnapshotType extends WalrusDataRequestType {
}

public class DeleteWalrusSnapshotResponseType extends WalrusDataResponseType {
}

public class GetSnapshotInfoType extends WalrusRequestType {
    String bucket;
    String key;
}

public class GetSnapshotInfoResponseType extends WalrusResponseType {
    ArrayList<String> snapshotSet = new ArrayList<String>();
}

public class GetSnapshotType extends WalrusDataRequestType {
}

public class GetSnapshotResponseType extends WalrusDataResponseType {
}
