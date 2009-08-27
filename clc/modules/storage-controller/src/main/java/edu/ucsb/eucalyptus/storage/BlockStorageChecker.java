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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.storage;

import edu.ucsb.eucalyptus.cloud.entities.SnapshotInfo;
import edu.ucsb.eucalyptus.cloud.entities.VolumeInfo;
import com.eucalyptus.util.StorageProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;

import java.net.URL;
import java.util.List;

public class BlockStorageChecker {
    private static Logger LOG = Logger.getLogger(BlockStorageChecker.class);
    private StorageManager volumeStorageManager;
    private StorageManager snapshotStorageManager;
    private LogicalStorageManager blockManager;

    public BlockStorageChecker(StorageManager volumeStorageManager, StorageManager snapshotStorageManager, LogicalStorageManager blockManager) {
        this.volumeStorageManager = volumeStorageManager;
        this.snapshotStorageManager = snapshotStorageManager;
        this.blockManager = blockManager;
    }

    public void cleanup() {
        cleanVolumes();
        cleanSnapshots();
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
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
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
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
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
        EntityWrapper<VolumeInfo> db = new EntityWrapper<VolumeInfo>();
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
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
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
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
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
        EntityWrapper<SnapshotInfo> db = new EntityWrapper<SnapshotInfo>();
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
