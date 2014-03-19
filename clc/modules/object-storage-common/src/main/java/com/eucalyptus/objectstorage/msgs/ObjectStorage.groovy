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

@GroovyAddClassUUID
package com.eucalyptus.objectstorage.msgs

import com.eucalyptus.auth.policy.PolicySpec
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.User
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.objectstorage.ObjectStorage
import com.eucalyptus.objectstorage.policy.AdminOverrideAllowed
import com.eucalyptus.objectstorage.policy.RequiresACLPermission
import com.eucalyptus.objectstorage.policy.RequiresPermission
import com.eucalyptus.objectstorage.policy.ResourceType
import com.eucalyptus.objectstorage.util.ObjectStorageProperties
import com.eucalyptus.storage.msgs.BucketLogData
import com.eucalyptus.storage.msgs.s3.AccessControlList
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy
import com.eucalyptus.storage.msgs.s3.CanonicalUser
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry
import com.eucalyptus.storage.msgs.s3.Initiator
import com.eucalyptus.storage.msgs.s3.KeyEntry
import com.eucalyptus.storage.msgs.s3.LifecycleConfiguration
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList
import com.eucalyptus.storage.msgs.s3.ListEntry
import com.eucalyptus.storage.msgs.s3.LoggingEnabled
import com.eucalyptus.storage.msgs.s3.MetaDataEntry
import com.eucalyptus.storage.msgs.s3.Part
import com.eucalyptus.storage.msgs.s3.Upload
import com.eucalyptus.util.ChannelBufferStreamingInputStream
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.ComponentMessageResponseType
import edu.ucsb.eucalyptus.msgs.ComponentMessageType
import edu.ucsb.eucalyptus.msgs.ComponentProperty
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import edu.ucsb.eucalyptus.msgs.StreamedBaseMessage
import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.HttpResponseStatus

public class ObjectStorageResponseType extends ObjectStorageRequestType {
    HttpResponseStatus status; //Most should be 200-ok, but for deletes etc it may be 204-No Content

    def ObjectStorageResponseType() {}
}

public class ObjectStorageStreamingResponseType extends StreamedBaseMessage {
    BucketLogData logData;

    def ObjectStorageStreamingResponseType() {}
}

@ComponentMessage(ObjectStorage.class)
public class ObjectStorageRequestType extends BaseMessage {
    protected String accessKeyID;
    protected String securityToken;
    protected Date timeStamp;
    protected String signature;
    protected String credential;
    BucketLogData logData;
    protected String bucket;
    protected String key;

    public ObjectStorageRequestType() {}

    public ObjectStorageRequestType(String bucket, String key) {
        this.bucket = bucket;
        this.key = key;
    }

    public ObjectStorageRequestType(String accessKeyID, Date timeStamp, String signature, String credential) {
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

    String getSecurityToken() {
        return securityToken;
    }

    void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
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

    public void setTimestamp(Date stamp) {
        this.timeStamp = stamp;
    }

    public Date getTimestamp() {
        return this.timeStamp;
    }

    public User getUser() {
        return Principals.nobodyUser();
    }

    public String getFullResource() {
        return this.bucket + "/" + this.key;
    }
}

public class ObjectStorageDataRequestType extends ObjectStorageRequestType {
    String randomKey;
    Boolean isCompressed;
    ChannelBufferStreamingInputStream data;
    boolean isChunked;
    boolean expectHeader; //is 100-continue expected by the client

    def ObjectStorageDataRequestType() {
    }

    def ObjectStorageDataRequestType(String bucket, String key) {
        super(bucket, key);
    }

}

public class ObjectStorageDataResponseType extends ObjectStorageStreamingResponseType {
    String etag;
    Date lastModified;
    Long size;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    Integer errorCode;
    String contentType;
    String contentDisposition;
    String versionId;
}

public class ObjectStorageDataGetRequestType extends ObjectStorageDataRequestType {
    protected Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    def ObjectStorageDataGetRequestType() {}

