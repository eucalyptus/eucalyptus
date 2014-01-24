/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.tokens.SecurityToken;
import com.eucalyptus.auth.tokens.SecurityTokenManager;
import com.google.common.base.Objects;
import org.apache.log4j.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.objectstorage.util.S3Client;
import com.eucalyptus.util.EucalyptusCloudException;


public class SnapshotObjectOps {
    private Logger LOG = Logger.getLogger( SnapshotObjectOps.class );

    S3Client s3Client;

    public SnapshotObjectOps() {
        try {
            Account  blockstorageAccount = Accounts.lookupAccountByName( StorageProperties.BLOCKSTORAGE_ACCOUNT);
            Role role = blockstorageAccount.lookupRoleByName("S3Access");
            SecurityToken token = SecurityTokenManager.issueSecurityToken(role, (int) TimeUnit.HOURS.toSeconds(1));
            s3Client = new S3Client(new BasicAWSCredentials(token.getAccessKeyId(), token.getSecretKey()), false);
            s3Client.setUsePathStyle(true);
            s3Client.setS3Endpoint(StorageProperties.WALRUS_URL);
        } catch (AuthException ex) {
            LOG.error("Something went really wrong. Block storage account does not exist or have an associated role.");
            LOG.error(ex, ex);
        }
    }

    public void uploadSnapshot(File snapshotFile,
                               SnapshotProgressCallback callback,
                               String snapshotKey,
                               String snapshotId) throws EucalyptusCloudException {
        try {
            s3Client.getS3Client().listObjects(StorageProperties.SNAPSHOT_BUCKET);
        } catch (Exception ex) {
            try {
                //if (!s3Client.getS3Client().doesBucketExist(StorageProperties.SNAPSHOT_BUCKET)) {
                s3Client.getS3Client().createBucket(StorageProperties.SNAPSHOT_BUCKET);
                //}
            } catch (Exception e) {
                LOG.error("Snapshot upload failed. Unable to create bucket: snapshots", e);
                throw new EucalyptusCloudException(e);
            }
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(snapshotFile.length());
            //FIXME: need to set MD5
            s3Client.getS3Client().putObject(StorageProperties.SNAPSHOT_BUCKET, snapshotKey, new FileInputStreamWithCallback(snapshotFile, callback), metadata);
        } catch (Exception ex) {
            LOG.error("Snapshot " + snapshotId + " upload failed to: " + snapshotKey, ex);
            throw new EucalyptusCloudException(ex);
        }
    }

    public void deleteSnapshot(String snapshotLocation, String snapshotId) throws EucalyptusCloudException {
        try {
            s3Client.getS3Client().deleteObject(StorageProperties.SNAPSHOT_BUCKET, snapshotLocation);
        } catch (Exception ex) {
            LOG.error("Snapshot delete failed for: " + snapshotId, ex);
            throw new EucalyptusCloudException(ex);
        }
        try {
            s3Client.getS3Client().deleteBucket(StorageProperties.SNAPSHOT_BUCKET);
        } catch (Exception ex) {
            LOG.error("Snapshot bucket delete failed for: " + StorageProperties.SNAPSHOT_BUCKET, ex);
            throw new EucalyptusCloudException(ex);
        }
    }


    /**
     * File stream with update callbacks to update on progress. Will call
     * callback on each operation, callback must be selective on when to do update and
     * should do so asynchronously for best performance.
     *
     */
    public class FileInputStreamWithCallback extends FileInputStream {

        private long totalRead;
        private long fileSize;
        private SnapshotProgressCallback callback;

        public FileInputStreamWithCallback(File file, SnapshotProgressCallback callback)
                throws FileNotFoundException {
            super(file);
            totalRead = 0;
            fileSize = file.length();
            this.callback = callback;
        }

        @Override
        public int available() throws IOException {
            int available = super.available();
            if (available == 0) {
                callback.finish();
            }
            return available;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int bytesRead = super.read(b, off, len);
            totalRead += bytesRead;
            callback.update(totalRead);
            return bytesRead;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int bytesRead = super.read(b);
            totalRead += bytesRead;
            callback.update(totalRead);
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            if(totalRead == fileSize) {
                callback.finish();
            } else {
                callback.failed();
            }
            super.close();
        }

    }


    public void downloadSnapshot(String snapshotBucket, String snapshotLocation,
                                 File tmpCompressedFile) throws EucalyptusCloudException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(snapshotBucket, snapshotLocation);
        try {
            long startTime = System.currentTimeMillis();
            s3Client.getS3Client().getObject(getObjectRequest, tmpCompressedFile);
            LOG.info("Snapshot " + snapshotBucket + "/" + snapshotLocation + " download took " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
        } catch (Exception ex) {
            LOG.error("Snapshot download failed for: " + snapshotLocation, ex);
            throw new EucalyptusCloudException(ex);
        }
    }

}
