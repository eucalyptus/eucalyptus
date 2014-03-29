package com.eucalyptus.objectstorage.providers;

import com.eucalyptus.objectstorage.exceptions.s3.BadDigestException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidPartOrderException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchUploadException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType;
import com.eucalyptus.objectstorage.msgs.HeadBucketType;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.HeadObjectType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsResponseType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsType;
import com.eucalyptus.objectstorage.msgs.ListPartsResponseType;
import com.eucalyptus.objectstorage.msgs.ListPartsType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Initiator;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.Upload;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * This is a fake provider for testing-purposes only. This is a stateful in-memory
 * provider that will respond like a regular provider but with no external calls
 * or dependencies. Keeps track of buckets and objects in memory. Objects keep the
 * data sent, so be careful about testing with large objects.
 * <p/>
 * Useful for cases where specifying the provider via a mock interface, like jmock
 * is overly complicated due to large api coverage.
 */
public class InMemoryProvider implements ObjectStorageProviderClient {
    private static final Logger LOG = Logger.getLogger(InMemoryProvider.class);

    static final String IN_MEMORY_USERNAME = "memoryprovider@testunit.com";

    public static enum FAIL_TYPE {
        NOT_FOUND,
        INTERNAL_ERROR,
        NONE
    }

    //Public knobs for testing failures
    public static FAIL_TYPE failObjectPut = FAIL_TYPE.NONE;
    public static FAIL_TYPE failObjectGet = FAIL_TYPE.NONE;
    public static FAIL_TYPE failObjectDelete = FAIL_TYPE.NONE;
    public static FAIL_TYPE failBucketPut = FAIL_TYPE.NONE;
    public static FAIL_TYPE failBucketGet = FAIL_TYPE.NONE;
    public static FAIL_TYPE failBucketDelete = FAIL_TYPE.NONE;
    public static FAIL_TYPE failCopyObject = FAIL_TYPE.NONE;

    private class ObjectKey implements Comparable {
        String key = "";
        String versionId = "null";

        public ObjectKey(String k, String version) {
            this.key = k;
            this.versionId = version;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ObjectKey) {
                ObjectKey otherKey = (ObjectKey) other;
                return otherKey.key.equals(key) && otherKey.versionId.equals(versionId);
            } else {
                return false;
            }
        }