    def ObjectStorageDataGetRequestType(String bucket, String key) {
        super(bucket, key);
    }
}

public class ObjectStorageDataGetResponseType extends ObjectStorageDataResponseType {
    InputStream dataInputStream; //Stream to read data from to then write to wire
    Long byteRangeStart;
    Long byteRangeEnd;

    def ObjectStorageDataGetResponseType() {}
}

public class ObjectStorageErrorMessageType extends BaseMessage {
    protected String message;
    protected String code;
    protected HttpResponseStatus status;
    protected String resourceType;
    protected String resource;
    protected String requestId;
    protected String hostId;
    BucketLogData logData;

    def ObjectStorageErrorMessageType() {}

    def ObjectStorageErrorMessageType(String message,
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

    protected void setCode(final String code) {
        this.code = code
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

public class ObjectStorageRedirectMessageType extends ObjectStorageErrorMessageType {
    private String redirectUrl;

    def ObjectStorageRedirectMessageType() {
        this.code = 301;
    }

    def ObjectStorageRedirectMessageType(String redirectUrl) {
        this.redirectUrl = redirectUrl;
        this.code = 301;
    }

    public String toString() {
        return "WalrusRedirectMessage:" + redirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}

/* GET /bucket?acl */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETBUCKETACL])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.READ_ACP])
public class GetBucketAccessControlPolicyType extends ObjectStorageRequestType {}

public class GetBucketAccessControlPolicyResponseType extends ObjectStorageResponseType {
    AccessControlPolicy accessControlPolicy;
}

/* GET /bucket/object?acl */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETOBJECTACL])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.READ_ACP], bucket = [])
public class GetObjectAccessControlPolicyType extends ObjectStorageRequestType {
    String versionId;
}

public class GetObjectAccessControlPolicyResponseType extends ObjectStorageResponseType {
    AccessControlPolicy accessControlPolicy;
}

/* GET / on service. Lists buckets*/

/*
 * This is an exception rather than the rule for IAM checks. For implementing this
 * we must create a fake bucket entry that will pass the ACL checks, leaving only
 * the IAM checks. This is weird because the request isn't for a specific resource, but
 * IAM requires a check of a permission against a specific resourceId.
 */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTALLMYBUCKETS])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class ListAllMyBucketsType extends ObjectStorageRequestType {}

public class ListAllMyBucketsResponseType extends ObjectStorageResponseType {
    CanonicalUser owner;
    ListAllMyBucketsList bucketList;
}

/* HEAD /bucket */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTBUCKET])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.READ])
public class HeadBucketType extends ObjectStorageRequestType {}

public class HeadBucketResponseType extends ObjectStorageResponseType {}

/* PUT /bucket */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_CREATEBUCKET])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
//No ACLs for creating a bucket, weird like ListAllMyBuckets.
public class CreateBucketType extends ObjectStorageRequestType {
    AccessControlList accessControlList;
    String locationConstraint;

    //For unit testing
    public CreateBucketType() {}

    public CreateBucketType(String bucket) {
        this.bucket = bucket;
    }

    public AccessControlList getAccessControlList() {
        return this.accessControlList;
    }

    public void setAccessControlList(AccessControlList accessControlList) {
        this.accessControlList = accessControlList;
    }

    public String getLocationConstraint() {
        return this.locationConstraint;
    }

    public void setLocationConstraint(String locationConstraint) {
        this.locationConstraint = locationConstraint;
    }
}

public class CreateBucketResponseType extends ObjectStorageResponseType {
    String bucket;
}

/* DELETE /bucket */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_DELETEBUCKET])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
//No ACLs for deleting a bucket, only owner account can delete
public class DeleteBucketType extends ObjectStorageRequestType {}

public class DeleteBucketResponseType extends ObjectStorageResponseType {}

/* PUT /bucket/object */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
//Account must have write access to the bucket
public class PutObjectType extends ObjectStorageDataRequestType {
    String contentLength;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String storageClass;
    String contentType;
    String contentDisposition;
    String contentMD5;

