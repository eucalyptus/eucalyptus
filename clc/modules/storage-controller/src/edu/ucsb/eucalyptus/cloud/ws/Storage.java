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


import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.ElasticBlockManager;
import edu.ucsb.eucalyptus.storage.LVM2Manager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.util.*;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.keys.ServiceKeyStore;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.AttachedVolumeInfo;
import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.transport.query.WalrusQueryDispatcher;
import org.apache.log4j.Logger;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.xml.security.utils.Base64;

import java.util.List;
import java.util.Date;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;
import java.security.Signature;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.nio.channels.FileChannel;


public class Storage {

    private static Logger LOG = Logger.getLogger(Storage.class);

    static StorageManager imageStorageManager;
    static StorageManager volumeStorageManager;
    static StorageManager snapshotStorageManager;
    static ElasticBlockManager ebsManager;

    static {
        imageStorageManager = new FileSystemStorageManager(WalrusProperties.bucketRootDirectory);
        volumeStorageManager = new FileSystemStorageManager(StorageProperties.volumeRootDirectory);
        snapshotStorageManager = new FileSystemStorageManager(StorageProperties.snapshotRootDirectory);
        ebsManager = new LVM2Manager();
        ebsManager.initVolumeManager(StorageProperties.volumeRootDirectory, StorageProperties.snapshotRootDirectory);
    }

    //For unit testing
    public Storage() {}


    public GetVolumeResponseType GetVolume(GetVolumeType request) throws EucalyptusCloudException {
        GetVolumeResponseType reply = (GetVolumeResponseType) request.getReply();
        String volumeId = request.getVolumeId();

        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
        if(foundVolumeInfo != null) {
            List<String> returnValues = ebsManager.getVolume(volumeId);
            reply.setMajorNumber(returnValues.get(0));
            reply.setMinorNumber(returnValues.get(1));
        } else {
            db.rollback();
            throw new EucalyptusCloudException();
        }
        db.commit();
        return reply;
    }

    public DeleteVolumeResponseType DeleteVolume(DeleteVolumeType request) throws EucalyptusCloudException {
        DeleteVolumeResponseType reply = (DeleteVolumeResponseType) request.getReply();
        String volumeId = request.getVolumeId();

        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setVolumeId(volumeId);
        List<VolumeInfo> volumeList = db.query(volumeInfo);

        reply.set_return(Boolean.FALSE);
        if(volumeList.size() > 0) {
            VolumeInfo foundVolume = volumeList.get(0);
            //check its attachment set & status
            if(foundVolume.getAttachmentSet().isEmpty() && foundVolume.getStatus().equals(StorageProperties.Status.available.toString())) {
                try {
                    ebsManager.deleteVolume(volumeId);
                    volumeStorageManager.deleteObject("", volumeId);
                    reply.set_return(Boolean.TRUE);
                    db.delete(foundVolume);
                    db.commit();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                db.rollback();
                throw new EucalyptusCloudException();
            }
        }
        return reply;
    }



    public CreateSnapshotResponseType CreateSnapshot( CreateSnapshotType request ) throws EucalyptusCloudException {
        CreateSnapshotResponseType reply = ( CreateSnapshotResponseType ) request.getReply();
        String volumeId = request.getVolumeId();

        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);

        String snapshotId = "snap-" + Hashes.getRandom(8);
        if(foundVolumeInfo != null) {
            //check status
            if(foundVolumeInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                //create snapshot
                EntityWrapper<SnapshotInfo> db2 = new EntityWrapper<SnapshotInfo>();
                edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo snapshotInfo = new edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo(snapshotId);
                snapshotInfo.setUserName(foundVolumeInfo.getUserName());
                snapshotInfo.setVolumeId(volumeId);
                snapshotInfo.setStartTime(new Date());
                snapshotInfo.setProgress("0%");
                snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
                db2.add(snapshotInfo);
                db2.commit();
                db.commit();
                //snapshot asynchronously
                ebsManager.createSnapshot(volumeId, snapshotId);
                Snapshotter snapshotter = new Snapshotter(volumeId, snapshotId);
                snapshotter.run();
                reply.setSnapshot(convertSnapshotInfo(snapshotInfo));
            } else {
                db.rollback();
                throw new EucalyptusCloudException();
            }
        } else {
            throw new EucalyptusCloudException();
        }
        return reply;
    }

