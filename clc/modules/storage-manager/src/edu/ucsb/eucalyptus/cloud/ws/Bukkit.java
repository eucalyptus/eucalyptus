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

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.keys.UserKeyStore;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.transport.query.WalrusQueryDispatcher;
import edu.ucsb.eucalyptus.util.*;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

public class Bukkit {

    private static Logger LOG = Logger.getLogger( Bukkit.class );

    private static WalrusDataMessenger imageMessenger = new WalrusDataMessenger();

    static StorageManager storageManager;
    static boolean shouldEnforceUsageLimits = true;

    static {
        storageManager = new FileSystemStorageManager(WalrusProperties.bucketRootDirectory);
        String limits = System.getProperty(WalrusProperties.USAGE_LIMITS_PROPERTY);
        if(limits != null) {
            shouldEnforceUsageLimits = Boolean.parseBoolean(limits);
        }
        //NOTE: initializeForEBS MUST be called before exercizing any storage/EBS functionality
        //initializeForEBS();
    }

    public static void initializeForEBS() {
        storageManager.initialize();
    }

    //For unit testing
    public Bukkit () {}

    public InitializeWalrusResponseType InitializeWalrus(InitializeWalrusType request) {
        InitializeWalrusResponseType reply = (InitializeWalrusResponseType) request.getReply();
        initializeForEBS();
        return reply;
    }

