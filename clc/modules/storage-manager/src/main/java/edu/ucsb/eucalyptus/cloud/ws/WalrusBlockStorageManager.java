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
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WalrusBlockStorageManager {
    private static Logger LOG = Logger.getLogger( WalrusBlockStorageManager.class );
    private StorageManager storageManager;
    private WalrusManager walrusManager;

    public WalrusBlockStorageManager(StorageManager storageManager, WalrusManager walrusManager) {
        this.storageManager = storageManager;
        this.walrusManager = walrusManager;
    }

    public void initialize() {
        WalrusProperties.enableSnapshots = true;
        startupChecks();
        //inform SC in case it is running on the same host
        WalrusProperties.sharedMode = true;
    }

    public void startupChecks() {
        cleanFailedCachedImages();
    }

    public void cleanFailedCachedImages() {
        EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
        ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
        searchImageCacheInfo.setInCache(false);
        List<ImageCacheInfo> icInfos = db.query(searchImageCacheInfo);
        for(ImageCacheInfo icInfo : icInfos) {
            String decryptedImageName = icInfo.getImageName();
            String bucket = icInfo.getBucketName();
            LOG.info("Cleaning failed cache entry: " + bucket + "/" + icInfo.getManifestName());
            try {
                if(decryptedImageName.contains(".tgz")) {
                    storageManager.deleteObject(bucket, decryptedImageName.replaceAll(".tgz", "crypt.gz"));
                    storageManager.deleteObject(bucket, decryptedImageName.replaceAll(".tgz", ".tar"));
                    storageManager.deleteObject(bucket, decryptedImageName.replaceAll(".tgz", ""));
                }
                storageManager.deleteObject(bucket, decryptedImageName);
            } catch(IOException ex) {
                LOG.error(ex);
            }
            db.delete(icInfo);
        }
        db.commit();
    }

    public StoreSnapshotResponseType storeSnapshot(StoreSnapshotType request) throws EucalyptusCloudException {
        StoreSnapshotResponseType reply = (StoreSnapshotResponseType) request.getReply();

        if(!WalrusProperties.enableSnapshots) {
            LOG.warn("Snapshots not enabled. Please check pre-conditions and restart Walrus.");
            return reply;
        }
        String snapshotId = request.getKey();
        String bucketName = request.getBucket();
        boolean createBucket = true;

        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapInfos = db.query(snapshotInfo);
        if(snapInfos.size() > 0) {
            db.rollback();
            throw new EntityAlreadyExistsException(snapshotId);
        }
        db.commit();

        //set snapshot props
        //read and store it
        //convert to a PutObject request

        String userId = request.getUserId();
        if(createBucket) {
            CreateBucketType createBucketRequest = new CreateBucketType();
            createBucketRequest.setUserId(userId);
            createBucketRequest.setBucket(bucketName);
            try {
                walrusManager.createBucket(createBucketRequest);
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
        try {
            PutObjectResponseType putObjectResponseType = walrusManager.putObject(putObjectRequest);
            reply.setEtag(putObjectResponseType.getEtag());
            reply.setLastModified(putObjectResponseType.getLastModified());
            reply.setStatusMessage(putObjectResponseType.getStatusMessage());
            int snapshotSize = (int)(putObjectResponseType.getSize() / WalrusProperties.G);
            if(WalrusProperties.shouldEnforceUsageLimits) {
                int totalSnapshotSize = 0;
                WalrusSnapshotInfo snapInfo = new WalrusSnapshotInfo();
                db = new EntityWrapper<WalrusSnapshotInfo>();
                List<WalrusSnapshotInfo> sInfos = db.query(snapInfo);
                for (WalrusSnapshotInfo sInfo : sInfos) {
                    totalSnapshotSize += sInfo.getSize();
                }
                if((totalSnapshotSize + snapshotSize) > WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE) {
                    db.rollback();
                    throw new EntityTooLargeException(snapshotId);
                }
                db.commit();
            }
            //change state
            snapshotInfo = new WalrusSnapshotInfo(snapshotId);
            db = new EntityWrapper<WalrusSnapshotInfo>();
            snapshotInfo.setSnapshotBucket(bucketName);
            snapshotInfo.setSize(snapshotSize);
            db.add(snapshotInfo);
            db.commit();
        } catch (EucalyptusCloudException ex) {
            db.rollback();
            throw ex;
        }
        return reply;
    }

    public GetWalrusSnapshotResponseType getSnapshot(GetWalrusSnapshotType request) throws EucalyptusCloudException {
        GetWalrusSnapshotResponseType reply = (GetWalrusSnapshotResponseType) request.getReply();
        if(!WalrusProperties.enableSnapshots) {
            LOG.warn("Snapshots not enabled. Please check pre-conditions and restart Walrus.");
            return reply;
        }
        String snapshotId = request.getKey();
        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        if(snapshotInfos.size() > 0) {
            WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
            String bucketName = foundSnapshotInfo.getSnapshotBucket();
            GetObjectType getObjectType = new GetObjectType();
            getObjectType.setBucket(bucketName);
            getObjectType.setUserId(request.getUserId());
            getObjectType.setEffectiveUserId(request.getEffectiveUserId());
            getObjectType.setKey(snapshotId);
            getObjectType.setDeleteAfterGet(false);
            getObjectType.setGetData(true);
            getObjectType.setInlineData(false);
            getObjectType.setGetMetaData(false);
            getObjectType.setIsCompressed(true);
            try {
                walrusManager.getObject(getObjectType);
            } catch(EucalyptusCloudException ex) {
                LOG.error(ex, ex);
                throw ex;
            }
        } else {
            db.rollback();
            throw new NoSuchEntityException(snapshotId);
        }
        return reply;
    }

    public DeleteWalrusSnapshotResponseType deleteWalrusSnapshot(DeleteWalrusSnapshotType request) throws EucalyptusCloudException {
        DeleteWalrusSnapshotResponseType reply = (DeleteWalrusSnapshotResponseType) request.getReply();
        if(!WalrusProperties.enableSnapshots) {
            LOG.warn("Snapshots not enabled. Please check pre-conditions and restart Walrus.");
            return reply;
        }
        String snapshotId = request.getKey();

        //Load the entire snapshot tree and then remove the snapshot
        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);

        ArrayList<String> vgNames = new ArrayList<String>();
        ArrayList<String> lvNames = new ArrayList<String>();
        ArrayList<String> snapIdsToDelete = new ArrayList<String>();

        //Delete is idempotent.
        reply.set_return(true);
        if(snapshotInfos.size() > 0) {
            WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
            //remove the snapshot in the background
            db.delete(foundSnapshotInfo);
            db.commit();
            SnapshotDeleter snapshotDeleter = new SnapshotDeleter(request.getUserId(), foundSnapshotInfo.getSnapshotBucket(), snapshotId);
            snapshotDeleter.start();
        } else {
            db.rollback();
            throw new NoSuchSnapshotException(snapshotId);
        }
        return reply;
    }

    private class SnapshotDeleter extends Thread {
        private String userId;
        private String bucketName;
        private String snapshotId;

        public SnapshotDeleter(String userId, String bucketName, String snapshotId) {
            this.userId = userId;
            this.bucketName = bucketName;
            this.snapshotId = snapshotId;
        }

        public void run() {
            DeleteObjectType deleteObjectType = new DeleteObjectType();
            deleteObjectType.setBucket(bucketName);
            deleteObjectType.setKey(snapshotId);
            deleteObjectType.setUserId(userId);

            try {
                walrusManager.deleteObject(deleteObjectType);
                DeleteBucketType deleteBucketType = new DeleteBucketType();
                deleteBucketType.setBucket(bucketName);
                deleteBucketType.setUserId(userId);
                walrusManager.deleteBucket(deleteBucketType);
            } catch(EucalyptusCloudException ex) {
                LOG.error(ex, ex);
                return;
            }
        }
    }

}