    def PutObjectType() {}
}

public class PutObjectResponseType extends ObjectStorageDataResponseType {}

/* POST /bucket/object */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
public class PostObjectType extends ObjectStorageDataRequestType {
    String contentLength;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String storageClass;
    String successActionRedirect;
    Integer successActionStatus;
    String contentType;
}

public class PostObjectResponseType extends ObjectStorageDataResponseType {
    String redirectUrl;
    Integer successCode;
    String location;
    String bucket;
    String key;
}

/* GET /bucket/object */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.READ], bucket = [])
public class GetObjectType extends ObjectStorageDataGetRequestType {
    Boolean getMetaData;
    Boolean inlineData;
    Boolean deleteAfterGet;
    Boolean getTorrent;
    String versionId;

    def GetObjectType() {
    }

    def GetObjectType(final String bucketName, final String key, final Boolean getMetaData, final Boolean inlineData) {
        super(bucketName, key);
        this.getMetaData = getMetaData;
        this.inlineData = inlineData;
    }
}

public class GetObjectResponseType extends ObjectStorageDataGetResponseType {
    String base64Data; //In-line data response if requested
}

/* GET /bucket/object */

//TODO: zhill -- remove this request type and fold into regular GetObject
@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.READ], bucket = [])
public class GetObjectExtendedType extends ObjectStorageDataGetRequestType {
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

public class GetObjectExtendedResponseType extends ObjectStorageDataGetResponseType {
}

/* PUT /bucket/object with x-amz-copy-src header */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
//TODO: need to add support for both, refactor annotation into a set of k,v pairs
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.READ], bucket = [ObjectStorageProperties.Permission.WRITE])
public class CopyObjectType extends ObjectStorageRequestType {
    String sourceBucket;
    String sourceObject;
    String sourceVersionId;
    String destinationBucket;
    String destinationObject;
    String metadataDirective;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String copySourceIfMatch;
    String copySourceIfNoneMatch;
    Date copySourceIfModifiedSince;
    Date copySourceIfUnmodifiedSince;
}

/* HEAD /bucket/object */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_HEADOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.READ], bucket = [])
public class HeadObjectType extends ObjectStorageDataGetRequestType {
    String versionId;

    def HeadObjectType() {
    }

    def HeadObjectType(final String bucketName, final String key) {
        super(bucketName, key);
    }
}

public class HeadObjectResponseType extends ObjectStorageDataResponseType {}

public class CopyObjectResponseType extends ObjectStorageResponseType {
    String etag;
    String lastModified;
    Long size;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    Integer errorCode;
    String contentType;
    String contentDisposition;
    String versionId;
    String copySourceVersionId;
}

//TODO: REMOVE THIS
/* SOAP put object */

public class PutObjectInlineType extends ObjectStorageDataRequestType {
    String contentLength;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String storageClass;
    String base64Data;
    String contentType;
    String contentDisposition;
}

public class PutObjectInlineResponseType extends ObjectStorageDataResponseType {}

/* DELETE /bucket/object */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_DELETEOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
public class DeleteObjectType extends ObjectStorageRequestType {}

public class DeleteObjectResponseType extends ObjectStorageResponseType {}

/* DELETE /bucket/object?versionid=x */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_DELETEOBJECTVERSION])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class DeleteVersionType extends ObjectStorageRequestType {
    String versionId;
}

public class DeleteVersionResponseType extends ObjectStorageResponseType {}

/* GET /bucket */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTBUCKET])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.READ])
public class ListBucketType extends ObjectStorageRequestType {
    String prefix;
    String marker;
    String maxKeys;
    String delimiter;

    def ListBucketType() {
        prefix = "";
        marker = "";
        //delimiter = "";
    }
}

public class ListBucketResponseType extends ObjectStorageResponseType {
    String name;
    String prefix;
    String marker;
    String nextMarker;
    int maxKeys;
    String delimiter;
    boolean isTruncated;
    ArrayList<ListEntry> contents;
    ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
}

