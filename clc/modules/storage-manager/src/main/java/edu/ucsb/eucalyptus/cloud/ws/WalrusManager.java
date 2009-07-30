package edu.ucsb.eucalyptus.cloud.ws;
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

import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import com.eucalyptus.ws.handlers.WalrusRESTBinding;

public class WalrusManager {
    private static Logger LOG = Logger.getLogger( WalrusManager.class );

    private StorageManager storageManager;
    private WalrusImageManager walrusImageManager;

    public WalrusManager(StorageManager storageManager, WalrusImageManager walrusImageManager) {
        this.storageManager = storageManager;
        this.walrusImageManager = walrusImageManager;
    }

    public void initialize() {
        check();
    }

    public void check() {
        File bukkitDir = new File(WalrusProperties.bucketRootDirectory);
        if(!bukkitDir.exists()) {
            if(!bukkitDir.mkdirs()) {
                LOG.fatal("Unable to make bucket root directory: " + WalrusProperties.bucketRootDirectory);
            }
        } else if(!bukkitDir.canWrite()) {
            LOG.fatal("Cannot write to bucket root directory: " + WalrusProperties.bucketRootDirectory);
        }
    }

    public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException {
        ListAllMyBucketsResponseType reply = (ListAllMyBucketsResponseType) request.getReply();
        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        String userId = request.getUserId();

        if(userId == null) {
            throw new AccessDeniedException("no such user");
        }

        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
        UserInfo searchUser = new UserInfo(userId);
        List<UserInfo> userInfoList = db2.query(searchUser);

        if(userInfoList.size() > 0) {
            UserInfo user = userInfoList.get(0);
            BucketInfo searchBucket = new BucketInfo();
            searchBucket.setOwnerId(userId);
            List<BucketInfo> bucketInfoList = db.query(searchBucket);

            ArrayList<BucketListEntry> buckets = new ArrayList<BucketListEntry>();

            for(BucketInfo bucketInfo: bucketInfoList) {
                buckets.add(new BucketListEntry(bucketInfo.getBucketName(), DateUtils.format(bucketInfo.getCreationDate().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z"));
            }

            CanonicalUserType owner = new CanonicalUserType(user.getQueryId(), user.getUserName());
            ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
            reply.setOwner(owner);
            bucketList.setBuckets(buckets);
            reply.setBucketList(bucketList);
        } else {
            db.rollback();
            throw new AccessDeniedException(userId);
        }
        db.commit();
        return reply;
    }


    private void makeBucket(String userId, String bucketName, String locationConstraint, AccessControlListType accessControlList, boolean isAdministrator) throws EucalyptusCloudException {
        if(userId == null) {
            throw new AccessDeniedException(bucketName);
        }

        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();

        if(WalrusProperties.shouldEnforceUsageLimits && !isAdministrator) {
            BucketInfo searchBucket = new BucketInfo();
            searchBucket.setOwnerId(userId);
            List<BucketInfo> bucketList = db.query(searchBucket);
            if(bucketList.size() >= WalrusProperties.MAX_BUCKETS_PER_USER) {
                db.rollback();
                throw new TooManyBucketsException(bucketName);
            }
        }

        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if(bucketList.size() > 0) {
            if(bucketList.get(0).getOwnerId().equals(userId)) {
                //bucket already exists and you created it
                db.rollback();
                throw new BucketAlreadyOwnedByYouException(bucketName);
            }
            //bucket already exists
            db.rollback();
            throw new BucketAlreadyExistsException(bucketName);
        }   else {
            //create bucket and set its acl
            BucketInfo bucket = new BucketInfo(userId, bucketName, new Date());
            ArrayList<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
            bucket.addGrants(userId, grantInfos, accessControlList);
            bucket.setGrants(grantInfos);
            bucket.setBucketSize(0L);
            if(locationConstraint != null)
                bucket.setLocation(locationConstraint);
            else
                bucket.setLocation("US");
            db.add(bucket);
        }
        db.commit();
    }

    public CreateBucketResponseType createBucket(CreateBucketType request) throws EucalyptusCloudException {
        CreateBucketResponseType reply = (CreateBucketResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();
        String locationConstraint = request.getLocationConstraint();

        if(userId == null) {
            throw new AccessDeniedException(bucketName);
        }

        AccessControlListType accessControlList = request.getAccessControlList();
        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        makeBucket(userId, bucketName, locationConstraint, accessControlList, request.isAdministrator());

        //call the storage manager to save the bucket to disk
        try {
            storageManager.createBucket(bucketName);
        } catch (IOException ex) {
            LOG.error(ex);
            throw new EucalyptusCloudException(bucketName);
        }

        if(WalrusProperties.enableVirtualHosting) {
            UpdateARecordType updateARecord = new UpdateARecordType();
            updateARecord.setUserId(userId);
            String address = WalrusProperties.WALRUS_IP;
            String zone = WalrusProperties.WALRUS_DOMAIN + ".";
            updateARecord.setAddress(address);
            updateARecord.setName(bucketName + "." + zone);
            updateARecord.setTtl(604800);
            updateARecord.setZone(zone);
            LOG.info("Mapping " + updateARecord.getName() + " to " + address);
            Messaging.send(DNSProperties.DNS_REF, updateARecord);
        }

        reply.setBucket(bucketName);
        return reply;
    }

    public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
        DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
        String bucketName = request.getBucket();
        String userId = request.getUserId();
        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo searchBucket = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(searchBucket);


        if(bucketList.size() > 0) {
            BucketInfo bucketFound = bucketList.get(0);
            if (bucketFound.canWrite(userId)) {
                EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
                ObjectInfo searchObject = new ObjectInfo();
                searchObject.setBucketName(bucketName);
                List<ObjectInfo> objectInfos = dbObject.query(searchObject);
                if(objectInfos.size() == 0) {
                    //asychronously flush any images in this bucket
                    EntityWrapper<ImageCacheInfo> dbIC = db.recast(ImageCacheInfo.class);
                    ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
                    searchImageCacheInfo.setBucketName(bucketName);
                    List<ImageCacheInfo> foundImageCacheInfos = dbIC.query(searchImageCacheInfo);

                    if(foundImageCacheInfos.size() > 0) {
                        ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
                        walrusImageManager.startImageCacheFlusher(bucketName, foundImageCacheInfo.getManifestName());
                    }

                    db.delete(bucketFound);
                    //Actually remove the bucket from the backing store
                    try {
                        storageManager.deleteBucket(bucketName);
                    } catch (IOException ex) {
                        //set exception code in reply
                        LOG.error(ex);
                    }

                    if(WalrusProperties.enableVirtualHosting) {
                        RemoveARecordType removeARecordType = new RemoveARecordType();
                        removeARecordType.setUserId(userId);
                        String zone = WalrusProperties.WALRUS_DOMAIN + ".";
                        removeARecordType.setName(bucketName + "." + zone);
                        removeARecordType.setZone(zone);
                        LOG.info("Removing mapping for " + removeARecordType.getName());
                        Messaging.send(DNSProperties.DNS_REF, removeARecordType);
                    }

                    Status status = new Status();
                    status.setCode(204);
                    status.setDescription("No Content");
                    reply.setStatus(status);
                } else {
                    db.rollback();
                    throw new BucketNotEmptyException(bucketName);
                }
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws EucalyptusCloudException
    {
        GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request.getReply();

        String bucketName = request.getBucket();
        String userId = request.getUserId();
        String ownerId = null;

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        AccessControlListType accessControlList = new AccessControlListType();

        if (bucketList.size() > 0) {
            //construct access control policy from grant infos
            BucketInfo bucket = bucketList.get(0);
            List<GrantInfo> grantInfos = bucket.getGrants();
            if (bucket.canReadACP(userId)) {
                ownerId = bucket.getOwnerId();
                ArrayList<Grant> grants = new ArrayList<Grant>();
                for (GrantInfo grantInfo: grantInfos) {
                    String uId = grantInfo.getUserId();
                    UserInfo userInfo = new UserInfo(uId);
                    EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
                    List<UserInfo> grantUserInfos = db2.query(userInfo);
                    db2.commit();

                    if(grantUserInfos.size() > 0) {
                        UserInfo grantUserInfo = grantUserInfos.get(0);
                        bucket.readPermissions(grants);
                        addPermission(grants, grantUserInfo, grantInfo);
                    }
                }
                accessControlList.setGrants(grants);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        UserInfo userInfo = new UserInfo(ownerId);
        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
        List<UserInfo> ownerUserInfos = db2.query(userInfo);
        db2.commit();

        AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
        if(ownerUserInfos.size() > 0) {
            UserInfo ownerUserInfo = ownerUserInfos.get(0);
            accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo.getQueryId(), ownerUserInfo.getUserName()));
            accessControlPolicy.setAccessControlList(accessControlList);
        }
        reply.setAccessControlPolicy(accessControlPolicy);
        db.commit();
        return reply;
    }


    private static void addPermission(ArrayList<Grant>grants, UserInfo userInfo, GrantInfo grantInfo) {
        CanonicalUserType user = new CanonicalUserType(userInfo.getQueryId(), userInfo.getUserName());

        if (grantInfo.isRead() && grantInfo.isWrite() && grantInfo.isReadACP() && grantInfo.isWriteACP()) {
            grants.add(new Grant(new Grantee(user), "FULL_CONTROL"));
            return;
        }

        if (grantInfo.isRead()) {
            grants.add(new Grant(new Grantee(user), "READ"));
        }

        if (grantInfo.isWrite()) {
            grants.add(new Grant(new Grantee(user), "WRITE"));
        }

        if (grantInfo.isReadACP()) {
            grants.add(new Grant(new Grantee(user), "READ_ACP"));
        }

        if (grantInfo.isWriteACP()) {
            grants.add(new Grant(new Grantee(user), "WRITE_ACP"));
        }
    }

    public PutObjectResponseType putObject(PutObjectType request) throws EucalyptusCloudException {
        PutObjectResponseType reply = (PutObjectResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();
        String objectKey = request.getKey();

        Long oldBucketSize = 0L;

        String md5 = "";
        Date lastModified = null;

        AccessControlListType accessControlList = request.getAccessControlList();
        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if(bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if (bucket.canWrite(userId)) {

                ObjectInfo foundObject = null;
                EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
                ObjectInfo searchObject = new ObjectInfo();
                searchObject.setBucketName(bucketName);
                List<ObjectInfo> objectInfos = dbObject.query(searchObject);
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectKey().equals(objectKey)) {
                        //key (object) exists. check perms
                        if (!objectInfo.canWrite(userId)) {
                            db.rollback();
                            throw new AccessDeniedException(objectKey);
                        }
                        foundObject = objectInfo;
                        oldBucketSize = -foundObject.getSize();
                        break;
                    }
                }
                //write object to bucket
                String objectName;
                if (foundObject == null) {
                    //not found. create an object info
                    foundObject = new ObjectInfo(bucketName, objectKey);
                    foundObject.setOwnerId(userId);
                    List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                    foundObject.addGrants(userId, grantInfos, accessControlList);
                    foundObject.setGrants(grantInfos);
                    objectName = objectKey.replaceAll("/", "-") + Hashes.getRandom(4);
                    foundObject.setObjectName(objectName);
                    dbObject.add(foundObject);
                } else {
                    //object already exists. see if we can modify acl
                    if (foundObject.canWriteACP(userId)) {
                        List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                        foundObject.addGrants(userId, grantInfos, accessControlList);
                        foundObject.setGrants(grantInfos);
                    }
                    objectName = foundObject.getObjectName();
                    if(WalrusProperties.enableTorrents) {
                        EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
                        TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
                        List<TorrentInfo> torrentInfos = dbTorrent.query(torrentInfo);
                        if(torrentInfos.size() > 0) {
                            TorrentInfo foundTorrentInfo = torrentInfos.get(0);
                            TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
                            if(torrentClient != null) {
                                torrentClient.bye();
                            }
                            dbTorrent.delete(foundTorrentInfo);
                        }
                    } else {
                        LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
                    }
                }
                foundObject.setObjectKey(objectKey);
                foundObject.replaceMetaData(request.getMetaData());
                //writes are unconditional
                String randomKey = request.getRandomKey();

                WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();
                String key = bucketName + "." + objectKey;
                LinkedBlockingQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

                try {
                    WalrusDataMessage dataMessage;
                    String tempObjectName = objectName;
                    MessageDigest digest = null;
                    long size = 0;
                    FileIO fileIO = null;
                    while ((dataMessage = putQueue.take())!=null) {
                        if(WalrusDataMessage.isStart(dataMessage)) {
                            tempObjectName = objectName + "." + Hashes.getRandom(12);
                            digest = Hashes.Digest.MD5.get();
                            try {
                                fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
                            } catch(Exception ex) {
                                throw new EucalyptusCloudException(ex);
                            }
                        } else if(WalrusDataMessage.isEOF(dataMessage)) {
                            //commit object
                            try {
                                storageManager.renameObject(bucketName, tempObjectName, objectName);
                            } catch (IOException ex) {
                                LOG.error(ex);
                                db.rollback();
                                throw new EucalyptusCloudException(objectKey);
                            }
                            md5 = Hashes.bytesToHex(digest.digest());
                            lastModified = new Date();
                            foundObject.setEtag(md5);
                            foundObject.setSize(size);
                            foundObject.setLastModified(lastModified);
                            foundObject.setStorageClass("STANDARD");
                            foundObject.setContentType(request.getContentType());
                            foundObject.setContentDisposition(request.getContentDisposition());
                            reply.setSize(size);
                            if(WalrusProperties.shouldEnforceUsageLimits && !request.isAdministrator()) {
                                Long bucketSize = bucket.getBucketSize();
                                long newSize = bucketSize + oldBucketSize + size;
                                if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
                                    db.rollback();
                                    throw new EntityTooLargeException(objectKey);
                                }
                                bucket.setBucketSize(newSize);
                            }
                            db.commit();
                            fileIO.finish();
                            //restart all interrupted puts
                            WalrusMonitor monitor = messenger.getMonitor(key);
                            synchronized (monitor) {
                                monitor.setLastModified(lastModified);
                                monitor.setMd5(md5);
                                monitor.notifyAll();
                            }
                            messenger.removeQueue(key, randomKey);
                            messenger.removeMonitor(key);
                            LOG.info("Transfer complete: " + key);
                            break;

                        } else if(WalrusDataMessage.isInterrupted(dataMessage)) {

                            //there was a write after this one started
                            //abort writing but wait until the other (last) writer has completed
                            WalrusMonitor monitor = messenger.getMonitor(key);
                            synchronized (monitor) {
                                monitor.wait();
                                lastModified = monitor.getLastModified();
                                md5 = monitor.getMd5();
                            }
                            //ok we are done here
                            try {
                                fileIO.finish();
                                storageManager.deleteObject(bucketName, tempObjectName);
                            } catch (IOException ex) {
                                LOG.error(ex);
                            }
                            db.rollback();
                            LOG.info("Transfer interrupted: "+ key);
                            break;
                        } else {
                            assert(WalrusDataMessage.isData(dataMessage));
                            byte[] data = dataMessage.getPayload();
                            //start writing object (but do not commit yet)
                            try {
                                fileIO.write(data);
                            } catch (IOException ex) {
                                LOG.error(ex);
                            }
                            //calculate md5 on the fly
                            size += data.length;
                            digest.update(data);
                        }
                    }
                } catch (InterruptedException ex) {
                    LOG.error(ex, ex);
                    throw new EucalyptusCloudException();
                }
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        reply.setEtag(md5);
        reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        return reply;
    }

    public PostObjectResponseType postObject(PostObjectType request) throws EucalyptusCloudException {
        PostObjectResponseType reply = (PostObjectResponseType) request.getReply();

        String bucketName = request.getBucket();
        String key = request.getKey();

        PutObjectType putObject = new PutObjectType();
        putObject.setUserId(request.getUserId());
        putObject.setBucket(bucketName);
        putObject.setKey(key);
        putObject.setRandomKey(request.getRandomKey());
        putObject.setAccessControlList(request.getAccessControlList());
        putObject.setContentLength(request.getContentLength());
        putObject.setAccessKeyID(request.getAccessKeyID());
        putObject.setEffectiveUserId(request.getEffectiveUserId());
        putObject.setCredential(request.getCredential());
        putObject.setIsCompressed(request.getIsCompressed());
        putObject.setMetaData(request.getMetaData());
        putObject.setStorageClass(request.getStorageClass());

        PutObjectResponseType putObjectResponse = putObject(putObject);

        String etag = putObjectResponse.getEtag();
        reply.setEtag(etag);
        reply.setLastModified(putObjectResponse.getLastModified());
        reply.set_return(putObjectResponse.get_return());
        reply.setMetaData(putObjectResponse.getMetaData());
        reply.setErrorCode(putObjectResponse.getErrorCode());
        reply.setSize(putObjectResponse.getSize());
        reply.setStatusMessage(putObjectResponse.getStatusMessage());

        String successActionRedirect = request.getSuccessActionRedirect();
        if(successActionRedirect != null) {
            try {
                java.net.URI addrUri = new URL(successActionRedirect).toURI();
                InetAddress.getByName(addrUri.getHost());
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            String paramString = "bucket=" + bucketName + "&key=" + key + "&etag=quot;" + etag + "quot;";
            reply.setRedirectUrl(successActionRedirect + "?" + paramString);
        } else {
            Integer successActionStatus = request.getSuccessActionStatus();
            if(successActionStatus != null) {
                if((successActionStatus == 200) || (successActionStatus == 201)) {
                    reply.setSuccessCode(successActionStatus);
                    if(successActionStatus == 200) {
                        return reply;
                    } else {
                        reply.setBucket(bucketName);
                        reply.setKey(key);
                        reply.setLocation(WalrusProperties.WALRUS_URL + "/" + bucketName + "/" + key);
                    }
                } else {
                    reply.setSuccessCode(204);
                    return reply;
                }
            }
        }
        return reply;
    }

    public PutObjectInlineResponseType putObjectInline(PutObjectInlineType request) throws EucalyptusCloudException {
        PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();
        String objectKey = request.getKey();

        String md5 = "";
        Long oldBucketSize = 0L;
        Date lastModified;

        AccessControlListType accessControlList = request.getAccessControlList();
        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if(bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if (bucket.canWrite(userId)) {
                EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
                ObjectInfo searchObjectInfo = new ObjectInfo();
                searchObjectInfo.setBucketName(bucketName);

                ObjectInfo foundObject = null;
                List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectKey().equals(objectKey)) {
                        //key (object) exists. check perms
                        if (!objectInfo.canWrite(userId)) {
                            db.rollback();
                            throw new AccessDeniedException(objectKey);
                        }
                        foundObject = objectInfo;
                        oldBucketSize = -foundObject.getSize();
                        break;
                    }
                }
                //write object to bucket
                String objectName;
                if (foundObject == null) {
                    //not found. create an object info
                    foundObject = new ObjectInfo(bucketName, objectKey);
                    foundObject.setOwnerId(userId);
                    List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                    foundObject.addGrants(userId, grantInfos, accessControlList);
                    foundObject.setGrants(grantInfos);
                    objectName = objectKey.replaceAll("/", "-") + Hashes.getRandom(4);
                    foundObject.setObjectName(objectName);
                    dbObject.add(foundObject);
                } else {
                    //object already exists. see if we can modify acl
                    if (foundObject.canWriteACP(userId)) {
                        List<GrantInfo> grantInfos = foundObject.getGrants();
                        foundObject.addGrants(userId, grantInfos, accessControlList);
                    }
                    objectName = foundObject.getObjectName();
                }
                foundObject.setObjectKey(objectKey);
                try {
                    //writes are unconditional
                    byte[] base64Data = request.getBase64Data().getBytes();
                    foundObject.setObjectName(objectName);
                    try {
                        FileIO fileIO = storageManager.prepareForWrite(bucketName, objectName);
                        fileIO.write(base64Data);
                        fileIO.finish();
                    } catch(Exception ex) {
                        throw new EucalyptusCloudException(ex);
                    }
                    md5 = Hashes.getHexString(Hashes.Digest.MD5.get().digest(base64Data));
                    foundObject.setEtag(md5);
                    Long size = Long.parseLong(request.getContentLength());
                    foundObject.setSize(size);
                    if(WalrusProperties.shouldEnforceUsageLimits && !request.isAdministrator()) {
                        Long bucketSize = bucket.getBucketSize();
                        long newSize = bucketSize + oldBucketSize + size;
                        if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
                            db.rollback();
                            throw new EntityTooLargeException(objectKey);
                        }
                        bucket.setBucketSize(newSize);
                    }
                    //Add meta data if specified
                    foundObject.replaceMetaData(request.getMetaData());

                    //TODO: add support for other storage classes
                    foundObject.setStorageClass("STANDARD");
                    lastModified = new Date();
                    foundObject.setLastModified(lastModified);
                } catch (/*TODO: NEIL, check if it is IOException*/Exception ex) {
                    LOG.error(ex);
                    db.rollback();
                    throw new EucalyptusCloudException(bucketName);
                }
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }

        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }

        db.commit();

        reply.setEtag(md5);
        reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        return reply;
    }

    public void addObject(String userId, String bucketName, String key) throws EucalyptusCloudException {

        AccessControlListType accessControlList = new AccessControlListType();
        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if(bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if (bucket.canWrite(userId)) {
                EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
                ObjectInfo searchObjectInfo = new ObjectInfo();
                searchObjectInfo.setBucketName(bucketName);
                List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectKey().equals(key)) {
                        //key (object) exists.
                        db.rollback();
                        throw new EucalyptusCloudException("object already exists " + key);
                    }
                }
                //write object to bucket
                ObjectInfo objectInfo = new ObjectInfo(bucketName, key);
                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                objectInfo.addGrants(userId, grantInfos, accessControlList);
                objectInfo.setGrants(grantInfos);
                dbObject.add(objectInfo);

                objectInfo.setObjectKey(key);
                objectInfo.setOwnerId(userId);
                objectInfo.setSize(storageManager.getSize(bucketName, key));
                objectInfo.setEtag("");
                objectInfo.setLastModified(new Date());
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
    }

    public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws EucalyptusCloudException {
        DeleteObjectResponseType reply = (DeleteObjectResponseType) request.getReply();
        String bucketName = request.getBucket();
        String objectKey = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfos = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfos);

        if (bucketList.size() > 0) {
            BucketInfo bucketInfo = bucketList.get(0);
            ObjectInfo foundObject = null;
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0) {
                foundObject = objectInfos.get(0);
            }

            if (foundObject != null) {
                if (foundObject.canWrite(userId)) {
                    dbObject.delete(foundObject);
                    String objectName = foundObject.getObjectName();
                    for (GrantInfo grantInfo: foundObject.getGrants()) {
                        db.getEntityManager().remove(grantInfo);
                    }
                    Long size = foundObject.getSize();
                    bucketInfo.setBucketSize(bucketInfo.getBucketSize() - size);
                    try {
                        storageManager.deleteObject(bucketName, objectName);
                    } catch (IOException ex) {
                        db.rollback();
                        LOG.error(ex);
                        throw new EucalyptusCloudException(objectKey);
                    }
                    reply.setCode("200");
                    reply.setDescription("OK");
                } else {
                    db.rollback();
                    throw new AccessDeniedException(objectKey);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectKey);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public ListBucketResponseType listBucket(ListBucketType request) throws EucalyptusCloudException {
        ListBucketResponseType reply = (ListBucketResponseType) request.getReply();
        String bucketName = request.getBucket();
        String userId = request.getUserId();
        String prefix = request.getPrefix();
        if(prefix == null)
            prefix = "";

        String marker = request.getMarker();
        if(marker == null)
            marker = "";

        int maxKeys = -1;
        String maxKeysString = request.getMaxKeys();
        if(maxKeysString != null)
            maxKeys = Integer.parseInt(maxKeysString);
        else
            maxKeys = WalrusProperties.MAX_KEYS;

        String delimiter = request.getDelimiter();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        ArrayList<PrefixEntry> prefixes = new ArrayList<PrefixEntry>();

        if(bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if(bucket.canRead(userId)) {
                reply.setName(bucketName);
                reply.setIsTruncated(false);
                if(maxKeys >= 0)
                    reply.setMaxKeys(maxKeys);
                reply.setPrefix(prefix);
                reply.setMarker(marker);
                if(delimiter != null)
                    reply.setDelimiter(delimiter);
                EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
                ObjectInfo searchObjectInfo = new ObjectInfo();
                searchObjectInfo.setBucketName(bucketName);
                List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
                if(objectInfos.size() > 0) {
                    int howManyProcessed = 0;
                    ArrayList<ListEntry> contents = new ArrayList<ListEntry>();
                    for(ObjectInfo objectInfo: objectInfos) {
                        String objectKey = objectInfo.getObjectKey();
                        if(marker != null) {
                            if(objectKey.compareTo(marker) < 0)
                                continue;
                        }
                        if(prefix != null) {
                            if(!objectKey.startsWith(prefix)) {
                                continue;
                            } else {
                                if(delimiter != null) {
                                    String[] parts = objectKey.substring(prefix.length()).split(delimiter);
                                    if(parts.length > 1) {
                                        String prefixString = parts[0] + delimiter;
                                        boolean foundPrefix = false;
                                        for(PrefixEntry prefixEntry : prefixes) {
                                            if(prefixEntry.getPrefix().equals(prefixString)) {
                                                foundPrefix = true;
                                                break;
                                            }
                                        }
                                        if(!foundPrefix) {
                                            prefixes.add(new PrefixEntry(prefixString));
                                            if(maxKeys >= 0) {
                                                if(howManyProcessed++ > maxKeys) {
                                                    reply.setIsTruncated(true);
                                                    break;
                                                }
                                            }
                                        }
                                        continue;
                                    }
                                }
                            }
                        }
                        if(maxKeys >= 0) {
                            if(howManyProcessed++ > maxKeys) {
                                reply.setIsTruncated(true);
                                break;
                            }
                        }
                        ListEntry listEntry = new ListEntry();
                        listEntry.setKey(objectKey);
                        listEntry.setEtag(objectInfo.getEtag());
                        listEntry.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                        listEntry.setStorageClass(objectInfo.getStorageClass());
                        String displayName = objectInfo.getOwnerId();

                        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
                        UserInfo userInfo = new UserInfo(displayName);
                        List<UserInfo> ownerInfos = db2.query(userInfo);
                        db2.commit();
                        if(ownerInfos.size() > 0) {
                            listEntry.setOwner(new CanonicalUserType(ownerInfos.get(0).getQueryId(), displayName));
                        }
                        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
                        objectInfo.returnMetaData(metaData);
                        reply.setMetaData(metaData);
                        listEntry.setSize(objectInfo.getSize());
                        listEntry.setStorageClass(objectInfo.getStorageClass());
                        contents.add(listEntry);
                    }
                    reply.setContents(contents);
                    if(prefix != null) {
                        reply.setCommonPrefixes(prefixes);
                    }
                }
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request.getReply();

        String bucketName = request.getBucket();
        String objectKey = request.getKey();
        String userId = request.getUserId();
        String ownerId = null;

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        AccessControlListType accessControlList = new AccessControlListType();
        if (bucketList.size() > 0) {
            //construct access control policy from grant infos
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0) {
                ObjectInfo objectInfo = objectInfos.get(0);
                if(objectInfo.canReadACP(userId)) {
                    ownerId = objectInfo.getOwnerId();
                    ArrayList<Grant> grants = new ArrayList<Grant>();
                    List<GrantInfo> grantInfos = objectInfo.getGrants();
                    for (GrantInfo grantInfo: grantInfos) {
                        String uId = grantInfo.getUserId();
                        UserInfo userInfo = new UserInfo(uId);
                        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
                        List<UserInfo> grantUserInfos = db2.query(userInfo);
                        db2.commit();
                        if(grantUserInfos.size() > 0) {
                            objectInfo.readPermissions(grants);
                            addPermission(grants, grantUserInfos.get(0), grantInfo);
                        }
                    }
                    accessControlList.setGrants(grants);
                } else {
                    db.rollback();
                    throw new AccessDeniedException(objectKey);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectKey);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        UserInfo userInfo = new UserInfo(ownerId);
        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
        List<UserInfo> ownerUserInfos = db2.query(userInfo);
        db2.commit();

        AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
        if(ownerUserInfos.size() > 0) {
            UserInfo ownerUserInfo = ownerUserInfos.get(0);
            accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo.getQueryId(), ownerUserInfo.getUserName()));
            accessControlPolicy.setAccessControlList(accessControlList);
            reply.setAccessControlPolicy(accessControlPolicy);
        }
        db.commit();
        return reply;
    }

    public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException
    {
        SetBucketAccessControlPolicyResponseType reply = (SetBucketAccessControlPolicyResponseType) request.getReply();
        String userId = request.getUserId();
        AccessControlPolicyType accessControlPolicy = request.getAccessControlPolicy();
        if(accessControlPolicy == null) {
            throw new AccessDeniedException(userId);
        }
        String bucketName = request.getBucket();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if (bucket.canWriteACP(userId)) {
                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                AccessControlListType accessControlList = accessControlPolicy.getAccessControlList();
                bucket.resetGlobalGrants();
                bucket.addGrants(bucket.getOwnerId(), grantInfos, accessControlList);
                bucket.setGrants(grantInfos);
                reply.setCode("204");
                reply.setDescription("OK");
            } else {
                db.rollback();
                throw new AccessDeniedException(bucketName);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        SetObjectAccessControlPolicyResponseType reply = (SetObjectAccessControlPolicyResponseType) request.getReply();
        String userId = request.getUserId();
        AccessControlPolicyType accessControlPolicy = request.getAccessControlPolicy();
        String bucketName = request.getBucket();
        String objectKey = request.getKey();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0)  {
                ObjectInfo objectInfo = objectInfos.get(0);
                if (!objectInfo.canWriteACP(userId)) {
                    db.rollback();
                    throw new AccessDeniedException(objectKey);
                }
                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                AccessControlListType accessControlList = accessControlPolicy.getAccessControlList();
                objectInfo.resetGlobalGrants();
                objectInfo.addGrants(objectInfo.getOwnerId(), grantInfos, accessControlList);
                objectInfo.setGrants(grantInfos);

                if(WalrusProperties.enableTorrents) {
                    if(!objectInfo.isGlobalRead()) {
                        EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
                        TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
                        List<TorrentInfo> torrentInfos = dbTorrent.query(torrentInfo);
                        if(torrentInfos.size() > 0) {
                            TorrentInfo foundTorrentInfo = torrentInfos.get(0);
                            TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
                            if(torrentClient != null) {
                                torrentClient.bye();
                            }
                            dbTorrent.delete(foundTorrentInfo);
                        }
                    }
                } else {
                    LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
                }
                reply.setCode("204");
                reply.setDescription("OK");
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectKey);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetObjectResponseType getObject(GetObjectType request) throws EucalyptusCloudException {
        GetObjectResponseType reply = (GetObjectResponseType) request.getReply();
        String bucketName = request.getBucket();
        String objectKey = request.getKey();
        String userId = request.getUserId();
        Boolean deleteAfterGet = request.getDeleteAfterGet();
        if(deleteAfterGet == null)
            deleteAfterGet = false;

        Boolean getTorrent = request.getGetTorrent();
        if(getTorrent == null)
            getTorrent = false;

        Boolean getMetaData = request.getGetMetaData();
        if(getMetaData == null)
            getMetaData = false;

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0) {
                ObjectInfo objectInfo = objectInfos.get(0);
                if(objectInfo.canRead(userId)) {
                    String objectName = objectInfo.getObjectName();
                    if(getMetaData) {
                        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
                        objectInfo.returnMetaData(metaData);
                        reply.setMetaData(metaData);
                        reply.setMetaData(metaData);
                    }
                    if(getTorrent) {
                        if(objectInfo.isGlobalRead()) {
                            if(!WalrusProperties.enableTorrents) {
                                LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
                                throw new EucalyptusCloudException("Torrents disabled");
                            }
                            EntityWrapper<TorrentInfo> dbTorrent = new EntityWrapper<TorrentInfo>();
                            TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
                            TorrentInfo foundTorrentInfo;
                            String absoluteObjectPath = storageManager.getObjectPath(bucketName, objectName);
                            try {
                                foundTorrentInfo = dbTorrent.getUnique(torrentInfo);
                            } catch (EucalyptusCloudException ex) {
                                String torrentFile = objectName + ".torrent";
                                String torrentFilePath = storageManager.getObjectPath(bucketName, torrentFile);
                                TorrentCreator torrentCreator = new TorrentCreator(absoluteObjectPath, objectKey, objectName, torrentFilePath, WalrusProperties.TRACKER_URL);
                                try {
                                    torrentCreator.create();
                                } catch(Exception e) {
                                    LOG.error(e);
                                    throw new EucalyptusCloudException("could not create torrent file " + torrentFile);
                                }
                                torrentInfo.setTorrentFile(torrentFile);
                                dbTorrent.add(torrentInfo);
                                foundTorrentInfo = torrentInfo;
                            }
                            dbTorrent.commit();
                            String torrentFile = foundTorrentInfo.getTorrentFile();
                            String torrentFilePath = storageManager.getObjectPath(bucketName, torrentFile);
                            TorrentClient torrentClient = new TorrentClient(torrentFilePath, absoluteObjectPath);
                            Torrents.addClient(bucketName + objectKey, torrentClient);
                            torrentClient.start();
                            //send torrent
                            String key = bucketName + "." + objectKey;
                            String randomKey = key + "." + Hashes.getRandom(10);
                            request.setRandomKey(randomKey);
                            LinkedBlockingQueue<WalrusDataMessage> getQueue = null; //TODO: NEIL WalrusQueryDispatcher.getReadMessenger().getQueue(key, randomKey);

                            File torrent = new File(torrentFilePath);
                            if(torrent.exists()) {
                                long torrentLength = torrent.length();
                                ObjectReader reader = new ObjectReader(bucketName, torrentFile, torrentLength, getQueue, false, null, storageManager);
                                reader.start();
                                //TODO: this should reflect params for the torrent?
                                reply.setEtag("");
                                reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN));
                                reply.setSize(torrentLength);
                                Status status = new Status();
                                status.setCode(200);
                                status.setDescription("OK");
                                reply.setStatus(status);
                                reply.setContentType("binary/octet-stream");
                                db.commit();
                                return reply;
                            } else {
                                String errorString = "Could not get torrent file " + torrentFilePath;
                                LOG.error(errorString);
                                throw new EucalyptusCloudException(errorString);
                            }
                        } else {
                            db.rollback();
                            throw new AccessDeniedException(objectKey);
                        }
                    }
                    if(request.getGetData()) {
                        if(request.getInlineData()) {
                            try {
                                byte[] bytes = new byte[102400/*TODO: NEIL WalrusQueryDispatcher.DATA_MESSAGE_SIZE*/];
                                int bytesRead = 0;
                                String base64Data = "";
                                while((bytesRead = storageManager.readObject(bucketName, objectName, bytes, bytesRead)) > 0) {
                                    base64Data += new String(bytes, 0, bytesRead);
                                }
                                reply.setBase64Data(base64Data);
                            } catch (IOException ex) {
                                db.rollback();
                                LOG.error(ex);
                                //set error code
                                return reply;
                            }
                        } else {
                            //support for large objects
                            String key = bucketName + "." + objectKey;
                            String randomKey = key + "." + Hashes.getRandom(10);
                            request.setRandomKey(randomKey);
                            LinkedBlockingQueue<WalrusDataMessage> getQueue = WalrusRESTBinding.getReadMessenger().getQueue(key, randomKey);

                            ObjectReader reader = new ObjectReader(bucketName, objectName, objectInfo.getSize(), getQueue, deleteAfterGet, null, storageManager);
                            reader.start();
                        }
                    }
                    reply.setEtag(objectInfo.getEtag());
                    reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN + ".000Z"));
                    reply.setSize(objectInfo.getSize());
                    reply.setContentType(objectInfo.getContentType());
                    reply.setContentDisposition(objectInfo.getContentDisposition());
                    Status status = new Status();
                    status.setCode(200);
                    status.setDescription("OK");
                    reply.setStatus(status);
                    db.commit();
                    return reply;
                } else {
                    db.rollback();
                    throw new AccessDeniedException(objectKey);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectKey);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
    }

    public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
        GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request.getReply();
        Long byteRangeStart = request.getByteRangeStart();
        if(byteRangeStart == null) {
            byteRangeStart = 0L;
        }
        Long byteRangeEnd = request.getByteRangeEnd();
        if(byteRangeEnd == null) {
            byteRangeEnd = -1L;
        }
        Date ifModifiedSince = request.getIfModifiedSince();
        Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
        String ifMatch = request.getIfMatch();
        String ifNoneMatch = request.getIfNoneMatch();
        boolean returnCompleteObjectOnFailure = request.getReturnCompleteObjectOnConditionFailure();

        String bucketName = request.getBucket();
        String objectKey = request.getKey();
        String userId = request.getUserId();
        Status status = new Status();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0) {
                ObjectInfo objectInfo = objectInfos.get(0);

                if(objectInfo.canRead(userId)) {
                    String etag = objectInfo.getEtag();
                    String objectName = objectInfo.getObjectName();
                    if(ifMatch != null) {
                        if(!ifMatch.equals(etag) && !returnCompleteObjectOnFailure) {
                            db.rollback();
                            throw new PreconditionFailedException(etag);
                        }

                    }
                    if(ifNoneMatch != null) {
                        if(ifNoneMatch.equals(etag) && !returnCompleteObjectOnFailure) {
                            db.rollback();
                            throw new NotModifiedException(etag);
                        }
                    }
                    Date lastModified = objectInfo.getLastModified();
                    if(ifModifiedSince != null) {
                        if((ifModifiedSince.getTime() >= lastModified.getTime()) && !returnCompleteObjectOnFailure) {
                            db.rollback();
                            throw new NotModifiedException(lastModified.toString());
                        }
                    }
                    if(ifUnmodifiedSince != null) {
                        if((ifUnmodifiedSince.getTime() < lastModified.getTime()) && !returnCompleteObjectOnFailure) {
                            db.rollback();
                            throw new PreconditionFailedException(lastModified.toString());
                        }
                    }
                    if(request.getGetMetaData()) {
                        ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
                        objectInfo.returnMetaData(metaData);
                        reply.setMetaData(metaData);
                    }
                    if(request.getGetData()) {
                        String key = bucketName + "." + objectKey;
                        String randomKey = key + "." + Hashes.getRandom(10);
                        request.setRandomKey(randomKey);
                        LinkedBlockingQueue<WalrusDataMessage> getQueue = null; //TODO: NEIL WalrusQueryDispatcher.getReadMessenger().getQueue(key, randomKey);

                        ObjectReader reader = new ObjectReader(bucketName, objectName, objectInfo.getSize(), getQueue, byteRangeStart, byteRangeEnd, storageManager);
                        reader.start();
                    }
                    reply.setEtag(objectInfo.getEtag());
                    reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                    if(byteRangeEnd > -1) {
                        if(byteRangeEnd <= objectInfo.getSize() && ((byteRangeEnd - byteRangeStart) > 0))
                            reply.setSize(byteRangeEnd - byteRangeStart);
                        else
                            reply.setSize(0L);
                    } else {
                        reply.setSize(objectInfo.getSize());
                    }
                    status.setCode(200);
                    status.setDescription("OK");
                    reply.setContentType("binary/octet-stream");                    
                    reply.setStatus(status);
                } else {
                    db.rollback();
                    throw new AccessDeniedException(objectKey);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectKey);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request) throws EucalyptusCloudException {
        GetBucketLocationResponseType reply = (GetBucketLocationResponseType) request.getReply();
        String bucketName = request.getBucket();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if(bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            if(bucket.canRead(userId)) {
                String location = bucket.getLocation();
                if(location == null) {
                    location = "NotSupported";
                }
                reply.setLocationConstraint(location);
            } else {
                db.rollback();
                throw new AccessDeniedException(userId);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public CopyObjectResponseType copyObject(CopyObjectType request) throws EucalyptusCloudException {
        CopyObjectResponseType reply = (CopyObjectResponseType) request.getReply();
        String userId = request.getUserId();
        String sourceBucket = request.getSourceBucket();
        String sourceKey = request.getSourceObject();
        String destinationBucket = request.getDestinationBucket();
        String destinationKey = request.getDestinationObject();
        String metadataDirective = request.getMetadataDirective();
        AccessControlListType accessControlList = request.getAccessControlList();

        String copyIfMatch = request.getCopySourceIfMatch();
        String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
        Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
        Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();

        if(metadataDirective == null)
            metadataDirective = "COPY";
        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(sourceBucket);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
            ObjectInfo searchObjectInfo = new ObjectInfo(sourceBucket, sourceKey);
            List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
            if(objectInfos.size() > 0) {
                ObjectInfo sourceObjectInfo = objectInfos.get(0);
                if(sourceObjectInfo.canRead(userId)) {
                    if(copyIfMatch != null) {
                        if(!copyIfMatch.equals(sourceObjectInfo.getEtag())) {
                            db.rollback();
                            throw new PreconditionFailedException("CopySourceIfMatch " + copyIfMatch);
                        }
                    }
                    if(copyIfNoneMatch != null) {
                        if(copyIfNoneMatch.equals(sourceObjectInfo.getEtag())) {
                            db.rollback();
                            throw new PreconditionFailedException("CopySourceIfNoneMatch " + copyIfNoneMatch);
                        }
                    }
                    if(copyIfUnmodifiedSince != null) {
                        long unmodifiedTime = copyIfUnmodifiedSince.getTime();
                        long objectTime = sourceObjectInfo.getLastModified().getTime();
                        if(unmodifiedTime < objectTime) {
                            db.rollback();
                            throw new PreconditionFailedException("CopySourceIfUnmodifiedSince " + copyIfUnmodifiedSince.toString());
                        }
                    }
                    if(copyIfModifiedSince != null) {
                        long modifiedTime = copyIfModifiedSince.getTime();
                        long objectTime = sourceObjectInfo.getLastModified().getTime();
                        if(modifiedTime > objectTime) {
                            db.rollback();
                            throw new PreconditionFailedException("CopySourceIfModifiedSince " + copyIfModifiedSince.toString());
                        }
                    }
                    BucketInfo destinationBucketInfo = new BucketInfo(destinationBucket);
                    List<BucketInfo> destinationBuckets = db.query(destinationBucketInfo);
                    if(destinationBuckets.size() > 0) {
                        BucketInfo foundDestinationBucketInfo = destinationBuckets.get(0);
                        if(foundDestinationBucketInfo.canWrite(userId)) {
                            //all ok
                            ObjectInfo destinationObjectInfo = null;
                            String destinationObjectName;
                            ObjectInfo destSearchObjectInfo = new ObjectInfo(destinationBucket, destinationKey);
                            List<ObjectInfo> destinationObjectInfos = dbObject.query(destSearchObjectInfo);
                            if(destinationObjectInfos.size() > 0) {
                                destinationObjectInfo = destinationObjectInfos.get(0);
                                if(!destinationObjectInfo.canWrite(userId)) {
                                    db.rollback();
                                    throw new AccessDeniedException(destinationKey);
                                }
                            }

                            if(destinationObjectInfo == null) {
                                //not found. create a new one
                                destinationObjectInfo = new ObjectInfo();
                                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                                destinationObjectInfo.setObjectKey(destinationKey);
                                destinationObjectInfo.addGrants(userId, grantInfos, accessControlList);
                                destinationObjectInfo.setGrants(grantInfos);
                                destinationObjectInfo.setObjectName(destinationKey.replaceAll("/", "-") + Hashes.getRandom(4));
                                dbObject.add(destinationObjectInfo);
                            } else {
                                if (destinationObjectInfo.canWriteACP(userId)) {
                                    List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                                    destinationObjectInfo.addGrants(userId, grantInfos, accessControlList);
                                    destinationObjectInfo.setGrants(grantInfos);
                                }
                            }

                            destinationObjectInfo.setSize(sourceObjectInfo.getSize());
                            destinationObjectInfo.setStorageClass(sourceObjectInfo.getStorageClass());
                            destinationObjectInfo.setOwnerId(sourceObjectInfo.getOwnerId());
                            String etag = sourceObjectInfo.getEtag();
                            Date lastModified = sourceObjectInfo.getLastModified();
                            destinationObjectInfo.setEtag(etag);
                            destinationObjectInfo.setLastModified(lastModified);
                            if(!metadataDirective.equals("REPLACE")) {
                                destinationObjectInfo.setMetaData(sourceObjectInfo.cloneMetaData());
                            } else {
                                List<MetaDataEntry> metaData = request.getMetaData();
                                if(metaData != null)
                                    destinationObjectInfo.replaceMetaData(metaData);
                            }

                            String sourceObjectName = sourceObjectInfo.getObjectName();
                            destinationObjectName = destinationObjectInfo.getObjectName();

                            try {
                                storageManager.copyObject(sourceBucket, sourceObjectName, destinationBucket, destinationObjectName);
                            } catch(Exception ex) {
                                LOG.error(ex);
                                db.rollback();
                                throw new EucalyptusCloudException("Could not rename " + sourceObjectName + " to " + destinationObjectName);
                            }
                            reply.setEtag(etag);
                            reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");

                            db.commit();
                            return reply;
                        } else {
                            db.rollback();
                            throw new AccessDeniedException(destinationBucket);
                        }
                    } else {
                        db.rollback();
                        throw new NoSuchBucketException(destinationBucket);
                    }
                } else {
                    db.rollback();
                    throw new AccessDeniedException(sourceKey);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(sourceKey);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(sourceBucket);
        }
    }
}