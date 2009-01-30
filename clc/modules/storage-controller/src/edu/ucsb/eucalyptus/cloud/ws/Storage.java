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
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.StorageMetaInfo;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.keys.ServiceKeyStore;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.BlockStorageManager;
import edu.ucsb.eucalyptus.storage.LVM2Manager;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.transport.query.WalrusQueryDispatcher;
import edu.ucsb.eucalyptus.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.net.URL;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;


public class Storage {

    private static Logger LOG = Logger.getLogger(Storage.class);

    static StorageManager volumeStorageManager;
    static StorageManager snapshotStorageManager;
    static BlockStorageManager blockManager;

    private static boolean enableSnapshots = false;
    private static boolean enableStorage = false;

    private static final String ETHERD_PREFIX = "/dev/etherd/e";

    static {
        volumeStorageManager = new FileSystemStorageManager(StorageProperties.storageRootDirectory);
        snapshotStorageManager = new FileSystemStorageManager(StorageProperties.storageRootDirectory);
        initializeForEBS();
    }

    public static void initializeForEBS() {
        enableSnapshots = enableStorage = true;
        EntityWrapper<StorageMetaInfo> db = new EntityWrapper<StorageMetaInfo>();
        StorageMetaInfo metaInfo = new StorageMetaInfo();
        try {
            StorageMetaInfo storageMetaInfo = db.getUnique(metaInfo);
            if(storageMetaInfo.getMaxTotalVolumeSize() == null)
                storageMetaInfo.setMaxTotalVolumeSize(0);
            if(storageMetaInfo.getMaxTotalSnapshotSize() == null)
                storageMetaInfo.setMaxTotalSnapshotSize(0);
        } catch(Exception ex) {
            metaInfo.setMaxTotalVolumeSize(0);
            metaInfo.setMaxTotalSnapshotSize(0);
            db.add(metaInfo);
        }
        db.commit();
        String walrusAddr = StorageProperties.WALRUS_URL;
        if(walrusAddr == null) {
            LOG.warn("Walrus host addr not set");
        }
        //TODO: this should be created by a factory
        blockManager = new LVM2Manager(StorageProperties.storageInterface);
        blockManager.initVolumeManager();
        try {
            blockManager.checkPreconditions();
        } catch(Exception ex) {
            enableStorage = false;
            LOG.warn(ex);
            LOG.warn("Could not initialize block manager");
            return;
        }
        startup();
        checkWalrusConnection();
        //TODO: inform CLC
        //StorageControllerHeartbeatMessage heartbeat = new StorageControllerHeartbeatMessage(StorageProperties.SC_ID);
    }

    public Storage() {}

    private static void startup() {
        cleanVolumes();
        cleanSnapshots();
        blockManager.startupChecks();
    }