    public CreateBucketResponseType CreateBucket(CreateBucketType request) throws EucalyptusCloudException {
        CreateBucketResponseType reply = (CreateBucketResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();

        if(userId == null) {
            throw new AccessDeniedException(bucketName);
        }

        AccessControlListType accessControlList = request.getAccessControlList();
        if (accessControlList == null) {
            accessControlList = new AccessControlListType();
        }

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();

        if(shouldEnforceUsageLimits && !request.isAdministrator()) {
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

            ArrayList<ObjectInfo> objectInfos = new ArrayList<ObjectInfo>();
            bucket.setObjects(objectInfos);
            //call the storage manager to save the bucket to disk
            try {
                storageManager.createBucket(bucketName);
                db.add(bucket);
            } catch (IOException ex) {
                LOG.warn(ex, ex);
                db.rollback();
                throw new EucalyptusCloudException(bucketName);
            }
        }
        db.commit();

        reply.setBucket(bucketName);
        return reply;
    }

    public DeleteBucketResponseType DeleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
        DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();

        String bucketName = request.getBucket();

        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo searchBucket = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(searchBucket);


        if(bucketList.size() > 0) {
            BucketInfo bucketFound = bucketList.get(0);
            if (bucketFound.canWrite(userId)) {
                List<ObjectInfo> objectInfos = bucketFound.getObjects();
                if(objectInfos.size() == 0) {
                    db.delete(bucketFound);
                    //Actually remove the bucket from the backing store
                    try {
                        storageManager.deleteBucket(bucketName);
                    } catch (IOException ex) {
                        //set exception code in reply
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

    public ListAllMyBucketsResponseType ListAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException {
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

    public GetBucketAccessControlPolicyResponseType GetBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws EucalyptusCloudException
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
            grants.add(new Grant(user, "FULL_CONTROL"));
            return;
        }

        if (grantInfo.isRead()) {
            grants.add(new Grant(user, "READ"));
        }

        if (grantInfo.isWrite()) {
            grants.add(new Grant(user, "WRITE"));
        }

        if (grantInfo.isReadACP()) {
            grants.add(new Grant(user, "READ_ACP"));
        }

        if (grantInfo.isWriteACP()) {
            grants.add(new Grant(user, "WRITE_ACP"));
        }
    }

    public PutObjectResponseType PutObject (PutObjectType request) throws EucalyptusCloudException {
        PutObjectResponseType reply = (PutObjectResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();
        String objectName = request.getKey();

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
                List<ObjectInfo> objectInfos = bucket.getObjects();
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectName().equals(objectName)) {
                        //key (object) exists. check perms
                        if (!objectInfo.canWrite(userId)) {
                            db.rollback();
                            throw new AccessDeniedException(objectName);
                        }
                        foundObject = objectInfo;
                        oldBucketSize = -foundObject.getSize();
                        break;
                    }
                }
                //write object to bucket
                if (foundObject == null) {
                    //not found. create an object info
                    foundObject = new ObjectInfo(objectName);
                    List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                    foundObject.addGrants(userId, grantInfos, accessControlList);
                    foundObject.setGrants(grantInfos);
                    objectInfos.add(foundObject);
                } else {
                    //object already exists. see if we can modify acl
                    if (foundObject.canWriteACP(userId)) {
                        List<GrantInfo> grantInfos = foundObject.getGrants();
                        foundObject.addGrants(userId, grantInfos, accessControlList);
                    }
                }
                foundObject.setObjectName(objectName);
                foundObject.setOwnerId(userId);
                foundObject.addMetaData(request.getMetaData());
                //writes are unconditional
                String randomKey = request.getRandomKey();

                WalrusDataMessenger messenger = WalrusQueryDispatcher.getWriteMessenger();
                String key = bucketName + "." + objectName;
                LinkedBlockingQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

                try {
                    WalrusDataMessage dataMessage;
                    String tempObjectName = objectName;
                    MessageDigest digest = null;
                    long size = 0;
                    while ((dataMessage = putQueue.take())!=null) {
                        if(WalrusDataMessage.isStart(dataMessage)) {
                            tempObjectName = objectName + "." + Hashes.getRandom(12);
                            digest = Hashes.Digest.MD5.get();
                        } else if(WalrusDataMessage.isEOF(dataMessage)) {
                            //commit object
                            try {
                                storageManager.renameObject(bucketName, tempObjectName, objectName);
                            } catch (IOException ex) {
                                LOG.warn(ex, ex);
                                db.rollback();
                                throw new EucalyptusCloudException(objectName);
                            }
                            md5 = bytesToHex(digest.digest());
                            lastModified = new Date();
                            foundObject.setEtag(md5);
                            foundObject.setSize(size);
                            foundObject.setLastModified(lastModified);
                            foundObject.setStorageClass("STANDARD");
                            if(shouldEnforceUsageLimits && !request.isAdministrator()) {
                                Long bucketSize = bucket.getBucketSize();
                                long newSize = bucketSize + oldBucketSize + size;
                                if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
                                    db.rollback();
                                    throw new EntityTooLargeException(objectName);
                                }
                                bucket.setBucketSize(newSize);
                            }
                            db.commit();
                            //restart all interrupted puts
                            WalrusMonitor monitor = messenger.getMonitor(key);
                            synchronized (monitor) {
                                monitor.setLastModified(lastModified);
                                monitor.setMd5(md5);
                                monitor.notifyAll();
                            }
                            messenger.removeQueue(key, randomKey);
                            messenger.removeMonitor(key);
                            LOG.info("Transfer complete" + key + " " + randomKey);
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
                                storageManager.deleteObject(bucketName, tempObjectName);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            db.rollback();
                            LOG.info("Transfer interrupted" + key + " " + randomKey);
                            break;
                        } else {
                            assert(WalrusDataMessage.isData(dataMessage));
                            byte[] data = dataMessage.getPayload();
                            //start writing object (but do not committ yet)
                            try {
                                storageManager.putObject(bucketName, tempObjectName, data, true);
                            } catch (IOException ex) {
                                LOG.warn(ex, ex);
                            }
                            //calculate md5 on the fly
                            size += data.length;
                            digest.update(data);
                        }
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
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

    public PutObjectInlineResponseType PutObjectInline (PutObjectInlineType request) throws EucalyptusCloudException {
        PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request.getReply();
        String userId = request.getUserId();

        String bucketName = request.getBucket();
        String objectName = request.getKey();

        String md5 = "";
        Long oldBucketSize = 0L;
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
                List<ObjectInfo> objectInfos = bucket.getObjects();
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectName().equals(objectName)) {
                        //key (object) exists. check perms
                        if (!objectInfo.canWrite(userId)) {
                            db.rollback();
                            throw new AccessDeniedException(objectName);
                        }
                        foundObject = objectInfo;
                        oldBucketSize = -foundObject.getSize();
                        break;
                    }
                }
                //write object to bucket
                if (foundObject == null) {
                    //not found. create an object info
                    foundObject = new ObjectInfo(objectName);
                    List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                    foundObject.addGrants(userId, grantInfos, accessControlList);
                    foundObject.setGrants(grantInfos);
                    objectInfos.add(foundObject);
                } else {
                    //object already exists. see if we can modify acl
                    if (foundObject.canWriteACP(userId)) {
                        List<GrantInfo> grantInfos = foundObject.getGrants();
                        foundObject.addGrants(userId, grantInfos, accessControlList);
                    }
                }
                foundObject.setObjectName(objectName);
                foundObject.setOwnerId(userId);
                try {
                    //writes are unconditional
                    byte[] base64Data = request.getBase64Data().getBytes();
                    storageManager.putObject(bucketName, objectName, base64Data, false);
                    md5 = Hashes.getHexString(Hashes.Digest.MD5.get().digest(base64Data));
                    foundObject.setEtag(md5);
                    Long size = Long.parseLong(request.getContentLength());
                    foundObject.setSize(size);
                    if(shouldEnforceUsageLimits && !request.isAdministrator()) {
                        Long bucketSize = bucket.getBucketSize();
                        long newSize = bucketSize + oldBucketSize + size;
                        if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
                            db.rollback();
                            throw new EntityTooLargeException(objectName);
                        }
                        bucket.setBucketSize(newSize);
                    }
                    //Add meta data if specified
                    foundObject.addMetaData(request.getMetaData());

                    //TODO: add support for other storage classes
                    foundObject.setStorageClass("STANDARD");
                    lastModified = new Date();
                    foundObject.setLastModified(lastModified);
                } catch (IOException ex) {
                    LOG.warn(ex, ex);
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

    private void AddObject (String userId, String bucketName, String key) throws EucalyptusCloudException {

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
                List<ObjectInfo> objectInfos = bucket.getObjects();
                for (ObjectInfo objectInfo: objectInfos) {
                    if (objectInfo.getObjectName().equals(key)) {
                        //key (object) exists.
                        db.rollback();
                        throw new EucalyptusCloudException("object already exists " + key);
                    }
                }
                //write object to bucket
                ObjectInfo objectInfo = new ObjectInfo(key);
                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                objectInfo.addGrants(userId, grantInfos, accessControlList);
                objectInfo.setGrants(grantInfos);
                objectInfos.add(objectInfo);

                objectInfo.setObjectName(key);
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

    public DeleteObjectResponseType DeleteObject (DeleteObjectType request) throws EucalyptusCloudException {
        DeleteObjectResponseType reply = (DeleteObjectResponseType) request.getReply();
        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfos = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfos);

        if (bucketList.size() > 0) {
            BucketInfo bucketInfo = bucketList.get(0);
            ObjectInfo foundObject = null;

            for (ObjectInfo objectInfo: bucketInfo.getObjects()) {
                if (objectInfo.getObjectName().equals(objectName)) {
                    foundObject = objectInfo;
                }
            }

            if (foundObject != null) {
                if (foundObject.canWrite(userId)) {
                    bucketInfo.getObjects().remove(foundObject);
                    for (GrantInfo grantInfo: foundObject.getGrants()) {
                        db.getEntityManager().remove(grantInfo);
                    }
                    Long size = foundObject.getSize();
                    bucketInfo.setBucketSize(bucketInfo.getBucketSize() - size);
                    db.getEntityManager().remove(foundObject);
                    try {
                        storageManager.deleteObject(bucketName, objectName);
                    } catch (IOException ex) {
                        db.rollback();
                        LOG.warn(ex, ex);
                        throw new EucalyptusCloudException(objectName);
                    }
                    reply.setCode("200");
                    reply.setDescription("OK");
                } else {
                    db.rollback();
                    throw new AccessDeniedException(objectName);
                }
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectName);
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public ListBucketResponseType ListBucket(ListBucketType request) throws EucalyptusCloudException {
        ListBucketResponseType reply = (ListBucketResponseType) request.getReply();
        String bucketName = request.getBucket();
        String userId = request.getUserId();
        String prefix = request.getPrefix();
        String marker = request.getMarker();

        int maxKeys = -1;
        String maxKeysString = request.getMaxKeys();
        if(maxKeysString != null)
            maxKeys = Integer.parseInt(maxKeysString);

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
                if(prefix != null) {
                    reply.setPrefix(prefix);
                }
                if(marker != null)
                    reply.setMarker(marker);
                if(delimiter != null)
                    reply.setDelimiter(delimiter);
                List<ObjectInfo> objectInfos = bucket.getObjects();
                if(objectInfos.size() > 0) {
                    int howManyProcessed = 0;
                    ArrayList<ListEntry> contents = new ArrayList<ListEntry>();
                    for(ObjectInfo objectInfo: objectInfos) {
                        String objectName = objectInfo.getObjectName();
                        if(marker != null) {
                            if(objectName.compareTo(marker) < 0)
                                continue;
                        }
                        if(prefix != null) {
                            if(!objectName.startsWith(prefix)) {
                                continue;
                            } else {
                                if(delimiter != null) {
                                    String[] parts = objectName.substring(prefix.length()).split(delimiter);
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
                        listEntry.setKey(objectName);
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
                        objectInfo.getMetaData(metaData);
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

    public GetObjectAccessControlPolicyResponseType GetObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request.getReply();

        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();
        String ownerId = null;

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        AccessControlListType accessControlList = new AccessControlListType();

        if (bucketList.size() > 0) {
            //construct access control policy from grant infos
            BucketInfo bucket = bucketList.get(0);
            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if (objectInfo.getObjectName().equals(objectName)) {
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
                                addPermission(grants, grantUserInfos.get(0), grantInfo);
                            }
                        }
                        accessControlList.setGrants(grants);
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
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

    public SetBucketAccessControlPolicyResponseType SetBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException
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
            if (bucket.canWriteACP(userId) && accessControlPolicy.getOwner().getDisplayName().equals(bucket.getOwnerId())) {
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

    public SetObjectAccessControlPolicyResponseType SetObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        SetObjectAccessControlPolicyResponseType reply = (SetObjectAccessControlPolicyResponseType) request.getReply();
        String userId = request.getUserId();
        AccessControlPolicyType accessControlPolicy = request.getAccessControlPolicy();
        String bucketName = request.getBucket();
        String objectName = request.getKey();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);
            ObjectInfo foundObject = null;
            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if (objectInfo.canWriteACP(userId) && accessControlPolicy.getOwner().getDisplayName().equals(objectInfo.getOwnerId())) {
                        foundObject = objectInfo;
                        break;
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
            }

            if(foundObject != null) {
                List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
                AccessControlListType accessControlList = accessControlPolicy.getAccessControlList();
                foundObject.resetGlobalGrants();
                foundObject.addGrants(foundObject.getOwnerId(), grantInfos, accessControlList);
                foundObject.setGrants(grantInfos);

                reply.setCode("204");
                reply.setDescription("OK");
            } else {
                db.rollback();
                throw new NoSuchEntityException(objectName);
            }
        }   else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetObjectResponseType GetObject(GetObjectType request) throws EucalyptusCloudException {
        GetObjectResponseType reply = (GetObjectResponseType) request.getReply();
        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId)) {
                        if(request.getGetMetaData()) {
                            ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
                            objectInfo.getMetaData(metaData);
                            reply.setMetaData(metaData);
                        }
                        if(request.getGetData()) {
                            if(request.getInlineData()) {
                                try {
                                    byte[] bytes = new byte[WalrusQueryDispatcher.DATA_MESSAGE_SIZE];
                                    int bytesRead = 0;
                                    String base64Data = "";
                                    while((bytesRead = storageManager.readObject(bucketName, objectName, bytes, bytesRead)) > 0) {
                                        base64Data += new String(bytes, 0, bytesRead);
                                    }
                                    reply.setBase64Data(base64Data);
                                } catch (IOException ex) {
                                    db.rollback();
                                    //set error code
                                    return reply;
                                }
                            } else {
                                //support for large objects
                                String key = bucketName + "." + objectName;
                                String randomKey = key + "." + Hashes.getRandom(10);
                                request.setRandomKey(randomKey);
                                LinkedBlockingQueue<WalrusDataMessage> getQueue = WalrusQueryDispatcher.getReadMessenger().getQueue(key, randomKey);

                                Reader reader = new Reader(bucketName, objectName, objectInfo.getSize(), getQueue);
                                reader.start();
                            }
                        }
                        reply.setEtag(objectInfo.getEtag());
                        reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                        reply.setSize(objectInfo.getSize());
                        Status status = new Status();
                        status.setCode(200);
                        status.setDescription("OK");
                        reply.setStatus(status);
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
            }
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
        db.commit();
        return reply;
    }

    public GetObjectExtendedResponseType GetObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
        GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request.getReply();
        long byteRangeStart = request.getByteRangeStart();
        long byteRangeEnd = request.getByteRangeEnd();
        Date ifModifiedSince = request.getIfModifiedSince();
        Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
        String ifMatch = request.getIfMatch();
        String ifNoneMatch = request.getIfNoneMatch();
        boolean returnCompleteObjectOnFailure = request.getReturnCompleteObjectOnConditionFailure();

        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();
        Status status = new Status();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId)) {
                        String etag = objectInfo.getEtag();
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
                            if((ifUnmodifiedSince.getTime() <= lastModified.getTime()) && !returnCompleteObjectOnFailure) {
                                db.rollback();
                                throw new PreconditionFailedException(lastModified.toString());
                            }
                        }
                        if(request.getGetMetaData()) {
                            ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
                            objectInfo.getMetaData(metaData);
                            reply.setMetaData(metaData);
                        }
                        if(request.getGetData()) {
                            String key = bucketName + "." + objectName;
                            String randomKey = key + "." + Hashes.getRandom(10);
                            request.setRandomKey(randomKey);
                            LinkedBlockingQueue<WalrusDataMessage> getQueue = WalrusQueryDispatcher.getReadMessenger().getQueue(key, randomKey);

                            Reader reader = new Reader(bucketName, objectName, objectInfo.getSize(), getQueue, byteRangeStart, byteRangeEnd);
                            reader.start();
                        }
                        reply.setEtag(objectInfo.getEtag());
                        reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                        reply.setSize(objectInfo.getSize());
                        status.setCode(200);
                        status.setDescription("OK");
                        reply.setStatus(status);
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
            }
        }
        db.commit();
        return reply;
    }

    public GetBucketLocationResponseType GetBucketLocation(GetBucketLocationType request) throws EucalyptusCloudException {
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

    public CopyObjectResponseType CopyObject(CopyObjectType request) throws EucalyptusCloudException {
        CopyObjectResponseType reply = (CopyObjectResponseType) request.getReply();

        throw new NotImplementedException("CopyObject");
    }

    public GetBucketLoggingStatusResponseType GetBucketLoggingStatus(GetBucketLoggingStatusType request) throws EucalyptusCloudException {
        GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request.getReply();

        throw new NotImplementedException("GetBucketLoggingStatus");
    }

    public SetBucketLoggingStatusResponseType SetBucketLoggingStatus(SetBucketLoggingStatusType request) throws EucalyptusCloudException {
        SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request.getReply();

        throw new NotImplementedException("SetBucketLoggingStatus");
    }

    private boolean canVerifySignature(Signature sigVerifier, X509Certificate cert, String signature, String verificationString) throws Exception {
        PublicKey publicKey = cert.getPublicKey();
        sigVerifier.initVerify(publicKey);
        sigVerifier.update((verificationString).getBytes());
        return sigVerifier.verify(hexToBytes(signature));
    }

    private String decryptImage(String bucketName, String objectName, String userId, boolean isAdministrator) throws EucalyptusCloudException {
        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId)) {
                        File file = new File(storageManager.getObjectPath(bucketName, objectName));
                        XMLParser parser = new XMLParser(file);
//Read manifest
                        String imageKey = parser.getValue("//image/name");
                        String encryptedKey = parser.getValue("//ec2_encrypted_key");
                        String encryptedIV = parser.getValue("//ec2_encrypted_iv");
                        String signature = parser.getValue("//signature");

                        AbstractKeyStore userKeyStore = UserKeyStore.getInstance();

                        String image = parser.getXML("image");
                        String machineConfiguration = parser.getXML("machine_configuration");

                        String verificationString = machineConfiguration + image;

                        Signature sigVerifier;
                        try {
                            sigVerifier = Signature.getInstance("SHA1withRSA");
                        } catch (NoSuchAlgorithmException ex) {
                            LOG.error(ex, ex);
                            throw new DecryptionFailedException("SHA1withRSA not found");
                        }

                        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
                        UserInfo userInfo = new UserInfo(userId);
                        List<UserInfo> foundUserInfos = db2.query(userInfo);
                        if(foundUserInfos.size() == 0) {
                            db2.rollback();
                            db.rollback();
                            throw new AccessDeniedException(userId);
                        }

                        if(isAdministrator) {
                            try {
                                boolean verified = false;
                                List<String> aliases = userKeyStore.getAliases();
                                for(String alias : aliases) {
                                    X509Certificate cert = userKeyStore.getCertificate(alias);
                                    verified = canVerifySignature(sigVerifier, cert, signature, verificationString);
                                    if(verified)
                                        break;
                                }
                                if(!verified) {
                                    throw new NotAuthorizedException("Invalid signature");
                                }
                            } catch (Exception ex) {
                                db2.rollback();
                                db.rollback();
                                LOG.error(ex, ex);
                                throw new DecryptionFailedException("signature verification");
                            }
                        } else {
                            List<CertificateInfo> certInfos = foundUserInfos.get(0).getCertificates();
                            boolean signatureVerified = false;
                            for(CertificateInfo certInfo: certInfos) {
                                String alias = certInfo.getCertAlias();
                                try {
                                    X509Certificate cert = userKeyStore.getCertificate(alias);
                                    signatureVerified = canVerifySignature(sigVerifier, cert, signature, verificationString);
                                    if (signatureVerified)
                                        break;
                                } catch(Exception ex) {
                                    db2.rollback();
                                    db.rollback();
                                    LOG.error(ex, ex);
                                    throw new DecryptionFailedException("signature verification");
                                }
                            }
                            if(!signatureVerified) {
                                throw new NotAuthorizedException("Invalid signature");
                            }
                        }
                        List<String> parts = parser.getValues("//image/parts/part/filename");
                        ArrayList<String> qualifiedPaths = new ArrayList<String>();

                        for (String part: parts) {
                            qualifiedPaths.add(storageManager.getObjectPath(bucketName, part));
                        }
                        //Assemble parts
                        String encryptedImageKey = imageKey + "-" + Hashes.getRandom(5) + ".crypt.gz";
                        String encryptedImageName = storageManager.getObjectPath(bucketName, encryptedImageKey);
                        String decryptedImageKey = encryptedImageKey.replaceAll("crypt.gz", "tgz");
                        String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
                        assembleParts(encryptedImageName, qualifiedPaths);
//Decrypt key and IV

                        byte[] key;
                        byte[] iv;
                        try {
                            PrivateKey pk = (PrivateKey) userKeyStore.getKey(EucalyptusProperties.NAME, EucalyptusProperties.NAME);
                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            cipher.init(Cipher.DECRYPT_MODE, pk);
                            key = hexToBytes(new String(cipher.doFinal(hexToBytes(encryptedKey))));
                            iv = hexToBytes(new String(cipher.doFinal(hexToBytes(encryptedIV))));
                        } catch(Exception ex) {
                            db2.rollback();
                            db.rollback();
                            LOG.error(ex, ex);
                            throw new DecryptionFailedException("AES params");
                        }

                        //Unencrypt image
                        try {
                            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                            IvParameterSpec salt = new IvParameterSpec(iv);
                            SecretKey keySpec = new SecretKeySpec(key, "AES");
                            cipher.init(Cipher.DECRYPT_MODE, keySpec, salt);
                            decryptImage(encryptedImageName, decryptedImageName, cipher);
                        } catch (Exception ex) {
                            db2.rollback();
                            db.rollback();
                            LOG.error(ex, ex);
                            throw new DecryptionFailedException("decryption failed");
                        }
                        try {
                            storageManager.deleteAbsoluteObject(encryptedImageName);
                        } catch (Exception ex) {
                            LOG.warn(ex, ex);
                            throw new EucalyptusCloudException();
                        }
                        db2.commit();
                        db.commit();
                        return decryptedImageKey;
                    }
                }
            }
        }
        return null;
    }