/* GET /bucket?versions */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTBUCKETVERSIONS])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class ListVersionsType extends ObjectStorageRequestType {
    String prefix;
    String keyMarker;
    String versionIdMarker;
    String maxKeys;
    String delimiter;

    def ListVersionsType() {
        prefix = "";
    }
}

public class ListVersionsResponseType extends ObjectStorageResponseType {
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

/* PUT /bucket?acl */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTBUCKETACL])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE_ACP])
public class SetBucketAccessControlPolicyType extends ObjectStorageRequestType {
    AccessControlPolicy accessControlPolicy;
}

public class SetBucketAccessControlPolicyResponseType extends ObjectStorageResponseType {}

/* PUT /bucket/object?acl */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECTACL])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [ObjectStorageProperties.Permission.WRITE_ACP], bucket = [])
public class SetObjectAccessControlPolicyType extends ObjectStorageRequestType {
    AccessControlPolicy accessControlPolicy;
    String versionId;
}

public class SetObjectAccessControlPolicyResponseType extends ObjectStorageResponseType {}

/* GET /bucket?location */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETBUCKETLOCATION])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class GetBucketLocationType extends ObjectStorageRequestType {}

public class GetBucketLocationResponseType extends ObjectStorageResponseType {
    String locationConstraint;
}

/* GET /bucket?logging */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETBUCKETLOGGING])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class GetBucketLoggingStatusType extends ObjectStorageRequestType {}

public class GetBucketLoggingStatusResponseType extends ObjectStorageResponseType {
    LoggingEnabled loggingEnabled;
}

/* PUT /bucket?logging */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTBUCKETLOGGING])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class SetBucketLoggingStatusType extends ObjectStorageRequestType {
    LoggingEnabled loggingEnabled;
}

public class SetBucketLoggingStatusResponseType extends ObjectStorageResponseType {}

/* GET /bucket?versioning */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETBUCKETVERSIONING])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class GetBucketVersioningStatusType extends ObjectStorageRequestType {}

public class GetBucketVersioningStatusResponseType extends ObjectStorageResponseType {
    String versioningStatus;

    private boolean isNotDisabled() {
        return versioningStatus != null && "disabled" != versioningStatus.toLowerCase();
    }
}

/* PUT /bucket?versioning */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTBUCKETVERSIONING])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class SetBucketVersioningStatusType extends ObjectStorageRequestType {
    String versioningStatus;
}

public class SetBucketVersioningStatusResponseType extends ObjectStorageResponseType {}

/* GET /bucket?lifecycle */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_GETLIFECYCLECONFIGURATION])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class GetBucketLifecycleType extends ObjectStorageRequestType {}

public class GetBucketLifecycleResponseType extends ObjectStorageResponseType {
    LifecycleConfiguration lifecycleConfiguration;
}

/* PUT /bucket?lifecycle */

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTLIFECYCLECONFIGURATION])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class SetBucketLifecycleType extends ObjectStorageRequestType {
    LifecycleConfiguration lifecycleConfiguration;
}

public class SetBucketLifecycleResponseType extends ObjectStorageResponseType {}

/* DELETE /bucket?lifecycle */

@AdminOverrideAllowed
/* according to docs, this is the appropriate permission, as of Jan 7, 2013 */
@RequiresPermission([PolicySpec.S3_PUTLIFECYCLECONFIGURATION])
@ResourceType(PolicySpec.S3_RESOURCE_BUCKET)
@RequiresACLPermission(object = [], bucket = [], ownerOnly = true)
public class DeleteBucketLifecycleType extends ObjectStorageRequestType {}

public class DeleteBucketLifecycleResponseType extends ObjectStorageResponseType {}

public class AddObjectResponseType extends ObjectStorageDataResponseType {}

public class AddObjectType extends ObjectStorageDataRequestType {
    String objectName;
    String etag;
    AccessControlList accessControlList = new AccessControlList();
}

public class UpdateObjectStorageConfigurationType extends ObjectStorageRequestType {
    String name;
    ArrayList<ComponentProperty> properties;

