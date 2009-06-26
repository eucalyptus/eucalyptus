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

import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotSet;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import edu.ucsb.eucalyptus.msgs.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class WalrusBlockStorageManager {
    private static Logger LOG = Logger.getLogger( WalrusBlockStorageManager.class );
    private StorageManager storageManager;

    public WalrusBlockStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void initialize() {
        WalrusProperties.enableSnapshots = true;
        startupChecks();
        try {
            storageManager.checkPreconditions();
        } catch(Exception ex) {
            WalrusProperties.enableSnapshots = false;
        }
        //inform SC in case it is running on the same host
        WalrusProperties.sharedMode = true;
    }

    public void startupChecks() {
        cleanFailedSnapshots();
        cleanFailedCachedImages();
    }

    public void cleanFailedSnapshots() {
        EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo();
        snapshotInfo.setTransferred(false);
        List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        for(WalrusSnapshotInfo snapInfo : snapshotInfos) {
            String snapSetId = snapInfo.getSnapshotSetId();
            EntityWrapper<WalrusSnapshotSet> dbSet = db.recast(WalrusSnapshotSet.class);
            List<WalrusSnapshotSet> snapsets = dbSet.query(new WalrusSnapshotSet(snapSetId));
            if(snapsets.size() > 0) {
                WalrusSnapshotSet snapset = snapsets.get(0);
                List<WalrusSnapshotInfo>snapList = snapset.getSnapshotSet();
                if(snapList.contains(snapInfo))
                    snapList.remove(snapInfo);
            }
            db.delete(snapInfo);
            try {
                storageManager.deleteObject(snapInfo.getSnapshotSetId(), snapInfo.getSnapshotId());
            } catch(Exception ex) {
                LOG.error(ex);
            }
        }
        db.commit();
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

    public GetVolumeResponseType getVolume(Bukkit bukkit, GetVolumeType request) throws EucalyptusCloudException {
        GetVolumeResponseType reply = (GetVolumeResponseType) request.getReply();
        if(!WalrusProperties.enableSnapshots) {
            LOG.warn("Snapshots not enabled. Please check pre-conditions and restart Walrus.");
            return reply;
        }
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

                bukkit.AddObject(userId, snapshotSetId, volumeKey);

                GetObjectType getObjectType = new GetObjectType();
                getObjectType.setUserId(request.getUserId());
                getObjectType.setGetData(true);
                getObjectType.setInlineData(false);
                getObjectType.setGetMetaData(false);
                getObjectType.setBucket(snapshotSetId);
                getObjectType.setKey(volumeKey);
                getObjectType.setIsCompressed(true);
                getObjectType.setDeleteAfterGet(true);
                db.commit();
                GetObjectResponseType getObjectResponse = bukkit.GetObject(getObjectType);
                reply.setEtag(getObjectResponse.getEtag());
                reply.setLastModified(getObjectResponse.getLastModified());
                reply.setSize(getObjectResponse.getSize());
                request.setRandomKey(getObjectType.getRandomKey());
                request.setBucket(snapshotSetId);
                request.setKey(volumeKey);
                request.setIsCompressed(true);
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

    public GetSnapshotInfoResponseType getSnapshotInfo(GetSnapshotInfoType request) throws EucalyptusCloudException {
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

    public StoreSnapshotResponseType storeSnapshot(Bukkit bukkit, StoreSnapshotType request) throws EucalyptusCloudException {
        StoreSnapshotResponseType reply = (StoreSnapshotResponseType) request.getReply();

        if(!WalrusProperties.enableSnapshots) {
            LOG.warn("Snapshots not enabled. Please check pre-conditions and restart Walrus.");
            return reply;
        }
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

        List<WalrusSnapshotInfo> snapshotInfos = foundSnapshotSet.getSnapshotSet();
        EntityWrapper<WalrusSnapshotInfo> dbSnap = db.recast(WalrusSnapshotInfo.class);
        WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
        List<WalrusSnapshotInfo> snapInfos = dbSnap.query(snapshotInfo);
        if(snapInfos.size() > 0) {
            snapshotInfo = snapInfos.get(0);
            if(snapshotInfo.getTransferred()) {
                db.rollback();
                throw new EntityAlreadyExistsException(snapshotId);
            }
        } else {
            snapshotInfos.add(snapshotInfo);
            dbSnap.add(snapshotInfo);
        }

        //set snapshot props
        snapshotInfo.setSnapshotSetId(bucketName);
        snapshotInfo.setVgName(snapshotVgName);
        snapshotInfo.setLvName(snapshotLvName);
        snapshotInfo.setTransferred(false);
        //read and store it
        //convert to a PutObject request

        String userId = request.getUserId();
        if(createBucket) {
            CreateBucketType createBucketRequest = new CreateBucketType();
            createBucketRequest.setUserId(userId);
            createBucketRequest.setBucket(bucketName);
            try {
                bukkit.CreateBucket(createBucketRequest);
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
            PutObjectResponseType putObjectResponseType = bukkit.PutObject(putObjectRequest);
            reply.setEtag(putObjectResponseType.getEtag());
            reply.setLastModified(putObjectResponseType.getLastModified());
            reply.setStatusMessage(putObjectResponseType.getStatusMessage());
            int snapshotSize = (int)(putObjectResponseType.getSize() / WalrusProperties.G);
            if(WalrusProperties.shouldEnforceUsageLimits) {
                int totalSnapshotSize = 0;
                WalrusSnapshotInfo snapInfo = new WalrusSnapshotInfo();

                List<WalrusSnapshotInfo> sInfos = dbSnap.query(snapInfo);
                for (WalrusSnapshotInfo sInfo : sInfos) {
                    totalSnapshotSize += sInfo.getSize();
                }
                if((totalSnapshotSize + snapshotSize) > WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE) {
                    db.rollback();
                    throw new EntityTooLargeException(snapshotId);
                }
            }

            //change state
            db.commit();

            snapshotInfo = new WalrusSnapshotInfo(snapshotId);
            dbSnap = new EntityWrapper<WalrusSnapshotInfo>();
            WalrusSnapshotInfo foundSnapshotInfo = dbSnap.getUnique(snapshotInfo);
            foundSnapshotInfo.setSize(snapshotSize);
            foundSnapshotInfo.setTransferred(true);
            dbSnap.commit();
        } catch (EucalyptusCloudException ex) {
            db.rollback();
            throw ex;
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
                    if(snapId.equals(foundSnapshotInfo.getSnapshotId())) {
                        snapshotSetSnapInfo = snapInfo;
                    }
                }
                //delete from the database
                db.delete(foundSnapshotInfo);
                vgNames.add(foundSnapshotInfo.getVgName());
                lvNames.add(foundSnapshotInfo.getLvName());
                snapIdsToDelete.add(foundSnapshotInfo.getSnapshotId());
                if(snapshotSetSnapInfo != null) {
                    snapshotSet.remove(snapshotSetSnapInfo);
                    //only 1 entry left? It is the volume
                    if(snapshotSet.size() == 1) {
                        WalrusSnapshotInfo snapZeroInfo = snapshotSet.get(0);
                        if(snapZeroInfo.getSnapshotId().startsWith("vol")) {
                            snapshotSet.remove(snapZeroInfo);
                            dbSet.delete(foundSnapshotSetInfo);
                            snapZeroInfo = new WalrusSnapshotInfo(snapZeroInfo.getSnapshotId());
                            WalrusSnapshotInfo foundVolInfo = db.getUnique(snapZeroInfo);
                            db.delete(foundVolInfo);
                            vgNames.add(foundVolInfo.getVgName());
                            lvNames.add(foundVolInfo.getLvName());
                            snapIdsToDelete.add(foundVolInfo.getSnapshotId());
                        }
                    }
                }
                //remove the snapshot in the background
                SnapshotDeleter snapshotDeleter = new SnapshotDeleter(bucketName, snapIdsToDelete,
                        vgNames, lvNames, snapshotIds);
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
        private List<String> snapshotIdsToDelete;
        private List<String> vgNames;
        private List<String> lvNames;

        public SnapshotDeleter(String bucketName, List<String> snapshotIdsToDelete, List<String> vgNames, List<String> lvNames, List<String> snapshotSet) {
            this.bucketName = bucketName;
            this.snapshotSet = snapshotSet;
            this.snapshotIdsToDelete = snapshotIdsToDelete;
            this.vgNames = vgNames;
            this.lvNames = lvNames;
        }

        public void run() {
            try {
                for(int i = 0; i < vgNames.size(); ++i) {
                    boolean removeVg = false;
                    //last one?
                    if(i == (vgNames.size() - 1))
                        removeVg = true;
                    String snapId = snapshotIdsToDelete.get(i);
                    storageManager.deleteSnapshot(bucketName, snapId, vgNames.get(i), lvNames.get(i), snapshotSet, removeVg);
                    String snapIdToRemove = null;
                    for(String snapsetId : snapshotSet) {
                        if(snapsetId.equals(snapId)) {
                            snapIdToRemove = snapsetId;
                            break;
                        }
                    }
                    if(snapIdToRemove != null)
                        snapshotSet.remove(snapIdToRemove);
                }
            } catch(EucalyptusCloudException ex) {
                LOG.error(ex, ex);
            }
        }
    }

}