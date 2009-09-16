/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage;

import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import edu.ucsb.eucalyptus.cloud.ws.BlockStorage;
import edu.ucsb.eucalyptus.cloud.ws.HttpWriter;
import edu.ucsb.eucalyptus.cloud.ws.SnapshotProgressCallback;
import edu.ucsb.eucalyptus.ic.StorageController;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockStorageChecker {
    private static Logger LOG = Logger.getLogger(BlockStorageChecker.class);
    private StorageManager volumeStorageManager;
    private StorageManager snapshotStorageManager;
    private LogicalStorageManager blockManager;

    public BlockStorageChecker(StorageManager volumeStorageManager, 
    		StorageManager snapshotStorageManager, 
    		LogicalStorageManager blockManager) {
        this.volumeStorageManager = volumeStorageManager;
        this.snapshotStorageManager = snapshotStorageManager;
        this.blockManager = blockManager;
    }

    public void cleanup() throws EucalyptusCloudException {
        cleanVolumes();
        cleanSnapshots();
        transferPendingSnapshots();
    }

    public void cleanVolumes() {
        cleanStuckVolumes();
        cleanFailedVolumes();
    }

    public void cleanSnapshots() {
        cleanStuckSnapshots();
        cleanFailedSnapshots();
    }

    public void cleanStuckVolumes() {
        EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setStatus(StorageProperties.Status.creating.toString());
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        for(VolumeInfo volInfo : volumeInfos) {
            String volumeId = volInfo.getVolumeId();
            LOG.info("Cleaning failed volume " + volumeId);
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

    public void cleanFailedVolumes() {
        EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
        VolumeInfo volumeInfo = new VolumeInfo();
        volumeInfo.setStatus(StorageProperties.Status.failed.toString());
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        for(VolumeInfo volInfo : volumeInfos) {
            String volumeId = volInfo.getVolumeId();
            LOG.info("Cleaning failed volume " + volumeId);
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

    public void cleanFailedVolume(String volumeId) {
        EntityWrapper<VolumeInfo> db = StorageController.getEntityWrapper();
        VolumeInfo volumeInfo = new VolumeInfo(volumeId);
        List<VolumeInfo> volumeInfos = db.query(volumeInfo);
        if(volumeInfos.size() > 0) {
            VolumeInfo volInfo = volumeInfos.get(0);
            LOG.info("Cleaning failed volume " + volumeId);
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

    public void cleanStuckSnapshots() {
        EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setStatus(StorageProperties.Status.creating.toString());
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        for(SnapshotInfo snapInfo : snapshotInfos) {
            String snapshotId = snapInfo.getSnapshotId();
            LOG.info("Cleaning failed snapshot " + snapshotId);
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

    public void cleanFailedSnapshots() {
        EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setStatus(StorageProperties.Status.failed.toString());
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        for(SnapshotInfo snapInfo : snapshotInfos) {
            String snapshotId = snapInfo.getSnapshotId();
            LOG.info("Cleaning failed snapshot " + snapshotId);
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

    public void cleanFailedSnapshot(String snapshotId) {
        EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
        SnapshotInfo snapshotInfo = new SnapshotInfo(snapshotId);
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        if(snapshotInfos.size() > 0) {
            SnapshotInfo snapInfo = snapshotInfos.get(0);
            LOG.info("Cleaning failed snapshot " + snapshotId);
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

    private void transferPendingSnapshots() throws EucalyptusCloudException {
        EntityWrapper<SnapshotInfo> db = StorageController.getEntityWrapper();
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setShouldTransfer(true);
        List<SnapshotInfo> snapshotInfos = db.query(snapshotInfo);
        if(snapshotInfos.size() > 0) {
            SnapshotInfo snapInfo = snapshotInfos.get(0);
            String snapshotId = snapInfo.getSnapshotId();
			List<String> returnValues = blockManager.prepareForTransfer(snapshotId);
			String snapshotFileName = returnValues.get(0);
			File snapshotFile = new File(snapshotFileName);
			Map<String, String> httpParamaters = new HashMap<String, String>();
			HttpWriter httpWriter;
			SnapshotProgressCallback callback = new SnapshotProgressCallback(snapshotId, snapshotFile.length(), StorageProperties.TRANSFER_CHUNK_SIZE);
			httpWriter = new HttpWriter("PUT", snapshotFile, callback, "snapset", snapshotId, "StoreSnapshot", null, httpParamaters);
			try {
				httpWriter.run();
			} catch(Exception ex) {
				LOG.error(ex, ex);
				this.cleanFailedSnapshot(snapshotId);
			}
        }
        db.commit();
    }
    
    public static void checkWalrusConnection() {
        HttpClient httpClient = new HttpClient();
        GetMethod getMethod = null;
        try {
            java.net.URI addrUri = new URL(StorageProperties.WALRUS_URL).toURI();
            String addrPath = addrUri.getPath();
            String addr = StorageProperties.WALRUS_URL.replaceAll(addrPath, "");
            getMethod = new GetMethod(addr);

            httpClient.executeMethod(getMethod);
            StorageProperties.enableSnapshots = true;
        } catch(Exception ex) {
            LOG.error("Could not connect to Walrus. Snapshot functionality disabled. Please check the Walrus url.");
            StorageProperties.enableSnapshots = false;
        } finally {
            if(getMethod != null)
                getMethod.releaseConnection();
        }
    }
}