    private void checkManifest(String bucketName, String objectName, String userId) throws EucalyptusCloudException {
        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId)) {
                        File file = new File(storageManager.getObjectPath(bucketName, objectName));
                        XMLParser parser = new XMLParser(file);
//Read manifest
                        String imageKey = parser.getValue("//image/name");
                        String encryptedKey = parser.getValue("//ec2_encrypted_key");
                        String encryptedIV = parser.getValue("//ec2_encrypted_iv");
                        String signature = parser.getValue("//signature");

                        AbstractKeyStore userKeyStore = UserKeyStore.getInstance();

                        String image = parser.getXML("image");
                        String machineConfiguration = parser.getXML("machine_configuration");

                        EntityWrapper<UserInfo> db2 = new EntityWrapper<UserInfo>();
                        UserInfo userInfo = new UserInfo(userId);
                        List<UserInfo> foundUserInfos = db2.query(userInfo);
                        if(foundUserInfos.size() == 0) {
                            db2.rollback();
                            db.rollback();
                            throw new AccessDeniedException(userId);
                        }

                        List<CertificateInfo> certInfos = foundUserInfos.get(0).getCertificates();
                        boolean signatureVerified = false;

                        Signature sigVerifier;
                        try {
                            sigVerifier = Signature.getInstance("SHA1withRSA");
                        } catch (NoSuchAlgorithmException ex) {
                            LOG.error(ex, ex);
                            throw new DecryptionFailedException("SHA1withRSA not found");
                        }

                        for(CertificateInfo certInfo: certInfos) {
                            String alias = certInfo.getCertAlias();
                            try {
                                X509Certificate cert = userKeyStore.getCertificate(alias);
                                PublicKey publicKey = cert.getPublicKey();
                                sigVerifier.initVerify(publicKey);
                                sigVerifier.update((machineConfiguration + image).getBytes());
                                signatureVerified = sigVerifier.verify(hexToBytes(signature));
                                if (signatureVerified) {
                                    break;
                                }
                            } catch(Exception ex) {
                                db2.rollback();
                                db.rollback();
                                LOG.error(ex, ex);
                                throw new DecryptionFailedException("signature verification");
                            }
                        }

                        if(!signatureVerified) {
                            throw new NotAuthorizedException("Invalid signature");
                        }
                        //Decrypt key and IV

                        byte[] key;
                        byte[] iv;
                        try {
                            PrivateKey pk = (PrivateKey) userKeyStore.getKey(EucalyptusProperties.NAME, EucalyptusProperties.NAME);
                            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                            cipher.init(Cipher.DECRYPT_MODE, pk);
                            key = hexToBytes(new String(cipher.doFinal(hexToBytes(encryptedKey))));
                            iv = hexToBytes(new String(cipher.doFinal(hexToBytes(encryptedIV))));
                        } catch(Exception ex) {
                            db2.rollback();
                            db.rollback();
                            LOG.error(ex, ex);
                            throw new DecryptionFailedException("AES params");
                        }
                        db2.commit();
                        db.commit();
                    }
                }
            }
        }
    }

    public GetDecryptedImageResponseType GetDecryptedImage(GetDecryptedImageType request) throws EucalyptusCloudException {
        GetDecryptedImageResponseType reply = (GetDecryptedImageResponseType) request.getReply();
        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);

        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId) || request.isAdministrator() ) {
                        WalrusSemaphore semaphore = imageMessenger.getSemaphore(bucketName + "/" + objectName);
                        try {
                            semaphore.acquire();
                        } catch(InterruptedException ex) {
                            throw new EucalyptusCloudException("semaphore could not be acquired");
                        }
                        EntityWrapper<ImageCacheInfo> db2 = new EntityWrapper<ImageCacheInfo>();
                        ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectName);
                        List<ImageCacheInfo> foundImageCacheInfos = db2.query(searchImageCacheInfo);

                        if(foundImageCacheInfos.size() == 0) {
                            db2.commit();
//issue a cache request
                            cacheImage(bucketName, objectName, userId, request.isAdministrator());
//query db again
                            db2 = new EntityWrapper<ImageCacheInfo>();
                            foundImageCacheInfos = db2.query(searchImageCacheInfo);
                        }
                        ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
                        if(!foundImageCacheInfo.getInCache()) {
                            WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + objectName);
                            synchronized (monitor) {
                                try {
                                    monitor.wait();
                                } catch(Exception ex) {
                                    LOG.warn(ex, ex);
                                    db2.rollback();
                                    db.rollback();
                                    throw new EucalyptusCloudException("monitor failure");
                                }
                            }
                            //caching may have modified the db. repeat the query
                            db2.commit();
                            db2 = new EntityWrapper<ImageCacheInfo>();
                            foundImageCacheInfos = db2.query(searchImageCacheInfo);
                            if(foundImageCacheInfos.size() > 0) {
                                foundImageCacheInfo = foundImageCacheInfos.get(0);
                                foundImageCacheInfo.setUseCount(foundImageCacheInfo.getUseCount() + 1);
                                assert(foundImageCacheInfo.getInCache());
                            } else {
                                db.rollback();
                                db2.rollback();
                                throw new NoSuchEntityException(objectName);
                            }
                        }

                        Long unencryptedSize = foundImageCacheInfo.getSize();
                        String imageKey = foundImageCacheInfo.getImageName();
                        String queueKey = bucketName + "." + objectName;
                        String randomKey = queueKey + "." + Hashes.getRandom(10);
                        request.setRandomKey(randomKey);

                        LinkedBlockingQueue<WalrusDataMessage> getQueue = WalrusQueryDispatcher.getReadMessenger().getQueue(queueKey, randomKey);
                        reply.setSize(unencryptedSize);
                        reply.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                        reply.setEtag("");
                        Reader reader = new Reader(bucketName, imageKey, unencryptedSize, getQueue, false, semaphore);
                        reader.start();
                        db.commit();
                        db2.commit();
                        return reply;
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
            }
            db.rollback();
            throw new NoSuchEntityException(objectName);
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
    }

    public CheckImageResponseType CheckImage(CheckImageType request) throws EucalyptusCloudException {
        CheckImageResponseType reply = (CheckImageResponseType) request.getReply();
        reply.setSuccess(false);
        String bucketName = request.getBucket();
        String objectName = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(objectName)) {
                    if(objectInfo.canRead(userId)) {
                        checkManifest(bucketName, objectName, userId);
                        reply.setSuccess(true);
                        db.commit();
                        return reply;
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(objectName);
                    }
                }
            }
            db.rollback();
            throw new NoSuchEntityException(objectName);
        } else {
            db.rollback();
            throw new NoSuchBucketException(bucketName);
        }
    }

    private synchronized void cacheImage(String bucketName, String manifestKey, String userId, boolean isAdministrator) throws EucalyptusCloudException {
        EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
        ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
        List<ImageCacheInfo> imageCacheInfos = db.query(searchImageCacheInfo);

        if(imageCacheInfos.size() == 0) {
            String decryptedImageKey = decryptImage(bucketName, manifestKey, userId, isAdministrator);
//decryption worked. Add it.
            ImageCacheInfo foundImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
            foundImageCacheInfo.setImageName(decryptedImageKey);
            foundImageCacheInfo.setInCache(false);
            foundImageCacheInfo.setCaching(true);
            foundImageCacheInfo.setUseCount(0);
            foundImageCacheInfo.setSize(0L);
            db.add(foundImageCacheInfo);
            db.commit();
//decrypt, unzip, untar image in the background
            ImageCacher imageCacher = new ImageCacher(bucketName, manifestKey, decryptedImageKey);
            imageCacher.run();
        } 
    }

    public CacheImageResponseType CacheImage(CacheImageType request) throws EucalyptusCloudException {
        CacheImageResponseType reply = (CacheImageResponseType) request.getReply();
        reply.setSuccess(false);
        String bucketName = request.getBucket();
        String manifestKey = request.getKey();
        String userId = request.getUserId();

        EntityWrapper<BucketInfo> db = new EntityWrapper<BucketInfo>();
        BucketInfo bucketInfo = new BucketInfo(bucketName);
        List<BucketInfo> bucketList = db.query(bucketInfo);


        if (bucketList.size() > 0) {
            BucketInfo bucket = bucketList.get(0);

            for(ObjectInfo objectInfo: bucket.getObjects()) {
                if(objectInfo.getObjectName().equals(manifestKey)) {
                    if(objectInfo.canRead(userId)) {
                        EntityWrapper<ImageCacheInfo> db2 = new EntityWrapper<ImageCacheInfo>();
                        ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
                        List<ImageCacheInfo> foundImageCacheInfos = db2.query(searchImageCacheInfo);
                        db2.commit();
                        if(foundImageCacheInfos.size() == 0) {
                            cacheImage(bucketName, manifestKey, userId, request.isAdministrator());
                            reply.setSuccess(true);
                        }
                        return reply;
                    } else {
                        db.rollback();
                        throw new AccessDeniedException(manifestKey);
                    }

                }
            }
        }
        throw new NoSuchEntityException(manifestKey);
    }

    public FlushCachedImageResponseType FlushCachedImage(FlushCachedImageType request) throws EucalyptusCloudException {
        FlushCachedImageResponseType reply = (FlushCachedImageResponseType) request.getReply();

        String bucketName = request.getBucket();
        String manifestKey = request.getKey();

        EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
        ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
        List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);

        if(foundImageCacheInfos.size() > 0) {
            ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
            if(foundImageCacheInfo.getInCache() && !foundImageCacheInfo.getCaching()) {
//check that there are no operations in progress and then flush cache and delete image file
                db.commit();
                ImageCacheFlusher imageCacheFlusher = new ImageCacheFlusher(bucketName, manifestKey);
                imageCacheFlusher.start();
            } else {
                db.rollback();
                throw new EucalyptusCloudException("not in cache");
            }
        } else {
            db.rollback();
            throw new NoSuchEntityException(bucketName + manifestKey);
        }
        return reply;
    }

    private void flushCachedImage (String bucketName, String objectName) {
        WalrusSemaphore semaphore = imageMessenger.getSemaphore(bucketName + "/" + objectName);
        while(semaphore.inUse()) {
            try {
                synchronized (semaphore) {
                    semaphore.wait();
                }
            } catch(InterruptedException ex) {
                LOG.warn(ex, ex);
            }
        }
        imageMessenger.removeSemaphore(bucketName + "/" + objectName);
        try {
            EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
            ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, objectName);
            List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);

            if(foundImageCacheInfos.size() > 0) {
                ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
                if(foundImageCacheInfo.getInCache() && !foundImageCacheInfo.getCaching()) {
                    db.delete(foundImageCacheInfo);
                    storageManager.deleteObject(bucketName, foundImageCacheInfo.getImageName());
                }
                db.commit();
            } else {
                db.rollback();
                LOG.warn("Cannot find image in cache" + bucketName + "/" + objectName);
            }
        } catch(Exception ex) {
            LOG.warn(ex, ex);
        }
    }

    private class ImageCacheFlusher extends Thread {
        private String bucketName;
        private String objectName;
        public ImageCacheFlusher(String bucketName, String objectName) {
            this.bucketName = bucketName;
            this.objectName = objectName;
        }

        public void run() {
            flushCachedImage(bucketName, objectName);
        }
    }

    private class ImageCacher extends Thread {

        private String bucketName;
        private String manifestKey;
        private String decryptedImageKey;

        public ImageCacher(String bucketName, String manifestKey, String decryptedImageKey) {
            this.bucketName = bucketName;
            this.manifestKey = manifestKey;
            this.decryptedImageKey = decryptedImageKey;
        }

        private long tryToCache(String decryptedImageName, String tarredImageName, String imageName) {
            Long unencryptedSize = 0L;
            boolean failed = false;
            try {
                unzipImage(decryptedImageName, tarredImageName);
                unencryptedSize = untarImage(tarredImageName, imageName);
                Long oldCacheSize = 0L;
                EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
                List<ImageCacheInfo> imageCacheInfos = db.query(new ImageCacheInfo());
                for(ImageCacheInfo imageCacheInfo: imageCacheInfos) {
                    if(imageCacheInfo.getInCache()) {
                        oldCacheSize += imageCacheInfo.getSize();
                    }
                }
                db.commit();
                if((oldCacheSize + unencryptedSize) > WalrusProperties.IMAGE_CACHE_SIZE) {
                    failed = true;
                }
            } catch(Exception ex) {
                LOG.warn(ex, ex);
                //try to evict an entry and try again
                failed = true;
            }
            if(failed) {
                try {
                    storageManager.deleteAbsoluteObject(decryptedImageName);
                    storageManager.deleteAbsoluteObject(tarredImageName);
                } catch (Exception exception) {
                    LOG.warn(exception, exception);
                }
                return -1L;
            }
            return unencryptedSize;
        }

        public void run() {
            //update status
            //wake up any waiting consumers
            String decryptedImageName = storageManager.getObjectPath(bucketName, decryptedImageKey);
            String tarredImageName = decryptedImageName.replaceAll("tgz", "tar");
            String imageName = tarredImageName.replaceAll(".tar", "");
            String imageKey = decryptedImageKey.replaceAll(".tgz", "");
            Long unencryptedSize;
            while((unencryptedSize = tryToCache(decryptedImageName, tarredImageName, imageName)) < 0) {
                EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
                List<ImageCacheInfo> imageCacheInfos = db.query(new ImageCacheInfo());
                ImageCacheInfo imageCacheInfo = null;
                if(imageCacheInfos.size() > 1) {
                    Collections.sort(imageCacheInfos);
                    imageCacheInfo = imageCacheInfos.get(0);
                    break;
                }
                db.commit();
                if(imageCacheInfo != null && imageCacheInfo.getInCache()) {
                    flushCachedImage(imageCacheInfo.getBucketName(), imageCacheInfo.getManifestName());
                }
            }
            try {
                storageManager.deleteAbsoluteObject(decryptedImageName);
                storageManager.deleteAbsoluteObject(tarredImageName);

                EntityWrapper<ImageCacheInfo>db = new EntityWrapper<ImageCacheInfo>();
                ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo(bucketName, manifestKey);
                List<ImageCacheInfo> foundImageCacheInfos = db.query(searchImageCacheInfo);
                if(foundImageCacheInfos.size() > 0) {
                    ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
                    foundImageCacheInfo.setImageName(imageKey);
                    foundImageCacheInfo.setInCache(true);
                    foundImageCacheInfo.setCaching(false);
                    foundImageCacheInfo.setSize(unencryptedSize);
                    db.commit();
                    //wake up waiters
                    WalrusMonitor monitor = imageMessenger.getMonitor(bucketName + "/" + manifestKey);
                    synchronized (monitor) {
                        monitor.notifyAll();
                    }
                    imageMessenger.removeMonitor(bucketName + "/" + manifestKey);

                } else {
                    db.rollback();
                    LOG.warn("Could not expand image" + decryptedImageName);
                }
            } catch (Exception ex) {
                LOG.warn(ex, ex);
            }
        }
    }

    private void unzipImage(String decryptedImageName, String tarredImageName) throws Exception {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(new File(decryptedImageName)));
        File outFile = new File(tarredImageName);
        ReadableByteChannel inChannel = Channels.newChannel(in);
        WritableByteChannel outChannel = new FileOutputStream(outFile).getChannel();

        ByteBuffer buffer = ByteBuffer.allocate(WalrusQueryDispatcher.DATA_MESSAGE_SIZE);
        while (inChannel.read(buffer) != -1) {
            buffer.flip();
            outChannel.write(buffer);
            buffer.clear();
        }
        outChannel.close();
        inChannel.close();
    }

    private long untarImage(String tarredImageName, String imageName) throws Exception {
        TarInputStream in = new TarInputStream(new FileInputStream(new File(tarredImageName)));
        File outFile = new File(imageName);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

        TarEntry tEntry = in.getNextEntry();
        assert(!tEntry.isDirectory());

        in.copyEntryContents(out);
        out.close();
        in.close();
        return outFile.length();
    }

    private void decryptImage(String encryptedImageName, String decyptedImageName, Cipher cipher) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(decyptedImageName)));
            File inFile = new File(encryptedImageName);
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));

            int bytesRead = 0;
            byte[] bytes = new byte[8192];

            while((bytesRead = in.read(bytes)) > 0) {
                byte[] outBytes = cipher.update(bytes, 0, bytesRead);
                out.write(outBytes);
            }
            byte[] outBytes = cipher.doFinal();
            out.write(outBytes);
            in.close();
            out.close();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void assembleParts(String name, List<String> parts) {
        try {
            FileChannel out = new FileOutputStream(new File(name)).getChannel();
            for (String partName: parts) {
                FileChannel in = new FileInputStream(new File(partName)).getChannel();
                in.transferTo(0, in.size(), out);
                in.close();
            }
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private byte[] hexToBytes(String data) {
        int k = 0;
        byte[] results = new byte[data.length() / 2];
        for (int i = 0; i < data.length();) {
            results[k] = (byte) (Character.digit(data.charAt(i++), 16) << 4);
            results[k] += (byte) (Character.digit(data.charAt(i++), 16));
            k++;
        }

        return results;
    }


    private String bytesToHex(byte[] data) {
        StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i < data.length; i++ ) {
            buffer.append( byteToHex(data[i]) );
        }
        return(buffer.toString());
    }

    private String byteToHex(byte data) {
        StringBuffer hexString =  new StringBuffer();
        hexString.append(toHex((data>>>4)&0x0F));
        hexString.append(toHex(data&0x0F));
        return hexString.toString();
    }

    private char toHex(int value) {
        if ((0 <= value) && (value <= 9 ))
            return (char)('0' + value);
        else
            return (char)('a' + (value-10));
    }

    class Reader extends Thread {

        private String bucketName;
        private String objectName;
        private long objectSize;
        private LinkedBlockingQueue<WalrusDataMessage> getQueue;
        private long byteRangeStart;
        private long byteRangeEnd;
        private boolean deleteAfterXfer;
        private WalrusSemaphore semaphore;

        public Reader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue) {
            this.bucketName = bucketName;
            this.objectName = objectName;
            this.objectSize = objectSize;
            this.getQueue = getQueue;
        }


        public Reader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, long byteRangeStart, long byteRangeEnd) {
            this.bucketName = bucketName;
            this.objectName = objectName;
            this.objectSize = objectSize;
            this.getQueue = getQueue;
            this.byteRangeStart = byteRangeStart;
            this.byteRangeEnd = byteRangeEnd;
        }

        public Reader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, boolean deleteAfterXfer, WalrusSemaphore semaphore) {
            this(bucketName, objectName, objectSize, getQueue);
            this.deleteAfterXfer = deleteAfterXfer;
            this.semaphore = semaphore;
        }

        public void run() {
            byte[] bytes = new byte[WalrusQueryDispatcher.DATA_MESSAGE_SIZE];

            long bytesRemaining = objectSize;
            long offset = byteRangeStart;

            if(byteRangeEnd != 0) {
                assert(byteRangeEnd <= objectSize);
                assert(byteRangeEnd >= byteRangeStart);
                bytesRemaining = byteRangeEnd - byteRangeStart;
            }

            try {
                getQueue.put(WalrusDataMessage.StartOfData(bytesRemaining));

                while (bytesRemaining > 0) {
                    int bytesRead = storageManager.readObject(bucketName, objectName, bytes, offset);
                    bytesRemaining -= bytesRead;
                    getQueue.put(WalrusDataMessage.DataMessage(bytes, bytesRead));
                    offset += bytesRead;
                }
                getQueue.put(WalrusDataMessage.EOF());
            } catch (Exception ex) {
                LOG.error( ex,ex );
            }
            if(semaphore != null) {
                semaphore.release();
                synchronized (semaphore) {
                    semaphore.notifyAll();
                }
            }
            if(deleteAfterXfer) {
                try {
                    storageManager.deleteObject(bucketName, objectName);
                } catch(Exception ex) {
                    LOG.error( ex,ex );
                }
            }
        }
    }

    public StoreSnapshotResponseType StoreSnapshot(StoreSnapshotType request) throws EucalyptusCloudException {
        StoreSnapshotResponseType reply = (StoreSnapshotResponseType) request.getReply();
        String snapshotId = request.getKey();
        String bucketName = request.getBucket();
        String snapshotVgName = request.getSnapshotvgname();
        String snapshotLvName = request.getSnapshotlvname();
        boolean createBucket = true;

        EntityWrapper<WalrusSnapshotSet> db = new EntityWrapper<WalrusSnapshotSet>();
        WalrusSnapshotSet snapshotSet = new WalrusSnapshotSet(bucketName);
        List<WalrusSnapshotSet> snapshotSets = db.query(snapshotSet);

        WalrusSnapshotSet foundSnapshotSet;
        if(snapshotSets.size() == 0) {
            foundSnapshotSet = snapshotSet;
            createBucket = true;
            db.add(foundSnapshotSet);
        } else {
            foundSnapshotSet = snapshotSets.get(0);
        }

/*        List<WalrusSnapshotInfo> snapshotInfos = foundSnapshotSet.getSnapshotSet();
        for(WalrusSnapshotInfo snapInfo: snapshotInfos) {
            if(snapInfo.getSnapshotId().equals(snapshotId)) {
                db.rollback();
                throw new EntityAlreadyExistsException(snapshotId);
            }
        }

        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        //create a snapshot set
        snapshotInfo.setSnapshotSetId(bucketName);
        snapshotInfo.setVgName(snapshotVgName);
        snapshotInfo.setLvName(snapshotLvName);
        snapshotInfo.setTransferred(false);
        EntityWrapper<WalrusSnapshotInfo> dbSnap = db.recast(WalrusSnapshotInfo.class);
        snapshotInfos.add(snapshotInfo);
        dbSnap.add(snapshotInfo); */

        //read and store it
        db.commit();
        //convert to a PutObject request
        //Make sure the bucket exists

        String userId = request.getUserId();
        if(createBucket) {
            CreateBucketType createBucketRequest = new CreateBucketType();
            createBucketRequest.setUserId(userId);
            createBucketRequest.setBucket(bucketName);
            try {
                CreateBucket(createBucketRequest);
            } catch(EucalyptusCloudException ex) {
                if(!(ex instanceof BucketAlreadyExistsException || ex instanceof BucketAlreadyOwnedByYouException)) {
                    db.rollback();
                    throw ex;
                }
            }
        }

        //put happens synchronously
        PutObjectType putObjectRequest = new PutObjectType();
        putObjectRequest.setUserId(userId);
        putObjectRequest.setBucket(bucketName);
        putObjectRequest.setKey(snapshotId);
        putObjectRequest.setRandomKey(request.getRandomKey());
        PutObjectResponseType putObjectResponseType = PutObject(putObjectRequest);
        reply.setEtag(putObjectResponseType.getEtag());
        reply.setLastModified(putObjectResponseType.getLastModified());
        reply.setStatusMessage(putObjectResponseType.getStatusMessage());

        //change state 
        /* snapshotInfo = new WalrusSnapshotInfo(snapshotId);
      dbSnap = new EntityWrapper<WalrusSnapshotInfo>();
      WalrusSnapshotInfo foundSnapshotInfo = dbSnap.getUnique(snapshotInfo);
      foundSnapshotInfo.setTransferred(true);
      dbSnap.commit();  */

        return reply;
    }

    public GetSnapshotInfoResponseType GetSnapshotInfo(GetSnapshotInfoType request) throws EucalyptusCloudException {
        GetSnapshotInfoResponseType reply = (GetSnapshotInfoResponseType) request.getReply();
        String snapshotId = request.getKey();
        ArrayList<String> snapshotSet = reply.getSnapshotSet();

        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);

        if(snapshotInfos.size() > 0) {
            WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
            EntityWrapper<WalrusSnapshotSet> dbSet = db.recast(WalrusSnapshotSet.class);
            try {
                String snapshotSetId = foundSnapshotInfo.getSnapshotSetId();
                WalrusSnapshotSet snapshotSetInfo = dbSet.getUnique(new WalrusSnapshotSet(snapshotSetId));
                List<WalrusSnapshotInfo> walrusSnapshotInfos = snapshotSetInfo.getSnapshotSet();
                //bucket name
                reply.setBucket(snapshotSetId);
                //the volume is the snapshot at time 0
                for(WalrusSnapshotInfo walrusSnapshotInfo : walrusSnapshotInfos) {
                    snapshotSet.add(walrusSnapshotInfo.getSnapshotId());
                }
            } catch(Exception ex) {
                db.rollback();
                throw new NoSuchEntityException(snapshotId);
            }
        } else {
            db.rollback();
            throw new NoSuchEntityException(snapshotId);
        }
        db.commit();
        return reply;
    }

    public GetVolumeResponseType GetVolume(GetVolumeType request) throws EucalyptusCloudException {
        GetVolumeResponseType reply = (GetVolumeResponseType) request.getReply();
        String snapshotId = request.getKey();
        String userId = request.getUserId();
        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);

        if(snapshotInfos.size() > 0) {
            WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
            String snapshotSetId = foundSnapshotInfo.getSnapshotSetId();
            EntityWrapper<WalrusSnapshotSet> dbSet = db.recast(WalrusSnapshotSet.class);
            WalrusSnapshotSet snapSet = new WalrusSnapshotSet(snapshotSetId);
            List<WalrusSnapshotSet> snapshotSets = dbSet.query(snapSet);
            if(snapshotSets.size() > 0) {
                WalrusSnapshotSet snapshotSet = snapshotSets.get(0);
                List<WalrusSnapshotInfo> snapshots = snapshotSet.getSnapshotSet();
                ArrayList<String> snapshotIds = new ArrayList<String>();
                ArrayList<String> vgNames = new ArrayList<String>();
                ArrayList<String> lvNames = new ArrayList<String>();
                for(WalrusSnapshotInfo snap : snapshots) {
                    snapshotIds.add(snap.getSnapshotId());
                    vgNames.add(snap.getVgName());
                    lvNames.add(snap.getLvName());
                }
                String volumeKey = storageManager.createVolume(snapshotSetId, snapshotIds, vgNames, lvNames, snapshotId, foundSnapshotInfo.getVgName(), foundSnapshotInfo.getLvName());

                AddObject(userId, snapshotSetId, volumeKey);

                GetObjectType getObjectType = new GetObjectType();
                getObjectType.setUserId(request.getUserId());
                getObjectType.setGetData(true);
                getObjectType.setInlineData(false);
                getObjectType.setGetMetaData(false);
                getObjectType.setBucket(snapshotSetId);
                getObjectType.setKey(volumeKey);
                db.commit();
                GetObjectResponseType getObjectResponse = GetObject(getObjectType);
                reply.setEtag(getObjectResponse.getEtag());
                reply.setLastModified(getObjectResponse.getLastModified());
                reply.setSize(getObjectResponse.getSize());
                request.setRandomKey(getObjectType.getRandomKey());
                request.setBucket(snapshotSetId);
                request.setKey(volumeKey);
            } else {
                db.rollback();
                throw new EucalyptusCloudException("Could not find snapshot set");
            }
        } else {
            db.rollback();
            throw new NoSuchSnapshotException(snapshotId);
        }

        return reply;
    }

    public DeleteWalrusSnapshotResponseType DeleteWalrusSnapshot(DeleteWalrusSnapshotType request) throws EucalyptusCloudException {
        DeleteWalrusSnapshotResponseType reply = (DeleteWalrusSnapshotResponseType) request.getReply();
        String snapshotId = request.getKey();

        //Load the entire snapshot tree and then remove the snapshot
        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);

        //Delete is idempotent.
        reply.set_return(true);
        if(snapshotInfos.size() > 0) {
            WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
            if(!foundSnapshotInfo.getTransferred()) {
                db.rollback();
                throw new SnapshotInUseException(snapshotId);
            }
            String snapshotSetId = foundSnapshotInfo.getSnapshotSetId();

            EntityWrapper<WalrusSnapshotSet> dbSet = db.recast(WalrusSnapshotSet.class);
            WalrusSnapshotSet snapshotSetInfo = new WalrusSnapshotSet(snapshotSetId);
            List<WalrusSnapshotSet> snapshotSetInfos = dbSet.query(snapshotSetInfo);

            if(snapshotSetInfos.size() > 0) {
                WalrusSnapshotSet foundSnapshotSetInfo = snapshotSetInfos.get(0);
                String bucketName = foundSnapshotSetInfo.getSnapshotSetId();
                List<WalrusSnapshotInfo> snapshotSet = foundSnapshotSetInfo.getSnapshotSet();
                ArrayList<String> snapshotIds = new ArrayList<String>();
                WalrusSnapshotInfo snapshotSetSnapInfo = null;
                for(WalrusSnapshotInfo snapInfo : snapshotSet) {
                    String snapId = snapInfo.getSnapshotId();
                    snapshotIds.add(snapId);
                    if(snapId.equals(foundSnapshotInfo.getSnapshotId()))
                        snapshotSetSnapInfo = snapInfo;
                }
                //delete from the database
                if(snapshotSetSnapInfo != null)
                    snapshotSet.remove(snapshotSetSnapInfo);
                db.delete(foundSnapshotInfo);
                //remove the snapshot in the background
                SnapshotDeleter snapshotDeleter = new SnapshotDeleter(bucketName, foundSnapshotInfo.getSnapshotId(),
                        foundSnapshotInfo.getVgName(), foundSnapshotInfo.getLvName(), snapshotIds);
                snapshotDeleter.start();
            } else {
                db.rollback();
                throw new NoSuchSnapshotException(snapshotId);
            }
        }
        db.commit();
        return reply;
    }


    private class SnapshotDeleter extends Thread {
        private String bucketName;
        private List<String> snapshotSet;
        private String snapshotId;
        private String vgName;
        private String lvName;

        public SnapshotDeleter(String bucketName, String snapshotId, String vgName, String lvName, List<String> snapshotSet) {
            this.bucketName = bucketName;
            this.snapshotSet = snapshotSet;
            this.snapshotId = snapshotId;
            this.vgName = vgName;
            this.lvName = lvName;
        }

        public void run() {
            try {
                storageManager.deleteSnapshot(bucketName, snapshotId, vgName, lvName, snapshotSet);
            } catch(EucalyptusCloudException ex) {
                LOG.warn(ex, ex);
            }
        }
    }

    public UpdateWalrusConfigurationResponseType UpdateWalrusConfiguration(UpdateWalrusConfigurationType request) {
        UpdateWalrusConfigurationResponseType reply = (UpdateWalrusConfigurationResponseType) request.getReply();
        String rootDir = request.getBucketRootDirectory();
        if(rootDir != null)
            storageManager.setRootDirectory(rootDir);

        return reply;
    }
}