    public DescribeSnapshotsResponseType DescribeSnapshots( DescribeSnapshotsType request ) throws EucalyptusCloudException {
        DescribeSnapshotsResponseType reply = ( DescribeSnapshotsResponseType ) request.getReply();
        List<String> snapshotSet = request.getSnapshotSet();
        ArrayList<SnapshotInfo> snapshotInfos = new ArrayList<SnapshotInfo>();
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();

        for(String snapshotSetEntry: snapshotSet) {
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotSetEntry);
            SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
            if(foundSnapshotInfo != null) {
                snapshotInfos.add(foundSnapshotInfo);
            }
        }
        List<String> status = null;

        ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
        for(SnapshotInfo snapshotInfo: snapshotInfos) {
            snapshots.add(convertSnapshotInfo(snapshotInfo));
        }
        reply.setSnapshotSet(snapshots);
        return reply;
    }


    public DeleteSnapshotResponseType DeleteSnapshot( DeleteSnapshotType request ) throws EucalyptusCloudException {
        DeleteSnapshotResponseType reply = ( DeleteSnapshotResponseType ) request.getReply();

        String snapshotId = request.getSnapshotId();

        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setSnapshotId(snapshotId);
        SnapshotInfo foundSnapshotInfo = null;
        try {
            foundSnapshotInfo = db.getUnique(snapshotInfo);
        } catch (Exception ex) {
            db.rollback();
            ex.printStackTrace();
        }
        reply.set_return(Boolean.FALSE);
        if(foundSnapshotInfo != null) {
            if(foundSnapshotInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                try {
                    ebsManager.deleteSnapshot(snapshotId);
                    snapshotStorageManager.deleteObject("", snapshotId);
                    db.delete(foundSnapshotInfo);
                    reply.set_return(Boolean.TRUE);
                    db.commit();
                    //TODO: Asynchronously tell Walrus to get rid of this snapshot.
                    //This is not required for correctness, but will save space.
                    //Also, if there are multiple obsolete snapshots, they will have to be copied over to have a
                    //consistent view of the volume and its snapshots (for new volume creation).
                    //But this requires the current volume to be transferred to Walrus again (the state after lvremove).
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                //snapshot is still in progress.
                db.rollback();
                throw new EucalyptusCloudException();
            }
        }
        return reply;
    }

    public CreateVolumeResponseType CreateVolume(CreateVolumeType request) throws EucalyptusCloudException {
        CreateVolumeResponseType reply = (CreateVolumeResponseType) request.getReply();

        String snapshotId = request.getSnapshotId();
        String userId = request.getUserId();
        //in GB
        String size = request.getSize();
        int sizeAsInt = 0;
        if(size != null) {
            sizeAsInt = Integer.parseInt(size);
        }
        String volumeId = "vol-" + Hashes.getRandom(8);


        edu.ucsb.eucalyptus.cloud.entities.VolumeInfo volumeInfo = new edu.ucsb.eucalyptus.cloud.entities.VolumeInfo(volumeId);
        volumeInfo.setUserName(userId);
        volumeInfo.setSize(sizeAsInt);
        volumeInfo.setStatus(StorageProperties.Status.creating.toString());
        volumeInfo.setAttachmentSet(new ArrayList<edu.ucsb.eucalyptus.cloud.entities.AttachedVolumeInfo>());
        volumeInfo.setCreateTime(new Date());
        volumeInfo.setTransferred(Boolean.FALSE);
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
        db.add(volumeInfo);
        db.commit();
        Volume volume = convertVolumeInfo(volumeInfo);

        //create volume asynchronously
        VolumeCreator volumeCreator = new VolumeCreator(volumeId, snapshotId, sizeAsInt);
        volumeCreator.start();

        reply.setVolume(volume);
        return reply;
    }

    public class VolumeCreator extends Thread {
        private String volumeId;
        private String snapshotId;
        private int size;

        public VolumeCreator(String volumeId, String snapshotId, int size) {
            this.volumeId = volumeId;
            this.snapshotId = snapshotId;
            this.size = size;
        }

        public void run() {
            if(snapshotId != null) {
                EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
                try {
                    SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
                    SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
                    if(foundSnapshotInfo == null) {
                        //retrieve the snapshot (tree) from Walrus.
                        //"load" it locally and add to SnapshotInfos
                        ArrayList<String> snapshotFileNames = new ArrayList<String>();
                        List<String> snapshotSet = getSnapshots(snapshotId, snapshotFileNames);
                        ebsManager.loadSnapshots(snapshotSet, snapshotFileNames);
                    } else {
                        if(!foundSnapshotInfo.getStatus().equals(StorageProperties.Status.available.toString())) {
                            db.rollback();
                            throw new EucalyptusCloudException();
                        }
                    }

                    //create new volume from snapshot volume
                    size = ebsManager.createVolume(volumeId, snapshotId, size);
                    db.commit();
                } catch(Exception ex) {
                    db.rollback();
                    ex.printStackTrace();
                }
            } else {
                try {
                    assert(size > 0);
                    ebsManager.createVolume(volumeId, size);
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            try {
                EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
                VolumeInfo volumeInfo = new VolumeInfo(volumeId);
                VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
                if(foundVolumeInfo != null) {
                    foundVolumeInfo.setStatus(StorageProperties.Status.available.toString());
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

    private List<String> getSnapshots(String snapshotId, List<String> snapshotFileNames) {
        String walrusSnapshotPath = WalrusProperties.URL_PROPERTY + "/" + StorageProperties.snapshotBucket + "/" + snapshotId;
        HttpReader reader = new HttpReader(walrusSnapshotPath, null, null, "GetSnapshotInfo", "");
        String snapshotDescription = reader.getResponseAsString();
        XMLParser parser = new XMLParser(snapshotDescription);
        //read the list of snapshots and issue requests to walrus to get all deltas
        List<String> snapshotSet = parser.getValues("/GetSnapshotInfoResponse/SnapshotId");
        for(String snapshot: snapshotSet) {
            walrusSnapshotPath = WalrusProperties.URL_PROPERTY + "/" + StorageProperties.snapshotBucket + "/" + snapshot;
            String snapshotPath = StorageProperties.snapshotRootDirectory + "/" + snapshot;
            snapshotFileNames.add(snapshotPath);
            File file = new File(snapshotPath);
            HttpReader snapshotReader = new HttpReader(walrusSnapshotPath, null, file, "GetSnapshot", "");
            snapshotReader.start();
        }
        return snapshotSet;
    }


    public DescribeVolumesResponseType DescribeVolumes(DescribeVolumesType request) throws EucalyptusCloudException {
        DescribeVolumesResponseType reply = (DescribeVolumesResponseType) request.getReply();
        String userId = request.getUserId();
        List<String> volumeSet = request.getVolumeSet();
        ArrayList<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();

        for(String volumeSetEntry: volumeSet) {
            VolumeInfo volumeInfo = new VolumeInfo(volumeSetEntry);
            VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
            if(foundVolumeInfo != null) {
                volumeInfos.add(foundVolumeInfo);
            }
        }

        List<String> status = null;
        ArrayList<Volume> volumes = new ArrayList<Volume>();
        for(VolumeInfo volumeInfo: volumeInfos) {
            volumes.add(convertVolumeInfo(volumeInfo));
        }
        reply.setVolumeSet(volumes);
        return reply;
    }


    private AttachedVolume convertAttachedVolumeInfo(AttachedVolumeInfo attachedVolInfo) {
        AttachedVolume attachedVolume = new AttachedVolume();
        attachedVolume.setVolumeId(attachedVolInfo.getVolumeId());
        attachedVolume.setInstanceId(attachedVolInfo.getInstanceId());
        attachedVolume.setDevice(attachedVolInfo.getDevice());
        attachedVolume.setStatus(attachedVolInfo.getStatus());
        attachedVolume.setAttachTime(attachedVolInfo.getAttachTime());
        return attachedVolume;
    }

    private Volume convertVolumeInfo(VolumeInfo volInfo) {
        Volume volume = new Volume();
        volume.setVolumeId(volInfo.getVolumeId());
        volume.setStatus(volInfo.getStatus());
        volume.setCreateTime(volInfo.getCreateTime());
        volume.setAvailabilityZone(volInfo.getZone());
        volume.setSize(String.valueOf(volInfo.getSize()));
        volume.setSnapshotId("");
        ArrayList<AttachedVolume> attachmentSet = new ArrayList<AttachedVolume>();
        for (AttachedVolumeInfo attachedVolInfo: volInfo.getAttachmentSet()) {
            attachmentSet.add(convertAttachedVolumeInfo(attachedVolInfo));
        }
        return volume;
    }

    private Snapshot convertSnapshotInfo(SnapshotInfo snapInfo) {
        Snapshot snapshot = new Snapshot();
        snapshot.setVolumeId(snapInfo.getVolumeId());
        snapshot.setStatus(snapInfo.getStatus());
        snapshot.setSnapshotId(snapInfo.getSnapshotId());
        snapshot.setProgress(snapInfo.getProgress());
        snapshot.setStartTime(snapInfo.getStartTime());
        return snapshot;
    }

    private class Snapshotter extends Thread {
        private String volumeId;
        private String snapshotId;
        private String volumeBucket;
        private String volumeKey;
        private String volumeFileName;
        private String snapshotFileName;

        public Snapshotter(String volumeId, String snapshotId) {
            this.volumeId = volumeId;
            this.snapshotId = snapshotId;
        }

        public void run() {
            try {
                List<String> returnValues = ebsManager.prepareForTransfer(volumeId, snapshotId);
                volumeFileName = returnValues.get(0);
                snapshotFileName = returnValues.get(1);
                EntityWrapper<VolumeInfo>db = new EntityWrapper<VolumeInfo>();
                VolumeInfo volumeInfo = new VolumeInfo(volumeId);
                VolumeInfo foundVolumeInfo = db.getUnique(volumeInfo);
                boolean shouldTransferVolume = false;
                if(foundVolumeInfo != null) {
                    if(!foundVolumeInfo.getTransferred()) {
                        //transfer volume to Walrus
                        volumeBucket = StorageProperties.snapshotBucket;
                        volumeKey = volumeId + Hashes.getRandom(4);
                        foundVolumeInfo.setVolumeBucket(volumeBucket);
                        foundVolumeInfo.setVolumeKey(volumeKey);
                        foundVolumeInfo.setTransferred(Boolean.TRUE);
                        shouldTransferVolume = true;
                    } else {
                        volumeKey = foundVolumeInfo.getVolumeKey();
                        volumeBucket = foundVolumeInfo.getVolumeBucket();
                    }
                    db.commit();
                    EntityWrapper<SnapshotInfo> db2 = new EntityWrapper<SnapshotInfo>();
                    SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
                    SnapshotInfo foundSnapshotInfo = db2.getUnique(snapshotInfo);
                    if(foundSnapshotInfo != null) {
                        assert(foundSnapshotInfo.getTransferred());
                        foundSnapshotInfo.setTransferred(Boolean.TRUE);
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
            HttpWriter httpWriter;
            if(shouldTransferVolume) {
                httpWriter = new HttpWriter(volumeFile, StorageProperties.TRANSFER_CHUNK_SIZE, callback, volumeBucket, volumeKey, "StoreSnapshot", null);
                httpWriter.run();
            }
            httpWriter = new HttpWriter(snapshotFile, StorageProperties.TRANSFER_CHUNK_SIZE, callback, volumeBucket, snapshotId, "StoreSnapshot", volumeKey);
            httpWriter.run();
        }
    }

    public interface CallBack {
        void run();
    }

    public class SnapshotProgressCallback implements CallBack {
        private String snapshotId;
        private int progressTick;

        public SnapshotProgressCallback(String snapshotId, long size, int chunkSize) {
            this.snapshotId = snapshotId;
            progressTick = (int) (chunkSize / size) * 100;
        }

        public void run() {
            EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
            SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
            try {
                SnapshotInfo foundSnapshotInfo = db.getUnique(snapshotInfo);
                Integer progress = Integer.parseInt(foundSnapshotInfo.getProgress());
                progress += progressTick;
                foundSnapshotInfo.setProgress(String.valueOf(progress));
            } catch (Exception ex) {
                db.rollback();
                ex.printStackTrace();
            }
            db.commit();
        }
    }

    class HttpTransfer extends Thread {
        public HttpMethodBase constructHttpMethod(String verb, String addr, String eucaOperation, String eucaHeader) {
            AbstractKeyStore keyStore = null;
            try {
                keyStore = ServiceKeyStore.getInstance();
            } catch(Exception ex) {
                LOG.warn(ex, ex);
            }
            String date = new Date().toString();
            String httpVerb = verb;
            String data = httpVerb + "\n" + date + "\n" + addr + "\n";

            HttpMethodBase method = null;
            if(httpVerb.equals("PUT")) {
                method = new PutMethodWithProgress(addr);
            } else if(httpVerb.equals("GET")) {
                method = new GetMethod(addr);
            }
            method.setRequestHeader("Authorization", "Euca");
            method.setRequestHeader("Date", date);
            method.setRequestHeader(StorageProperties.EUCALYPTUS_OPERATION, eucaOperation);
            if(eucaHeader != null) {
                method.setRequestHeader(StorageProperties.EUCALYPTUS_HEADER, eucaHeader);
            }
            try {
                //TODO: Get credentials for CC from keystore

                PrivateKey ccPrivateKey = (PrivateKey) keyStore.getKey(EucalyptusProperties.NAME, EucalyptusProperties.NAME);
                X509Certificate cert = keyStore.getCertificate(EucalyptusProperties.NAME);
                PublicKey ccPublicKey = cert.getPublicKey();

                Signature sign = Signature.getInstance("SHA1withRSA");
                sign.initSign(ccPrivateKey);
                sign.update(data.getBytes());
                byte[] sig = sign.sign();

                method.setRequestHeader("EucaCert", new String(Base64.encode(ccPublicKey.toString().getBytes()))); // or maybe cert instead of ccPublicKey?
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

        public PutMethodWithProgress(String path) {
            super(path);
        }

        public void setOutFile(File outFile) {
            this.outFile = outFile;
        }

        public void setCallBack(CallBack callback) {
            this.callback = callback;
        }

        protected boolean writeRequestBody(HttpState state, HttpConnection conn) throws IOException {
            if(null != getRequestHeader("expect") && getStatusCode() != HttpStatus.SC_CONTINUE) {
                return false;
            }
            OutputStream out = conn.getRequestOutputStream();

            InputStream inputStream = null;
            if (outFile != null) {
                inputStream = new FileInputStream(outFile);
            }

            byte[] buffer = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
            int nb = 0;
            while (true) {
                nb = inputStream.read(buffer);
                if (nb == -1) {
                    break;
                }
                out.write(buffer, 0, nb);
                callback.run();
            }
            inputStream.close();
            out.close();
            return true;
        }


    }

    class HttpWriter extends HttpTransfer {

        private HttpClient httpClient;
        private HttpMethodBase method;
        private File file;
        private int transferChunkSize;
        private CallBack callback;

        public HttpWriter(File file, int transferChunkSize, CallBack callback, String bucket, String key, String eucaOperation, String eucaHeader) {
            httpClient = new HttpClient();
            this.file = file;
            this.transferChunkSize = transferChunkSize;
            this.callback = callback;
            String httpVerb = "PUT";
            String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/" + bucket + "/" + key;
            method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
        }

        public void run() {

            byte[] bytes = new byte[transferChunkSize];
            try {
                ((PutMethodWithProgress)method).setOutFile(file);
                ((PutMethodWithProgress)method).setCallBack(callback);
                httpClient.executeMethod(method);

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                method.releaseConnection();
            }
        }
    }

    class HttpReader extends HttpTransfer {

        private LinkedBlockingQueue<WalrusDataMessage> getQueue;
        private HttpClient httpClient;
        private HttpMethodBase method;
        private File file;

        public HttpReader(String path, LinkedBlockingQueue<WalrusDataMessage> getQueue, File file, String eucaOperation, String eucaHeader) {
            this.getQueue = getQueue;
            this.file = file;
            httpClient = new HttpClient();

            String httpVerb = "GET";
            String addr = System.getProperty(WalrusProperties.URL_PROPERTY) + "/" + path;

            method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader);
        }

        public String getResponseAsString() {
            try {
                httpClient.executeMethod(method);
                return method.getResponseBodyAsString();
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
                httpClient.executeMethod(method);
                InputStream httpIn = method.getResponseBodyAsStream();
                int bytesRead;
                FileOutputStream outStream = new FileOutputStream(file);
                while((bytesRead = httpIn.read(bytes)) > 0) {
                    outStream.write(bytes, 0, bytesRead);
                }
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