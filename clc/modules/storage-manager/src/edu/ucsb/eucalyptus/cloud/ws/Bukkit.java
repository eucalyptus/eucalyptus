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

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.NotImplementedException;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;

public class Bukkit {

    private static Logger LOG = Logger.getLogger( Bukkit.class );

    private static WalrusDataMessenger imageMessenger = new WalrusDataMessenger();
    private static StorageManager storageManager;
    private static WalrusManager walrusManager;
    private static WalrusBlockStorageManager walrusBlockStorageManager;
    private static WalrusImageManager walrusImageManager;

    static {
        storageManager = new FileSystemStorageManager(WalrusProperties.bucketRootDirectory);
        walrusImageManager = new WalrusImageManager(storageManager, imageMessenger);
        walrusManager = new WalrusManager(storageManager, walrusImageManager);
        walrusBlockStorageManager = new WalrusBlockStorageManager(storageManager, walrusManager);
        String limits = System.getProperty(WalrusProperties.USAGE_LIMITS_PROPERTY);
        if(limits != null) {
            WalrusProperties.shouldEnforceUsageLimits = Boolean.parseBoolean(limits);
        }
        walrusManager.initialize();
        Tracker.initialize();
        walrusBlockStorageManager.initialize();
        if(System.getProperty("euca.virtualhosting.disable") != null) {
            WalrusProperties.enableVirtualHosting = false;
        }
    }

    public Bukkit () {}

    public InitializeWalrusResponseType InitializeWalrus(InitializeWalrusType request) {
        InitializeWalrusResponseType reply = (InitializeWalrusResponseType) request.getReply();
        walrusBlockStorageManager.initialize();
        return reply;
    }

    public UpdateWalrusConfigurationResponseType UpdateWalrusConfiguration(UpdateWalrusConfigurationType request) {
        UpdateWalrusConfigurationResponseType reply = (UpdateWalrusConfigurationResponseType) request.getReply();
        String rootDir = request.getBucketRootDirectory();
        if(rootDir != null)
            storageManager.setRootDirectory(rootDir);
        walrusManager.check();
        return reply;
    }

    public CreateBucketResponseType CreateBucket(CreateBucketType request) throws EucalyptusCloudException {
        return walrusManager.createBucket(request);
    }

    public DeleteBucketResponseType DeleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
        return walrusManager.deleteBucket(request);
    }

    public ListAllMyBucketsResponseType ListAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException {
        return walrusManager.listAllMyBuckets(request);
    }

    public GetBucketAccessControlPolicyResponseType GetBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws EucalyptusCloudException
    {
        return walrusManager.getBucketAccessControlPolicy(request);
    }


    public PutObjectResponseType PutObject (PutObjectType request) throws EucalyptusCloudException {
        return walrusManager.putObject(request);
    }

    public PostObjectResponseType PostObject (PostObjectType request) throws EucalyptusCloudException {
        return walrusManager.postObject(request);
    }

    public PutObjectInlineResponseType PutObjectInline (PutObjectInlineType request) throws EucalyptusCloudException {
        return walrusManager.putObjectInline(request);
    }

    public void AddObject (String userId, String bucketName, String key) throws EucalyptusCloudException {
        walrusManager.addObject(userId, bucketName, key);
    }

    public DeleteObjectResponseType DeleteObject (DeleteObjectType request) throws EucalyptusCloudException {
        return walrusManager.deleteObject(request);
    }

    public ListBucketResponseType ListBucket(ListBucketType request) throws EucalyptusCloudException {
        return walrusManager.listBucket(request);
    }

    public GetObjectAccessControlPolicyResponseType GetObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        return walrusManager.getObjectAccessControlPolicy(request);
    }

    public SetBucketAccessControlPolicyResponseType SetBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException
    {
        return walrusManager.setBucketAccessControlPolicy(request);
    }

    public SetObjectAccessControlPolicyResponseType SetObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws EucalyptusCloudException
    {
        return walrusManager.setObjectAccessControlPolicy(request);
    }

    public GetObjectResponseType GetObject(GetObjectType request) throws EucalyptusCloudException {
        return walrusManager.getObject(request);
    }

    public GetObjectExtendedResponseType GetObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
        return walrusManager.getObjectExtended(request);
    }

    public GetBucketLocationResponseType GetBucketLocation(GetBucketLocationType request) throws EucalyptusCloudException {
        return walrusManager.getBucketLocation(request);
    }

    public CopyObjectResponseType CopyObject(CopyObjectType request) throws EucalyptusCloudException {
        return walrusManager.copyObject(request);
    }

    public GetBucketLoggingStatusResponseType GetBucketLoggingStatus(GetBucketLoggingStatusType request) throws EucalyptusCloudException {
        GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request.getReply();

        throw new NotImplementedException("GetBucketLoggingStatus");
    }

    public SetBucketLoggingStatusResponseType SetBucketLoggingStatus(SetBucketLoggingStatusType request) throws EucalyptusCloudException {
        SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request.getReply();

        throw new NotImplementedException("SetBucketLoggingStatus");
    }

    public GetDecryptedImageResponseType GetDecryptedImage(GetDecryptedImageType request) throws EucalyptusCloudException {
        return walrusImageManager.getDecryptedImage(request);
    }

    public CheckImageResponseType CheckImage(CheckImageType request) throws EucalyptusCloudException {
        return walrusImageManager.checkImage(request);
    }

    public CacheImageResponseType CacheImage(CacheImageType request) throws EucalyptusCloudException {
        return walrusImageManager.cacheImage(request);
    }

    public FlushCachedImageResponseType FlushCachedImage(FlushCachedImageType request) throws EucalyptusCloudException {
        return walrusImageManager.flushCachedImage(request);
    }

    public StoreSnapshotResponseType StoreSnapshot(StoreSnapshotType request) throws EucalyptusCloudException {
        return walrusBlockStorageManager.storeSnapshot(request);
    }

    public GetWalrusSnapshotResponseType GetWalrusSnapshot(GetWalrusSnapshotType request) throws EucalyptusCloudException {
        return walrusBlockStorageManager.getSnapshot(request);
    }

    public DeleteWalrusSnapshotResponseType DeleteWalrusSnapshot(DeleteWalrusSnapshotType request) throws EucalyptusCloudException {
        return walrusBlockStorageManager.deleteWalrusSnapshot(request);
    }

}