    def UpdateObjectStorageConfigurationType() {}
}

public class UpdateObjectStorageConfigurationResponseType extends ObjectStorageResponseType {}

public class GetObjectStorageConfigurationType extends ObjectStorageRequestType {
    String name;

    def GetObjectStorageConfigurationType() {}

    def GetObjectStorageConfigurationType(String name) {
        this.name = name;
    }
}

public class GetObjectStorageConfigurationResponseType extends ObjectStorageRequestType {
    String name;
    ArrayList<ComponentProperty> properties;

    def GetObjectStorageConfigurationResponseType() {}
}

public class ObjectStorageComponentMessageType extends ComponentMessageType {
    @Override
    public String getComponent() {
        return "objectstorage";
    }
}

public class ObjectStorageComponentMessageResponseType extends ComponentMessageResponseType {}


@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
//Account must have write access to the bucket
public class InitiateMultipartUploadType extends ObjectStorageDataRequestType {
    String cacheControl;
    String contentEncoding;
    String expires;
    List<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
    AccessControlList accessControlList = new AccessControlList();
    String storageClass;
    String contentType;
}

public class InitiateMultipartUploadResponseType extends ObjectStorageDataResponseType {
    String bucket;
    String key;
    String uploadId;
}

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
//Account must have write access to the bucket
public class UploadPartType extends ObjectStorageDataRequestType {
    String contentLength;
    String contentMD5
    String contentType;
    String expect;
    String uploadId;
    String partNumber;
}

public class UploadPartResponseType extends ObjectStorageDataResponseType {
}

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_PUTOBJECT])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
//Account must have write access to the bucket
public class CompleteMultipartUploadType extends ObjectStorageDataRequestType {
    ArrayList<Part> parts = new ArrayList<Part>();
    String uploadId;
}

public class CompleteMultipartUploadResponseType extends ObjectStorageDataResponseType {
    String location;
    String bucket;
    String key;
    String etag;
}

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_ABORTMULTIPARTUPLOAD])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.WRITE])
//Account must have write access to the bucket
public class AbortMultipartUploadType extends ObjectStorageDataRequestType {
    String uploadId;
}

public class AbortMultipartUploadResponseType extends ObjectStorageDataResponseType {
}

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTMULTIPARTUPLOADPARTS])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.READ])
//Account must have read access to the bucket
public class ListPartsType extends ObjectStorageDataRequestType {
    String uploadId;
    Integer maxParts;
    Integer partNumberMarker;
}

public class ListPartsResponseType extends ObjectStorageDataResponseType {
    String bucket;
    String key;
    String uploadId;
    Initiator initiator;
    CanonicalUser owner;
    String storageClass;
    Integer partNumberMarker;
    Integer nextPartNumberMarker;
    Integer maxParts;
    Boolean isTruncated;
    ArrayList<Part> parts = new ArrayList<Part>();
}

@AdminOverrideAllowed
@RequiresPermission([PolicySpec.S3_LISTBUCKETMULTIPARTUPLOADS])
@ResourceType(PolicySpec.S3_RESOURCE_OBJECT)
@RequiresACLPermission(object = [], bucket = [ObjectStorageProperties.Permission.READ])
//Account must have read access to the bucket
public class ListMultipartUploadsType extends ObjectStorageDataRequestType {
    String delimiter;
    Integer maxUploads;
    String keyMarker;
    String prefix;
    String uploadIdMarker;
}

public class ListMultipartUploadsResponseType extends ObjectStorageDataResponseType {
    String bucket;
    String keyMarker;
    String uploadIdMarker;
    String nextKeyMarker;
    String nextUploadIdMarker;
    Integer maxUploads;
    Boolean isTruncated;
    List<Upload> uploads = new ArrayList<Upload>();
    String prefix;
    String delimiter;
    ArrayList<CommonPrefixesEntry> commonPrefixes = new ArrayList<CommonPrefixesEntry>();
}