    private static void cleanVolumes() {
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setStatus(StorageProperties.Status.creating.toString());
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        for(VolumeInfo volInfo : volumeInfos) {
            String volumeId = volInfo.getVolumeId();
            blockManager.cleanVolume(volumeId);
            try {
                volumeStorageManager.deleteObject("", volumeId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(volInfo);
        }
        db.commit();
        cleanFailedVolumes();
    }

    private static void cleanFailedVolumes() {
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setStatus(StorageProperties.Status.failed.toString());
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        for(VolumeInfo volInfo : volumeInfos) {
            String volumeId = volInfo.getVolumeId();
            blockManager.cleanVolume(volumeId);
            try {
                volumeStorageManager.deleteObject("", volumeId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(volInfo);
        }
        db.commit();
    }

    private static void cleanFailedVolume(String volumeId) {
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        if(volumeInfos.size() > 0) {
            VolumeInfo volInfo = volumeInfos.get(0);
            blockManager.cleanVolume(volumeId);
            try {
                volumeStorageManager.deleteObject("", volumeId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(volInfo);
        }
        db.commit();
    }


    private static void cleanSnapshots() {
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        for(SnapshotInfo snapInfo : snapshotInfos) {
            String snapshotId = snapInfo.getSnapshotId();
            blockManager.cleanSnapshot(snapshotId);
            try {
                snapshotStorageManager.deleteObject("", snapshotId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(snapInfo);
        }
        db.commit();
        cleanFailedSnapshots();
    }

    private static void cleanFailedSnapshots() {
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        for(SnapshotInfo snapInfo : snapshotInfos) {
            String snapshotId = snapInfo.getSnapshotId();
            blockManager.cleanSnapshot(snapshotId);
            try {
                snapshotStorageManager.deleteObject("", snapshotId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(snapInfo);
        }
        db.commit();
    }

    private static void cleanFailedSnapshot(String snapshotId) {
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
        SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        if(snapshotInfos.size() > 0) {
            SnapshotInfo snapInfo = snapshotInfos.get(0);
            blockManager.cleanSnapshot(snapshotId);
            try {
                snapshotStorageManager.deleteObject("", snapshotId);
            } catch(Exception ex) {
                LOG.warn(ex);
            }
            db.delete(snapInfo);
        }
        db.commit();
    }

    private static void checkWalrusConnection() {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = null;
        try {
            java.net.URI addrUri = new URL(StorageProperties.WALRUS_URL).toURI();
            String addrPath = addrUri.getPath();
            String addr = StorageProperties.WALRUS_URL.replaceAll(addrPath, "");
            getMethod = new GetMethod(addr);

            httpClient.executeMethod(getMethod);
            enableSnapshots = true;
        } catch(Exception ex) {
            LOG.warn("Could not connect to Walrus. Snapshot functionality disabled. Please check the Walrus url.");
            enableSnapshots = false;
        } finally {
            if(getMethod != null)
                getMethod.releaseConnection();
        }
    }

    public InitializeStorageManagerResponseType InitializeStorageManager(InitializeStorageManagerType request) {
        InitializeStorageManagerResponseType reply = (InitializeStorageManagerResponseType) request.getReply();
        initializeForEBS();
        return reply;
    }

    public UpdateStorageConfigurationResponseType UpdateStorageConfiguration(UpdateStorageConfigurationType request) {
        UpdateStorageConfigurationResponseType reply = (UpdateStorageConfigurationResponseType) request.getReply();
        String storageRootDirectory = request.getStorageRootDirectory();
        String storageInterface = request.getStorageInterface();

        if(storageRootDirectory != null)  {
            volumeStorageManager.setRootDirectory(storageRootDirectory);
            snapshotStorageManager.setRootDirectory(storageRootDirectory);
        }

        if(storageInterface != null) {
            blockManager.setStorageInterface(storageInterface);
        }

        //test connection to Walrus
        checkWalrusConnection();
        try {
            blockManager.checkPreconditions();
            enableStorage = true;
        } catch (Exception ex) {
            enableStorage = false;
            ex.printStackTrace();
        }
        return reply;
    }

    public GetStorageVolumeResponseType GetStorageVolume(GetStorageVolumeType request) throws EucalyptusCloudException {
        GetStorageVolumeResponseType reply = (GetStorageVolumeResponseType) request.getReply();
        if(!enableStorage) {
            LOG.warn("Storage has been disabled. Please check your setup");
            return reply;
        }

        String volumeId = request.getVolumeId();

        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        List <VolumeInfo> volumeInfos = db.query(volumeInfo);
        if(volumeInfos.size() > 0) {
            VolumeInfo foundVolumeInfo = volumeInfos.get(0);
            List<String> returnValues = blockManager.getVolume(volumeId);
            reply.setVolumeId(foundVolumeInfo.getVolumeId());
            reply.setSize(foundVolumeInfo.getSize().toString());
            reply.setStatus(foundVolumeInfo.getStatus());
            reply.setSnapshotId(foundVolumeInfo.getSnapshotId());
            reply.setActualDeviceName(ETHERD_PREFIX + returnValues.get(0) + "." + returnValues.get(1));
        } else {
            db.rollback();
            throw new NoSuchVolumeException(volumeId);
        }
        db.commit();
        return reply;
    }

    public DeleteStorageVolumeResponseType DeleteStorageVolume(DeleteStorageVolumeType request) throws EucalyptusCloudException {
        DeleteStorageVolumeResponseType reply = (DeleteStorageVolumeResponseType) request.getReply();
        if(!enableStorage) {
            LOG.warn("Storage has been disabled. Please check your setup");
            return reply;
        }

        String volumeId = request.getVolumeId();

        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        List<VolumeInfo> volumeList = db.query(volumeInfo);

        reply.set_return(Boolean.FALSE);
        if(volumeList.size() > 0) {
            VolumeInfo foundVolume = volumeList.get(0);
            //check its status
            String status = foundVolume.getStatus();
            if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
                try {
                    blockManager.deleteVolume(volumeId);
                    volumeStorageManager.deleteObject("", volumeId);
                    db.delete(foundVolume);
                    db.commit();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                db.rollback();
                throw new VolumeInUseException(volumeId);
            }
        } else {
            db.rollback();
            throw new NoSuchVolumeException(volumeId);
        }
        return reply;
    }

    public CreateStorageSnapshotResponseType CreateStorageSnapshot( CreateStorageSnapshotType request ) throws EucalyptusCloudException {
        CreateStorageSnapshotResponseType reply = ( CreateStorageSnapshotResponseType ) request.getReply();

        if(!enableSnapshots || !enableStorage) {
            checkWalrusConnection();
            if(!enableSnapshots)
                LOG.warn("Snapshots have been disabled. Please check connection to Walrus.");
            return reply;
        }
        String volumeId = request.getVolumeId();
        String snapshotId = request.getSnapshotId();
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);

        if(volumeInfos.size() > 0) {
            VolumeInfo foundVolumeInfo = volumeInfos.get(0);
            //check status
            if(foundVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                //create snapshot
                EntityWrapper<SnapshotInfo> db2 = new EntityWrapper<SnapshotInfo>();
                edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo snapshotInfo = new edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo(snapshotId);
                snapshotInfo.setUserName(foundVolumeInfo.getUserName());
                snapshotInfo.setVolumeId(volumeId);
                Date startTime = new Date();
                snapshotInfo.setStartTime(startTime);
                snapshotInfo.setProgress("0");
                snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
                db2.add(snapshotInfo);
                db2.commit();
                db.commit();
                //snapshot asynchronously
                String snapshotSet = "snapset-" + UUID.randomUUID();
                blockManager.createSnapshot(volumeId, snapshotId);
                Snapshotter snapshotter = new Snapshotter(snapshotSet, volumeId, snapshotId);
                snapshotter.run();
                reply.setSnapshotId(snapshotId);
                reply.setVolumeId(volumeId);
                reply.setStatus(snapshotInfo.getStatus());
                reply.setStartTime(DateUtils.format(startTime.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
                reply.setProgress(snapshotInfo.getProgress());
            } else {
                db.rollback();
                throw new VolumeNotReadyException(volumeId);
            }
        } else {
            db.rollback();
            throw new NoSuchVolumeException(volumeId);
        }
        return reply;
    }

    //returns snapshots in progress or at the SC
    public DescribeStorageSnapshotsResponseType DescribeStorageSnapshots( DescribeStorageSnapshotsType request ) throws EucalyptusCloudException {
        DescribeStorageSnapshotsResponseType reply = ( DescribeStorageSnapshotsResponseType ) request.getReply();
        List<String> snapshotSet = request.getSnapshotSet();
        ArrayList<SnapshotInfo> snapshotInfos = new ArrayList<SnapshotInfo>();
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();

        if((snapshotSet != null) && !snapshotSet.isEmpty()) {
            for(String snapshotSetEntry: snapshotSet) {
                SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotSetEntry);
                List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
                if(foundSnapshotInfos.size() > 0) {
                    snapshotInfos.add(foundSnapshotInfos.get(0));
                }
            }
        } else {
            SnapshotInfo snapshotInfo = new SnapshotInfo();
            List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);
            for(SnapshotInfo snapInfo : foundSnapshotInfos) {
                snapshotInfos.add(snapInfo);
            }
        }

        ArrayList<StorageSnapshot> snapshots = reply.getSnapshotSet();
        for(SnapshotInfo snapshotInfo: snapshotInfos) {
            snapshots.add(convertSnapshotInfo(snapshotInfo));
            if(snapshotInfo.getStatus().equals(StorageProperties.Status.failed.toString()))
                cleanFailedSnapshot(snapshotInfo.getSnapshotId());
        }
        return reply;
    }


    public DeleteStorageSnapshotResponseType DeleteStorageSnapshot( DeleteStorageSnapshotType request ) throws EucalyptusCloudException {
        DeleteStorageSnapshotResponseType reply = ( DeleteStorageSnapshotResponseType ) request.getReply();

        if(!enableSnapshots || !enableStorage) {
            checkWalrusConnection();
            if(!enableSnapshots)
                LOG.warn("Snapshots have been disabled. Please check connection to Walrus.");
            return reply;
        }

        String snapshotId = request.getSnapshotId();

        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
        SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);

        reply.set_return(true);
        if(snapshotInfos.size() > 0) {
            SnapshotInfo  foundSnapshotInfo = snapshotInfos.get(0);
            String status = foundSnapshotInfo.getStatus();
            if(status.equals(StorageProperties.Status.available.toString()) || status.equals(StorageProperties.Status.failed.toString())) {
                try {
                    blockManager.deleteSnapshot(snapshotId);
                    snapshotStorageManager.deleteObject("", snapshotId);
                    db.delete(foundSnapshotInfo);
                    db.commit();
                    HttpWriter httpWriter = new HttpWriter("DELETE", "snapset", snapshotId, "DeleteWalrusSnapshot", null);
                    httpWriter.run();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                //snapshot is still in progress.
                reply.set_return(false);
                db.rollback();
                throw new SnapshotInUseException(snapshotId);
            }
        } else {
            //the SC knows nothing about this snapshot. It should be deleted directly from Walrus
            db.rollback();
        }
        return reply;
    }

    public void DeleteWalrusSnapshot(String snapshotId) {
        HttpWriter httpWriter = new HttpWriter("DELETE", "snapset", snapshotId, "DeleteWalrusSnapshot", null);
        try {
            httpWriter.run();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public CreateStorageVolumeResponseType CreateStorageVolume(CreateStorageVolumeType request) throws EucalyptusCloudException {
        CreateStorageVolumeResponseType reply = (CreateStorageVolumeResponseType) request.getReply();

        if(!enableStorage) {
            LOG.warn("Storage has been disabled. Please check your setup");
            return reply;
        }

        String snapshotId = request.getSnapshotId();
        String userId = request.getUserId();
        String volumeId = request.getVolumeId();

        //in GB
        String size = request.getSize();
        int sizeAsInt = 0;
        if(size != null) {
            sizeAsInt = Integer.parseInt(size);
            EntityWrapper<StorageMetaInfo> db = new EntityWrapper<StorageMetaInfo>();
            StorageMetaInfo metaInfo = new StorageMetaInfo();
            StorageMetaInfo foundMetaInfo;
            try {
                foundMetaInfo = db.getUnique(metaInfo);
                if(((foundMetaInfo.getMaxTotalVolumeSize() + sizeAsInt) > StorageProperties.MAX_TOTAL_VOLUME_SIZE) ||
                        (sizeAsInt > StorageProperties.MAX_VOLUME_SIZE))
                    throw new EntityTooLargeException(volumeId);
                if(sizeAsInt > StorageProperties.MAX_VOLUME_SIZE)
                    throw new EntityTooLargeException(volumeId);
            } catch(Exception ex) {
                db.rollback();
                ex.printStackTrace();
            }
            db.commit();
        }

        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        if(volumeInfos.size() > 0) {
            db.rollback();
            throw new VolumeAlreadyExistsException(volumeId);
        }
        volumeInfo.setUserName(userId);
        volumeInfo.setSize(sizeAsInt);
        volumeInfo.setStatus(StorageProperties.Status.creating.toString());
        Date creationDate = new Date();
        volumeInfo.setCreateTime(creationDate);
        volumeInfo.setTransferred(Boolean.FALSE);
        if(snapshotId != null) {
            volumeInfo.setSnapshotId(snapshotId);
            reply.setSnapshotId(snapshotId);
        }
        db.add(volumeInfo);
        reply.setVolumeId(volumeId);
        reply.setCreateTime(DateUtils.format(creationDate.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        reply.setSize(size);
        reply.setStatus(volumeInfo.getStatus());
        db.commit();

        //create volume asynchronously
        VolumeCreator volumeCreator = new VolumeCreator(volumeId, "snapset", snapshotId, sizeAsInt);
        volumeCreator.start();

        return reply;
    }

    public class VolumeCreator extends Thread {
        private String volumeId;
        private String snapshotSetName;
        private String snapshotId;
        private int size;

        public VolumeCreator(String volumeId, String snapshotSetName, String snapshotId, int size) {
            this.volumeId = volumeId;
            this.snapshotSetName = snapshotSetName;
            this.snapshotId = snapshotId;
            this.size = size;
        }

        public void run() {
            boolean success = true;
            if(snapshotId != null) {
                EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
                try {
                    SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
                    List<SnapshotInfo> foundSnapshotInfos = db.query(snapshotInfo);

                    if(foundSnapshotInfos.size() == 0) {
                        String volumePath = getVolume(volumeId, snapshotSetName, snapshotId);
                        size = blockManager.createVolume(volumeId, volumePath);
                        db.commit();
                    } else {
                        SnapshotInfo foundSnapshotInfo = foundSnapshotInfos.get(0);
                        if(!foundSnapshotInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                            success = false;
                            db.rollback();
                            LOG.warn("snapshot " + foundSnapshotInfo.getSnapshotId() + " not available.");
                        } else {
                            size = blockManager.createVolume(volumeId, snapshotId, size);
                            db.commit();
                        }
                    }
                } catch(Exception ex) {
                    success = false;
                    db.rollback();
                    ex.printStackTrace();
                }
            } else {
                try {
                    assert(size > 0);
                    blockManager.createVolume(volumeId, size);
                } catch(Exception ex) {
                    success = false;
                    ex.printStackTrace();
                }
            }
            try {
                EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
                VolumeInfo volumeInfo = new VolumeInfo(volumeId);
                VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
                if(foundVolumeInfo != null) {
                    if(success) {
                        EntityWrapper<StorageMetaInfo> dbMeta = new EntityWrapper<StorageMetaInfo>();
                        StorageMetaInfo metaInfo = new StorageMetaInfo();
                        StorageMetaInfo foundMetaInfo;
                        try {
                            foundMetaInfo = dbMeta.getUnique(metaInfo);
                            if((foundMetaInfo.getMaxTotalVolumeSize() + size) > StorageProperties.MAX_TOTAL_VOLUME_SIZE ||
                                    (size > StorageProperties.MAX_VOLUME_SIZE)) {
                                success = false;
                                LOG.warn("Volume size limit exceeeded");
                            } else {
                                foundMetaInfo.setMaxTotalVolumeSize(foundMetaInfo.getMaxTotalVolumeSize() + size);
                            }
                        } catch(Exception ex) {
                            dbMeta.rollback();
                            foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
                            db.commit();
                            ex.printStackTrace();
                            return;
                        }
                        dbMeta.commit();
                        foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
                    } else {
                        foundVolumeInfo.setStatus(StorageProperties.Status.failed.toString());
                    }
                    if(snapshotId != null) {
                        foundVolumeInfo.setSize(size);
                    }
                } else {
                    db.rollback();
                    throw new EucalyptusCloudException();
                }
                db.commit();
            } catch(EucalyptusCloudException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getVolume(String volumeId, String snapshotBucket, String snapshotId) throws EucalyptusCloudException {
        checkWalrusConnection();
        if(!enableSnapshots) {
            LOG.warn("Could not connect to Walrus. Snapshot functionality disabled. Please check the Walrus url");
            throw new EucalyptusCloudException("could not connect to Walrus.");
        }
        String walrusSnapshotPath = snapshotBucket + "/" + snapshotId;
        String volumePath = StorageProperties.storageRootDirectory + "/" + volumeId;
        File file = new File(volumePath);
        if(!file.exists()) {
            HttpReader volumeReader = new HttpReader(walrusSnapshotPath, null, file, "GetVolume", "", true);
            volumeReader.run();
        } else {
            throw new EucalyptusCloudException("volume file already exists");
        }
        if(file.length() == 0) {
            throw new EucalyptusCloudException("could not get volume");
        }
        return volumePath;
    }

    private List<String> getSnapshots(String snapshotBucket, String snapshotId, List<String> snapshotFileNames) {
        String walrusSnapshotPath = snapshotBucket + "/" + snapshotId;
        HttpReader reader = new HttpReader(walrusSnapshotPath, null, null, "GetSnapshotInfo", "");
        String snapshotDescription = reader.getResponseAsString();
        XMLParser parser = new XMLParser(snapshotDescription);
        //read the list of snapshots and issue requests to walrus to get all deltas
        String bucketName = parser.getValue("/GetSnapshotInfoResponse/Bucket");
        List<String> snapshotSet = parser.getValues("/GetSnapshotInfoResponse/snapshotId");
        for(String snapshot: snapshotSet) {
            walrusSnapshotPath = bucketName + "/" + snapshot;
            String snapshotPath = StorageProperties.storageRootDirectory + "/" + snapshot + Hashes.getRandom(10);
            snapshotFileNames.add(snapshotPath);
            File file = new File(snapshotPath);
            HttpReader snapshotReader = new HttpReader(walrusSnapshotPath, null, file, "GetSnapshot", "");
            snapshotReader.run();
        }
        return snapshotSet;
    }


    public void GetSnapshots(String volumeId, String snapshotSetName, String snapshotId) throws EucalyptusCloudException {
        String volumePath = getVolume(volumeId, snapshotSetName, snapshotId);
        int size = blockManager.createVolume(volumeId, volumePath);
    }


    public DescribeStorageVolumesResponseType DescribeStorageVolumes(DescribeStorageVolumesType request) throws EucalyptusCloudException {
        DescribeStorageVolumesResponseType reply = (DescribeStorageVolumesResponseType) request.getReply();

        List<String> volumeSet = request.getVolumeSet();
        ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();

        if((volumeSet != null) && !volumeSet.isEmpty()) {
            for(String volumeSetEntry: volumeSet) {
                VolumeInfo volumeInfo = new VolumeInfo(volumeSetEntry);
                List<VolumeInfo> foundVolumeInfos = db.query(volumeInfo);
                if(foundVolumeInfos.size() > 0) {
                    volumeInfos.add(foundVolumeInfos.get(0));
                }
            }
        } else {
            VolumeInfo volumeInfo = new VolumeInfo();
            List<VolumeInfo> foundVolumeInfos = db.query(volumeInfo);
            for(VolumeInfo volInfo : foundVolumeInfos) {
                volumeInfos.add(volInfo);
            }
        }

        ArrayList<StorageVolume> volumes = reply.getVolumeSet();
        for(VolumeInfo volumeInfo: volumeInfos) {
            volumes.add(convertVolumeInfo(volumeInfo));
            if(volumeInfo.getStatus().equals(StorageProperties.Status.failed.toString()))
                cleanFailedVolume(volumeInfo.getVolumeId());
        }
        db.commit();
        return reply;
    }


    private StorageVolume convertVolumeInfo(VolumeInfo volInfo) throws EucalyptusCloudException {
        StorageVolume volume = new StorageVolume();
        String volumeId = volInfo.getVolumeId();
        volume.setVolumeId(volumeId);
        volume.setStatus(volInfo.getStatus());
        volume.setCreateTime(DateUtils.format(volInfo.getCreateTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        volume.setSize(String.valueOf(volInfo.getSize()));
        volume.setSnapshotId(volInfo.getSnapshotId());
        List<String> returnValues = blockManager.getVolume(volumeId);
        if(returnValues.size() > 0)
            volume.setActualDeviceName(ETHERD_PREFIX + returnValues.get(0) + "." + returnValues.get(1));
        else
            volume.setActualDeviceName("invalid");
        return volume;
    }

    private StorageSnapshot convertSnapshotInfo(SnapshotInfo snapInfo) {
        StorageSnapshot snapshot = new StorageSnapshot();
        snapshot.setVolumeId(snapInfo.getVolumeId());
        snapshot.setStatus(snapInfo.getStatus());
        snapshot.setSnapshotId(snapInfo.getSnapshotId());
        snapshot.setProgress(snapInfo.getProgress());
        snapshot.setStartTime(DateUtils.format(snapInfo.getStartTime().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
        return snapshot;
    }

    private class Snapshotter extends Thread {
        private String volumeId;
        private String snapshotId;
        private String volumeBucket;
        private String volumeFileName;
        private String snapshotFileName;

        public Snapshotter(String volumeBucket, String volumeId, String snapshotId) {
            this.volumeBucket = volumeBucket;
            this.volumeId = volumeId;
            this.snapshotId = snapshotId;
        }

        public void run() {
            try {
                List<String> returnValues = blockManager.prepareForTransfer(volumeId, snapshotId);
                volumeFileName = returnValues.get(0);
                snapshotFileName = returnValues.get(1);
                EntityWrapper<VolumeInfo>db = new EntityWrapper<VolumeInfo>();
                VolumeInfo volumeInfo = new VolumeInfo(volumeId);
                List <VolumeInfo> volumeInfos = db.query(volumeInfo);

                boolean shouldTransferVolume = false;
                if(volumeInfos.size() > 0) {
                    VolumeInfo foundVolumeInfo = volumeInfos.get(0);
                    if(!foundVolumeInfo.getTransferred()) {
                        //transfer volume to Walrus
                        foundVolumeInfo.setVolumeBucket(volumeBucket);
                        shouldTransferVolume = true;
                    }
                    volumeBucket = foundVolumeInfo.getVolumeBucket();
                    db.commit();
                    EntityWrapper<SnapshotInfo> db2 = new EntityWrapper<SnapshotInfo>();
                    SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
                    SnapshotInfo foundSnapshotInfo = db2.getUnique(snapshotInfo);
                    if(foundSnapshotInfo != null) {
                        transferSnapshot(shouldTransferVolume);
                    }
                    db2.commit();
                } else {
                    db.rollback();
                    throw new EucalyptusCloudException();
                }
            } catch(EucalyptusCloudException ex) {
                ex.printStackTrace();
            }
        }

        private void transferSnapshot(boolean shouldTransferVolume) {
            long size = 0;

            File volumeFile = new File(volumeFileName);
            File snapshotFile = new File(snapshotFileName);

            assert(snapshotFile.exists() && volumeFile.exists());
            size += shouldTransferVolume ? snapshotFile.length() + volumeFile.length() : snapshotFile.length();
            SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, size, StorageProperties.TRANSFER_CHUNK_SIZE);
            Map<String, String> httpParamaters = new HashMap<String, String>();
            HttpWriter httpWriter;
            if(shouldTransferVolume) {
                try {
                    List<String> returnValues = blockManager.getSnapshotValues(volumeId);
                    if(returnValues.size() > 0) {
                        httpParamaters.put("SnapshotVgName", returnValues.get(0));
                        httpParamaters.put("SnapshotLvName", returnValues.get(1));
                    }
                } catch(Exception ex) {
                    LOG.warn(ex, ex);
                }
                httpWriter = new HttpWriter("PUT", volumeFile, callback, volumeBucket, volumeId, "StoreSnapshot", null, httpParamaters);
                try {
                    httpWriter.run();
                    EntityWrapper<VolumeInfo>db = new EntityWrapper<VolumeInfo>();
                    VolumeInfo volumeInfo = new VolumeInfo(volumeId);
                    List <VolumeInfo> volumeInfos = db.query(volumeInfo);
                    if(volumeInfos.size() > 0) {
                        VolumeInfo volInfo = volumeInfos.get(0);
                        volInfo.setTransferred(true);
                    }
                    db.commit();
                } catch(Exception ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            try {
                List<String> returnValues = blockManager.getSnapshotValues(snapshotId);
                if(returnValues.size() > 0) {
                    httpParamaters.put("SnapshotVgName", returnValues.get(0));
                    httpParamaters.put("SnapshotLvName", returnValues.get(1));
                }
            } catch(Exception ex) {
                LOG.warn(ex, ex);
            }
            httpWriter = new HttpWriter("PUT", snapshotFile, callback, volumeBucket, snapshotId, "StoreSnapshot", null, httpParamaters, true);
            try {
                httpWriter.run();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void transferSnapshot(String volumeId, String snapshotId, String dupSnapshotId, boolean shouldTransferVolume) throws EucalyptusCloudException {
        long size = 0;
        String volumeFileName = StorageProperties.storageRootDirectory + "/" + volumeId;
        String snapshotFileName = StorageProperties.storageRootDirectory + "/" + snapshotId;
        File volumeFile = new File(volumeFileName);
        File snapshotFile = new File(snapshotFileName);

        String volumeBucket = "";

        EntityWrapper<VolumeInfo>db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        List <VolumeInfo> volumeInfos = db.query(volumeInfo);

        if(volumeInfos.size() > 0) {
            VolumeInfo foundVolumeInfo = volumeInfos.get(0);
            volumeBucket = foundVolumeInfo.getVolumeBucket();
        } else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
        db.commit();

        assert(snapshotFile.exists() && volumeFile.exists());
        size += shouldTransferVolume ? snapshotFile.length() + volumeFile.length() : snapshotFile.length();
        SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, size, StorageProperties.TRANSFER_CHUNK_SIZE);
        Map<String, String> httpParamaters = new HashMap<String, String>();
        HttpWriter httpWriter;
        if(shouldTransferVolume) {
            try {
                List<String> returnValues = blockManager.getSnapshotValues(volumeId);
                if(returnValues.size() > 0) {
                    httpParamaters.put("SnapshotVgName", returnValues.get(0));
                    httpParamaters.put("SnapshotLvName", returnValues.get(1));
                }
            } catch(Exception ex) {
                LOG.warn(ex, ex);
            }
            httpWriter = new HttpWriter("PUT", volumeFile, callback, volumeBucket, volumeId, "StoreSnapshot", null, httpParamaters);
            try {
                httpWriter.run();
            } catch(Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        try {
            List<String> returnValues = blockManager.getSnapshotValues(snapshotId);
            if(returnValues.size() > 0) {
                httpParamaters.put("SnapshotVgName", returnValues.get(0));
                httpParamaters.put("SnapshotLvName", returnValues.get(1));
            }
        } catch(Exception ex) {
            LOG.warn(ex, ex);
        }
        httpWriter = new HttpWriter("PUT", snapshotFile, callback, volumeBucket, snapshotId, "StoreSnapshot", null, httpParamaters);
        try {
            httpWriter.run();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface CallBack {
        void run();
        int getUpdateThreshold();
        void finish();
        void failed();
    }

    public class SnapshotProgressCallback implements CallBack {
        private String snapshotId;
        private int progressTick;
        private int updateThreshold;

        public SnapshotProgressCallback(String snapshotId, long size, int chunkSize) {
            this.snapshotId = snapshotId;
            progressTick = 5; //minimum percent update
            updateThreshold = (int)(((size * progressTick) / 100) / chunkSize);
        }

        public void run() {
            EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
            try {
                SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
                if(foundSnapshotInfo.getProgress() == null)
                    foundSnapshotInfo.setProgress("0");
                Integer progress = Integer.parseInt(foundSnapshotInfo.getProgress());
                progress += progressTick;
                foundSnapshotInfo.setProgress(String.valueOf(progress));
            } catch (Exception ex) {
                db.rollback();
                failed();
                ex.printStackTrace();
            }
            db.commit();
        }

        public void finish() {
            EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
            try {
                SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
                foundSnapshotInfo.setProgress(String.valueOf(100));
                foundSnapshotInfo.setTransferred(true);
                foundSnapshotInfo.setStatus(StorageProperties.Status.available.toString());
            } catch (Exception ex) {
                db.rollback();
                ex.printStackTrace();
            }
            db.commit();
        }

        public void failed() {
            EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
            try {
                SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
                foundSnapshotInfo.setProgress(String.valueOf(0));
                foundSnapshotInfo.setTransferred(false);
                foundSnapshotInfo.setStatus(StorageProperties.Status.failed.toString());
            } catch (Exception ex) {
                db.rollback();
                ex.printStackTrace();
            }
            db.commit();

        }

        public int getUpdateThreshold() {
            return updateThreshold;
        }
    }

    //All HttpTransfer operations should be called asynchronously. The operations themselves are synchronous.
    class HttpTransfer {
        public HttpMethodBase constructHttpMethod(String verb, String addr, String eucaOperation, String eucaHeader) {
            AbstractKeyStore keyStore = null;
            try {
                keyStore = ServiceKeyStore.getInstance();
            } catch(Exception ex) {
                LOG.warn(ex, ex);
            }
            String date = new Date().toString();
            String httpVerb = verb;
            String addrPath;
            try {
                java.net.URI addrUri = new URL(addr).toURI();
                addrPath = addrUri.getPath().toString();
                String query = addrUri.getQuery();
                if(query != null) {
                    addrPath += "?" + query;
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }
            String data = httpVerb + "\n" + date + "\n" + addrPath + "\n";

            HttpMethodBase method = null;
            if(httpVerb.equals("PUT")) {
                method = new  PutMethodWithProgress(addr);
            } else if(httpVerb.equals("GET")) {
                method = new GetMethod(addr);
            } else if(httpVerb.equals("DELETE")) {
                method = new DeleteMethod(addr);
            }
            method.setRequestHeader("Authorization", "Euca");
            method.setRequestHeader("Date", date);
            method.setRequestHeader("Expect", "100-continue");
            method.setRequestHeader(StorageProperties.EUCALYPTUS_OPERATION, eucaOperation);
            if(eucaHeader != null) {
                method.setRequestHeader(StorageProperties.EUCALYPTUS_HEADER, eucaHeader);
            }
            try {
                //TODO: Get credentials for SC from keystore

                PrivateKey ccPrivateKey = (PrivateKey) keyStore.getKey(EucalyptusProperties.NAME, EucalyptusProperties.NAME);
                X509Certificate cert = keyStore.getCertificate(EucalyptusProperties.NAME);
                if(cert == null)
                    return null;
                byte[] pemCertBytes = Hashes.getPemBytes(cert);

                Signature sign = Signature.getInstance("SHA1withRSA");
                sign.initSign(ccPrivateKey);
                sign.update(data.getBytes());
                byte[] sig = sign.sign();

                method.setRequestHeader("EucaCert", new String(Base64.encode(pemCertBytes))); // or maybe cert instead of ccPublicKey?
                method.setRequestHeader("EucaSignature", new String(Base64.encode(sig)));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return method;
        }

        public HttpTransfer() {}
    }

    public class PutMethodWithProgress extends PutMethod {
        private File outFile;
        private CallBack callback;
        private boolean deleteOnXfer;

        public PutMethodWithProgress(String path) {
            super(path);
        }

        public void setOutFile(File outFile) {
            this.outFile = outFile;
        }

        public void setCallBack(CallBack callback) {
            this.callback = callback;
        }

        public void setDeleteOnXfer(boolean deleteOnXfer) {
            this.deleteOnXfer = deleteOnXfer;
        }

        @Override
        protected boolean writeRequestBody(HttpState state, HttpConnection conn) throws IOException {
            if(null != getRequestHeader("expect") && getStatusCode() != HttpStatus.SC_CONTINUE) {
                return false;
            }

            InputStream inputStream;
            if (outFile != null) {
                inputStream = new FileInputStream(outFile);

                GZIPOutputStream gzipOutStream = new GZIPOutputStream(conn.getRequestOutputStream());
                byte[] buffer = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
                int bytesRead;
                int numberProcessed = 0;
                long totalBytesProcessed = 0;
                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    gzipOutStream.write(buffer, 0, bytesRead);
                    totalBytesProcessed += bytesRead;
                    if(++numberProcessed >= callback.getUpdateThreshold()) {
                        callback.run();
                        numberProcessed = 0;
                    }
                }
                if(totalBytesProcessed == outFile.length()) {
                    callback.finish();
                } else {
                    callback.failed();
                }
                gzipOutStream.flush();
                gzipOutStream.finish();
                inputStream.close();
                if(deleteOnXfer) {
                    snapshotStorageManager.deleteAbsoluteObject(outFile.getAbsolutePath());
                }
            } else{
                return false;
            }
            return true;
        }
    }

    class HttpWriter extends HttpTransfer {

        private HttpClient httpClient;
        private HttpMethodBase method;
        public HttpWriter(String httpVerb, String bucket, String key, String eucaOperation, String eucaHeader) {
            httpClient = new HttpClient();
            String walrusAddr = StorageProperties.WALRUS_URL;
            if(walrusAddr != null) {
                String addr = walrusAddr + "/" + bucket + "/" + key;
                method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
            }
        }

        public HttpWriter(String httpVerb, File file, CallBack callback, String bucket, String key, String eucaOperation, String eucaHeader, Map<String, String> httpParameters) {
            httpClient = new HttpClient();
            String walrusAddr = StorageProperties.WALRUS_URL;
            if(walrusAddr != null) {
                String addr = walrusAddr + "/" + bucket + "/" + key;
                Set<String> paramKeySet = httpParameters.keySet();
                boolean first = true;
                for(String paramKey : paramKeySet) {
                    if(!first) {
                        addr += "&";
                    } else {
                        addr += "?";
                    }
                    first = false;
                    addr += paramKey;
                    String value = httpParameters.get(paramKey);
                    if(value != null)
                        addr += "=" + value;
                }
                method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
                method.setRequestHeader("Content-Length", String.valueOf(file.length()));
                ((PutMethodWithProgress)method).setOutFile(file);
                ((PutMethodWithProgress)method).setCallBack(callback);
            }
        }

        public HttpWriter(String httpVerb, File file, CallBack callback, String bucket, String key, String eucaOperation, String eucaHeader, Map<String, String> httpParameters, boolean deleteOnXfer) {
            this(httpVerb, file, callback, bucket, key, eucaOperation, eucaHeader, httpParameters);
            ((PutMethodWithProgress)method).setDeleteOnXfer(deleteOnXfer);
        }

        public void run() throws EucalyptusCloudException {
            try {
                httpClient.executeMethod(method);
                method.releaseConnection();
            } catch (Exception ex) {
                throw new EucalyptusCloudException("error transferring");
            }
        }
    }

    class HttpReader extends HttpTransfer {

        private LinkedBlockingQueue<WalrusDataMessage> getQueue;
        private HttpClient httpClient;
        private HttpMethodBase method;
        private File file;
        private boolean compressed;

        public HttpReader(String path, LinkedBlockingQueue<WalrusDataMessage> getQueue, File file, String eucaOperation, String eucaHeader) {
            this.getQueue = getQueue;
            this.file = file;
            httpClient = new HttpClient();

            String httpVerb = "GET";
            String addr = StorageProperties.WALRUS_URL + "/" + path;

            method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
        }

        public HttpReader(String path, LinkedBlockingQueue<WalrusDataMessage> getQueue, File file, String eucaOperation, String eucaHeader, boolean compressed) {
            this(path, getQueue, file, eucaOperation, eucaHeader);
            this.compressed = compressed;
        }

        public String getResponseAsString() {
            try {
                httpClient.executeMethod(method);
                InputStream inputStream;
                if(compressed) {
                    inputStream = new GZIPInputStream(method.getResponseBodyAsStream());
                } else {
                    inputStream = method.getResponseBodyAsStream();
                }

                String responseString = "";
                byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
                int bytesRead;
                while((bytesRead = inputStream.read(bytes)) > 0) {
                    responseString += new String(bytes, 0 , bytesRead);
                }
                return responseString;
            } catch(Exception ex) {
                ex.printStackTrace();
            } finally {
                method.releaseConnection();
            }
            return null;
        }

        private void getResponseToFile() {
            byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
            try {
                assert(method != null);
                httpClient.executeMethod(method);
                InputStream httpIn = method.getResponseBodyAsStream();
                int bytesRead;
                BufferedOutputStream bufferedOut = new BufferedOutputStream(new FileOutputStream(file));
                while((bytesRead = httpIn.read(bytes)) > 0) {
                    bufferedOut.write(bytes, 0, bytesRead);
                }
                bufferedOut.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                method.releaseConnection();
            }
        }

        private void getResponseToQueue() {
            byte[] bytes = new byte[WalrusQueryDispatcher.DATA_MESSAGE_SIZE];
            try {
                httpClient.executeMethod(method);
                InputStream httpIn = method.getResponseBodyAsStream();
                int bytesRead;
                getQueue.add(WalrusDataMessage.StartOfData(0));
                while((bytesRead = httpIn.read(bytes)) > 0) {
                    getQueue.add(WalrusDataMessage.DataMessage(bytes, bytesRead));
                }
                getQueue.add(WalrusDataMessage.EOF());
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                method.releaseConnection();
            }
        }

        public void run() {
            if(getQueue != null) {
                getResponseToQueue();
            } else if(file != null) {
                getResponseToFile();
            }
        }
    }

}