        @Override
        public int compareTo(Object o) {
            ObjectKey other = (ObjectKey) o;
            return (this.key + this.versionId).compareTo(other.key + other.versionId);
        }
    }

    private class MemoryBucket {
        String name;
        Date createdDate;
        AccessControlList acl;
        String canonicalId;
        ObjectStorageProperties.VersioningStatus versioningStatus;
        boolean loggingEnabled;
        String location;
        TreeMap<ObjectKey, MemoryObject> objects = new TreeMap<ObjectKey, MemoryObject>();
        TreeMap<String, MemoryMpu> uploads = new TreeMap<>();

        BucketListEntry toBucketListEntry() {
            return new BucketListEntry(this.name, DateFormatter.dateToListingFormattedString(this.createdDate));
        }
    }

    private class MemoryObject {
        String key;
        String versionId;
        long size;
        Date modifiedDate;
        byte[] content;
        AccessControlList acl;
        String canonicalId;
        String eTag;
        List<MetaDataEntry> userMetadata;

        public ListEntry toListEntry() {
            return new ListEntry(key, modifiedDate.toString(), eTag, size, new CanonicalUser(canonicalId, "user"), "STANDARD");
        }
    }

    private class MemoryMpu extends MemoryObject {
        String uploadId;
        TreeMap<Integer, MemoryPart> parts = new TreeMap<>();

        public Upload toUploadEntry() {
            Upload u = new Upload(this.key, this.uploadId, new Initiator(this.canonicalId, ""), new CanonicalUser(this.canonicalId, ""), "STANDARD", this.modifiedDate);
            return u;
        }
    }

    private class MemoryPart extends MemoryObject {
        Integer partNumber;

        public Part toPartEntry() {
            Part p = new Part();
            p.setPartNumber(this.partNumber);
            p.setEtag(this.eTag);
            p.setLastModified(this.modifiedDate);
            p.setSize(this.size);
            return p;
        }
    }

    private TreeMap<String, MemoryBucket> myBuckets = new TreeMap<String, MemoryBucket>();

    private String getOwnerCanonicalId(String accessKeyId) {
        return "one-user-canonicalid";
    }

    private MemoryBucket getBucket(String bucketName, String canonicalId) throws S3Exception {
        MemoryBucket b = this.myBuckets.get(bucketName);
        if (b == null) {
            throw new NoSuchBucketException(bucketName);
        }
        //Could add a canonicalId check here, but not needed yet.
        return b;
    }

    private MemoryObject getObject(String bucketName, String key, String canonicalId) throws S3Exception {
        MemoryBucket bucket = getBucket(bucketName, canonicalId);
        MemoryObject obj = bucket.objects.get(new ObjectKey(key, "null"));
        if (obj == null) {
            throw new NoSuchKeyException(key);
        }
        return obj;
    }

    @Override
    public void checkPreconditions() throws EucalyptusCloudException {
    }

    @Override
    public void initialize() throws EucalyptusCloudException {
    }

    @Override
    public void check() throws EucalyptusCloudException {
    }

    @Override
    public void start() throws EucalyptusCloudException {
    }

    @Override
    public void stop() throws EucalyptusCloudException {
        //Flush everything on stop
        myBuckets.clear();
    }

    @Override
    public void enable() throws EucalyptusCloudException {
    }

    @Override
    public void disable() throws EucalyptusCloudException {
    }

    @Override
    public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws S3Exception {
        ListAllMyBucketsResponseType response = request.getReply();
        ListAllMyBucketsList list = new ListAllMyBucketsList();
        list.setBuckets(new ArrayList<BucketListEntry>());
        response.setBucketList(list);
        response.setOwner(new CanonicalUser(request.getAccessKeyID(), ""));
        for (Map.Entry<String, MemoryBucket> bucketEntry : this.myBuckets.entrySet()) {
            list.getBuckets().add(bucketEntry.getValue().toBucketListEntry());
        }

        return response;
    }

    @Override
    public HeadBucketResponseType headBucket(HeadBucketType request) throws S3Exception {
        switch (failBucketGet) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
        HeadBucketResponseType response = request.getReply();
        response.setBucket(request.getBucket());
        response.setStatus(HttpResponseStatus.OK);
        response.setStatusMessage("OK");
        return response;
    }

    private AccessControlList genPrivateAcl(String canonicalid) {
        AccessControlList privateAcl = new AccessControlList();
        privateAcl.setGrants(new ArrayList<Grant>());
        privateAcl.getGrants().add(new Grant(new Grantee(new CanonicalUser(canonicalid, IN_MEMORY_USERNAME)), ObjectStorageProperties.Permission.FULL_CONTROL.toString()));
        return privateAcl;
    }


    @Override
    public CreateBucketResponseType createBucket(CreateBucketType request) throws S3Exception {
        CreateBucketResponseType response = request.getReply();
        MemoryBucket bucket;

        switch (failBucketPut) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }
        try {
            bucket = getBucket(request.getBucket(), request.getAccessKeyID());
        } catch (NoSuchBucketException e) {
            //Create
            bucket = null;
        }

        if (bucket == null) {
            bucket = new MemoryBucket();
            bucket.name = request.getBucket();
            bucket.canonicalId = getOwnerCanonicalId(request.getAccessKeyID());
            bucket.createdDate = new Date();
            bucket.acl = request.getAccessControlList();
            if (request.getAccessControlList() == null) {
                bucket.acl = genPrivateAcl(bucket.canonicalId);
            }
            bucket.location = request.getLocationConstraint();
            bucket.loggingEnabled = false;
            this.myBuckets.put(request.getBucket(), bucket);
        }

        response.setStatus(HttpResponseStatus.OK);
        response.setStatusMessage("OK");
        response.setBucket(request.getBucket());
        response.setTimestamp(new Date());
        return response;
    }

    @Override
    public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws S3Exception {
        switch (failBucketDelete) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        DeleteBucketResponseType response = request.getReply();
        response.setStatus(HttpResponseStatus.NO_CONTENT);
        response.setStatusMessage("NoContent");

        try {
            MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
            if (b != null) {
                //Do the delete
                if (b.objects.size() > 0) {
                    throw new BucketNotEmptyException(request.getBucket());
                } else {
                    this.myBuckets.remove(b.name);
                }
            }
        } catch (NoSuchBucketException e) {
            //fall thru
        }
        return response;
    }

    @Override
    public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
            GetBucketAccessControlPolicyType request) throws S3Exception {
        GetBucketAccessControlPolicyResponseType response = request.getReply();
        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
        response.setAccessControlPolicy(new AccessControlPolicy());
        response.getAccessControlPolicy().setAccessControlList(b.acl);
        response.getAccessControlPolicy().setOwner(new CanonicalUser(b.canonicalId, IN_MEMORY_USERNAME));
        response.setBucket(request.getBucket());
        return response;
    }

    @Override
    public ListBucketResponseType listBucket(ListBucketType request) throws S3Exception {
        switch (failBucketGet) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        /*
        Does not yet support prefix, delim, pagination, etc
         */

        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
        ListBucketResponseType response = request.getReply();

        response.setContents(new ArrayList<ListEntry>());
        for (MemoryObject obj : b.objects.values()) {
            response.getContents().add(obj.toListEntry());
        }

        response.setDelimiter("");
        response.setMarker("");
        response.setIsTruncated(false);
        response.setMaxKeys(1000);
        response.setName(b.name);
        return response;
    }

    @Override
    public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
            SetBucketAccessControlPolicyType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request)
            throws S3Exception {
        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
        GetBucketLocationResponseType response = request.getReply();
        response.setBucket(request.getBucket());
        response.setLocationConstraint(b.location);
        return response;
    }

    @Override
    public SetBucketLoggingStatusResponseType setBucketLoggingStatus(SetBucketLoggingStatusType request)
            throws S3Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GetBucketLoggingStatusResponseType getBucketLoggingStatus(GetBucketLoggingStatusType request)
            throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request)
            throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public SetBucketVersioningStatusResponseType setBucketVersioningStatus(SetBucketVersioningStatusType request)
            throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public ListVersionsResponseType listVersions(ListVersionsType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public PutObjectResponseType putObject(PutObjectType request, InputStream inputData)
            throws S3Exception {
        LOG.debug("InMemory PutObject");
        switch (failObjectPut) {
            case INTERNAL_ERROR:
                LOG.debug("InMemory PutObject throw internal error as specified");
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                LOG.debug("InMemory PutObject throe not-found as specified");
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        try {
            ObjectKey key = new ObjectKey(request.getKey(), "null");
            MemoryBucket bucket = getBucket(request.getBucket(), request.getAccessKeyID());

            MemoryObject memObj = new MemoryObject();
            memObj.key = request.getKey();
            memObj.versionId = "null";
            memObj.content = new byte[Integer.valueOf(request.getContentLength())];
            memObj.modifiedDate = new Date();
            memObj.canonicalId = getOwnerCanonicalId(request.getAccessKeyID());
            memObj.acl = request.getAccessControlList();
            memObj.userMetadata = request.getMetaData();

            if (request.getAccessControlList() == null) {
                memObj.acl = genPrivateAcl(memObj.canonicalId);
            }

            try {
                int readLength = inputData.read(memObj.content);
                memObj.size = readLength;
            } catch (IOException e) {
                LOG.debug("InMemory PutObject exception: ", e);
                throw new EucalyptusCloudException(e);
            }

            memObj.eTag = DigestUtils.md5Hex(new String(memObj.content));
            if (!Strings.isNullOrEmpty(request.getContentMD5()) && !memObj.eTag.equals(request.getContentMD5())) {
                LOG.error("InMemory PutObject MD5 mismatch");
                throw new BadDigestException(memObj.eTag);
            } else {
                //Do the put, replacing any previous object
                bucket.objects.put(key, memObj);
            }
            PutObjectResponseType response = request.getReply();
            response.setContentType(request.getContentType());
            response.setEtag(memObj.eTag);
            response.setVersionId("null");
            response.setContentDisposition(request.getContentDisposition());
            response.setLastModified(new Date());
            response.setStatusMessage("OK");
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory PutObject exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public PostObjectResponseType postObject(PostObjectType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws S3Exception {
        switch (failObjectDelete) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getKey());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchKeyException(request.getKey());
            default:
        }
        DeleteObjectResponseType response = request.getReply();
        response.setStatusMessage("NoContent");
        response.setStatus(HttpResponseStatus.NO_CONTENT);

        try {
            MemoryObject obj = getObject(request.getBucket(), request.getKey(), request.getAccessKeyID());
            if (obj != null) {
                MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
                b.objects.remove(new ObjectKey(request.getKey(), obj.versionId));
            }
        } catch (NoSuchKeyException e) {
            //Fall thru
        } catch (S3Exception e) {
            throw e;
        }
        return response;
    }

    @Override
    public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
            GetObjectAccessControlPolicyType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
            SetObjectAccessControlPolicyType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public GetObjectResponseType getObject(GetObjectType request) throws S3Exception {
        switch (failObjectGet) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getKey());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchKeyException(request.getKey());
            default:
        }
        MemoryObject obj = getObject(request.getBucket(), request.getKey(), request.getAccessKeyID());
        GetObjectResponseType response = request.getReply();
        response.setEtag(obj.eTag);
        response.setLastModified(obj.modifiedDate);
        response.setSize(obj.size);
        response.setVersionId(obj.versionId);
        response.setDataInputStream(new ByteArrayInputStream(obj.content));
        response.setStatusMessage("OK");
        response.setMetaData(obj.userMetadata);
        return response;
    }

    @Override
    public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request)
            throws S3Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeadObjectResponseType headObject(HeadObjectType request) throws S3Exception {
        switch (failObjectGet) {
            case INTERNAL_ERROR:
                throw new InternalErrorException(request.getKey());
            case NOT_FOUND:
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchKeyException(request.getKey());
            default:
        }
        MemoryObject obj = getObject(request.getBucket(), request.getKey(), request.getAccessKeyID());
        HeadObjectResponseType response = request.getReply();
        response.setEtag(obj.eTag);
        response.setLastModified(obj.modifiedDate);
        response.setSize(obj.size);
        response.setVersionId(obj.versionId);
        response.setStatusMessage("OK");
        response.setMetaData(obj.userMetadata);
        return response;
    }

    @Override
    public CopyObjectResponseType copyObject(CopyObjectType request) throws S3Exception {
        LOG.debug("InMemory CopyObject");
        switch (failCopyObject) {
            case INTERNAL_ERROR:
                LOG.debug("InMemory CopyObject throw internal error as specified");
                throw new InternalErrorException(request.getSourceBucket());
            case NOT_FOUND:
                LOG.debug("InMemory CopyObject throw not-found as specified");
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getSourceBucket());
            default:
        }

        try {
            MemoryBucket sourceBucket = getBucket(request.getSourceBucket(), request.getAccessKeyID());
            MemoryBucket destBucket = getBucket(request.getDestinationBucket(), request.getAccessKeyID());
            MemoryObject sourceObject = getObject(sourceBucket.name, request.getSourceObject(),
                    getOwnerCanonicalId(request.getAccessKeyID()));

            MemoryObject destObject = new MemoryObject();
            destObject.key = request.getDestinationObject();
            destObject.versionId = "null";
            destObject.modifiedDate = new Date();
            destObject.canonicalId = getOwnerCanonicalId(request.getAccessKeyID());
            destObject.acl = request.getAccessControlList();
            if (request.getAccessControlList() == null) {
                destObject.acl = genPrivateAcl(destObject.canonicalId);
            }
            destObject.size = sourceObject.size;
            destObject.content = sourceObject.content.clone();

            destObject.eTag = DigestUtils.md5Hex(new String(destObject.content));

            ObjectKey destKey = new ObjectKey(destObject.key, "null");
            destBucket.objects.put(destKey, destObject);

            CopyObjectResponseType response = request.getReply();
            response.setEtag(destObject.eTag);
            response.setVersionId("null");
            response.setLastModified(new Date().toString());
            response.setStatusMessage("OK");
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory PutObject exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public DeleteVersionResponseType deleteVersion(DeleteVersionType request) throws S3Exception {
        // TODO Auto-generated method stub
        throw new NotImplementedException();
    }

    @Override
    public InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws S3Exception {
        LOG.debug("InMemory InitiateMPU");
        switch (failObjectPut) {
            case INTERNAL_ERROR:
                LOG.debug("InMemory MPU throw internal error as specified");
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                LOG.debug("InMemory MPU throw not-found as specified");
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        try {
            MemoryBucket bucket = getBucket(request.getBucket(), request.getAccessKeyID());

            MemoryMpu memObj = new MemoryMpu();
            memObj.key = request.getKey();
            memObj.uploadId = UUID.randomUUID().toString();
            memObj.versionId = "null";
            memObj.modifiedDate = new Date();
            memObj.canonicalId = getOwnerCanonicalId(request.getAccessKeyID());
            memObj.acl = request.getAccessControlList();
            if (request.getAccessControlList() == null) {
                memObj.acl = genPrivateAcl(memObj.canonicalId);
            }

            bucket.uploads.put(memObj.uploadId, memObj);
            InitiateMultipartUploadResponseType response = request.getReply();
            response.setUploadId(memObj.uploadId);
            response.setStatusMessage("OK");
            response.setBucket(request.getBucket());
            response.setKey(request.getKey());
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory PutObject exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public UploadPartResponseType uploadPart(UploadPartType request, InputStream dataContent) throws S3Exception {
        LOG.debug("InMemory UploadPart");
        switch (failObjectPut) {
            case INTERNAL_ERROR:
                LOG.debug("InMemory UploadPart throw internal error as specified");
                throw new InternalErrorException(request.getBucket());
            case NOT_FOUND:
                LOG.debug("InMemory UploadPart throw not-found as specified");
                //Yes, this doesn't really make sense, but for consistency it's here
                throw new NoSuchBucketException(request.getBucket());
            default:
        }

        try {
            MemoryBucket bucket = getBucket(request.getBucket(), request.getAccessKeyID());
            if (!bucket.uploads.containsKey(request.getUploadId())) {
                throw new NoSuchUploadException(request.getUploadId());
            }
            MemoryMpu memMpu = bucket.uploads.get(request.getUploadId());
            MemoryPart memObj = new MemoryPart();
            memObj.key = request.getKey();
            memObj.content = new byte[Integer.valueOf(request.getContentLength())];
            memObj.modifiedDate = new Date();
            memObj.canonicalId = getOwnerCanonicalId(request.getAccessKeyID());
            memObj.partNumber = Integer.parseInt(request.getPartNumber());

            try {
                memObj.size = dataContent.read(memObj.content);
            } catch (IOException e) {
                LOG.debug("InMemory UploadPart exception: ", e);
                throw new EucalyptusCloudException(e);
            }

            memObj.eTag = DigestUtils.md5Hex(new String(memObj.content));
            if (!Strings.isNullOrEmpty(request.getContentMD5()) && !memObj.eTag.equals(request.getContentMD5())) {
                LOG.error("InMemory UploadPart MD5 mismatch");
                throw new BadDigestException(memObj.eTag);
            } else {
                //Do the put, replacing any previous object
                memMpu.parts.put(memObj.partNumber, memObj);
            }
            UploadPartResponseType response = request.getReply();
            response.setContentType(request.getContentType());
            response.setEtag(memObj.eTag);
            response.setLastModified(new Date());
            response.setStatusMessage("OK");
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory UploadPart exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public CompleteMultipartUploadResponseType completeMultipartUpload(CompleteMultipartUploadType request) throws S3Exception {
        try {
            MemoryBucket bucket = getBucket(request.getBucket(), request.getAccessKeyID());
            if (!bucket.uploads.containsKey(request.getUploadId())) {
                throw new NoSuchUploadException(request.getUploadId());
            }
            //Remove the MPU
            MemoryMpu mpuParent = bucket.uploads.get(request.getUploadId());

            int lastNumber = -1;
            ArrayList<MemoryPart> orderedParts = new ArrayList<>(request.getParts().size());
            int sizeSum = 0;
            MemoryPart tmp = null;
            //Validate the part list
            for (Part p : request.getParts()) {
                if (p.getPartNumber() <= lastNumber) {
                    throw new InvalidPartOrderException(p.getPartNumber().toString());
                } else {
                    tmp = mpuParent.parts.get(p.getPartNumber());
                    if (tmp == null || !tmp.eTag.equals(p.getEtag())) {
                        throw new InvalidPartException(p.getPartNumber().toString());
                    }
                }

                orderedParts.add(tmp);
                sizeSum += tmp.size;
                lastNumber = p.getPartNumber();
            }

            MemoryObject finishedObj = new MemoryObject();
            finishedObj.key = mpuParent.key;
            finishedObj.canonicalId = mpuParent.canonicalId;
            finishedObj.modifiedDate = new Date();
            finishedObj.acl = mpuParent.acl;
            finishedObj.size = sizeSum;
            finishedObj.versionId = bucket.versioningStatus == ObjectStorageProperties.VersioningStatus.Enabled ? UUID.randomUUID().toString() : "null";

            //Consolidate the parts, yes this is inefficient for md5
            ByteArrayOutputStream data = new ByteArrayOutputStream(sizeSum);
            for (MemoryPart p : orderedParts) {
                data.write(p.content);
            }

            finishedObj.content = data.toByteArray();
            finishedObj.eTag = DigestUtils.md5Hex(finishedObj.content);
            //Make the object live
            bucket.objects.put(new ObjectKey(finishedObj.key, finishedObj.versionId), finishedObj);

            //Remove the upload record and all parts
            bucket.uploads.remove(mpuParent.uploadId);

            CompleteMultipartUploadResponseType response = request.getReply();
            response.setEtag(finishedObj.eTag);
            response.setKey(request.getKey());
            response.setBucket(request.getBucket());
            response.setVersionId(finishedObj.versionId);
            response.setStatusMessage("OK");
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory abortMultipartUpload exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) throws S3Exception {
        try {
            MemoryBucket bucket = getBucket(request.getBucket(), request.getAccessKeyID());
            if (!bucket.uploads.containsKey(request.getUploadId())) {
                throw new NoSuchUploadException(request.getUploadId());
            }
            //Remove the MPU
            bucket.uploads.remove(request.getUploadId());

            AbortMultipartUploadResponseType response = request.getReply();
            response.setStatusMessage("OK");
            response.set_return(true);
            LOG.debug("InMemory return response: " + response.getStatusMessage());
            return response;
        } catch (Exception e) {
            LOG.debug("InMemory abortMultipartUpload exception: ", e);
            if (e instanceof S3Exception) {
                throw (S3Exception) e;
            } else {
                throw new InternalErrorException(e);
            }
        }
    }

    @Override
    public ListPartsResponseType listParts(ListPartsType request) throws S3Exception {
        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());
        MemoryMpu upload = b.uploads.get(request.getUploadId());
        if (upload == null) {
            throw new NoSuchUploadException(request.getUploadId());
        }

        ListPartsResponseType response = request.getReply();
        response.setParts(new ArrayList<Part>());
        for (MemoryPart p : upload.parts.values()) {
            response.getParts().add(p.toPartEntry());
        }

        Upload up = upload.toUploadEntry();
        response.setKey(request.getKey());
        response.setUploadId(request.getUploadId());
        response.setBucket(request.getBucket());
        response.setInitiator(up.getInitiator());
        response.setOwner(up.getOwner());
        response.setStorageClass("STANDARD");
        response.setIsTruncated(false);
        response.setMaxParts(1000);
        response.setBucket(b.name);
        return response;
    }

    @Override
    public ListMultipartUploadsResponseType listMultipartUploads(ListMultipartUploadsType request) throws S3Exception {
        MemoryBucket b = getBucket(request.getBucket(), request.getAccessKeyID());

        ListMultipartUploadsResponseType response = request.getReply();
        response.setBucket(request.getBucket());
        response.setDelimiter(request.getDelimiter());
        response.setMaxUploads(request.getMaxUploads() != null ? request.getMaxUploads() : 1000);
        response.setPrefix(request.getPrefix());

        //Does not support prefix and delims
        response.setUploads(new ArrayList<Upload>());
        for (MemoryMpu mpu : b.uploads.values()) {
            response.getUploads().add(mpu.toUploadEntry());
            response.setUploadIdMarker(mpu.uploadId);
        }

        response.setIsTruncated(false);
        return response;
    }

}
