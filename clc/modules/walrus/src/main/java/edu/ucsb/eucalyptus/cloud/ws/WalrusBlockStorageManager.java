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

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import com.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;

import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WalrusBlockStorageManager {
	private static Logger LOG = Logger
			.getLogger(WalrusBlockStorageManager.class);
	private StorageManager storageManager;
	private WalrusManager walrusManager;

	public WalrusBlockStorageManager(StorageManager storageManager,
			WalrusManager walrusManager) {
		this.storageManager = storageManager;
		this.walrusManager = walrusManager;
		startupChecks();
	}

	public void startupChecks() {
		cleanFailedCachedImages();
		WalrusProperties.sharedMode = true;
	}

	public void cleanFailedCachedImages() {
		EntityWrapper<ImageCacheInfo> db = new EntityWrapper<ImageCacheInfo>();
		ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
		searchImageCacheInfo.setInCache(false);
		List<ImageCacheInfo> icInfos = db.query(searchImageCacheInfo);
		for (ImageCacheInfo icInfo : icInfos) {
			String decryptedImageName = icInfo.getImageName();
			String bucket = icInfo.getBucketName();
			LOG.info("Cleaning failed cache entry: " + bucket + "/"
					+ icInfo.getManifestName());
			try {
				if (decryptedImageName.contains(".tgz")) {
					storageManager.deleteObject(bucket, decryptedImageName
							.replaceAll(".tgz", "crypt.gz"));
					storageManager.deleteObject(bucket, decryptedImageName
							.replaceAll(".tgz", ".tar"));
					storageManager.deleteObject(bucket, decryptedImageName
							.replaceAll(".tgz", ""));
				}
				storageManager.deleteObject(bucket, decryptedImageName);
			} catch (IOException ex) {
				LOG.error(ex);
			}
			db.delete(icInfo);
		}
		db.commit();
	}

	public StoreSnapshotResponseType storeSnapshot(StoreSnapshotType request)
			throws EucalyptusCloudException {
		StoreSnapshotResponseType reply = (StoreSnapshotResponseType) request
				.getReply();

		String snapshotId = request.getKey();
		String bucketName = request.getBucket();
		String snapSizeString = request.getSnapshotSize();
		Long snapSize = 0L;
		if (snapSizeString != null) {
			snapSize = Long.parseLong(snapSizeString);
		}
		if (WalrusProperties.shouldEnforceUsageLimits) {
			int totalSnapshotSize = 0;
			WalrusSnapshotInfo snapInfo = new WalrusSnapshotInfo();
			EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
			List<WalrusSnapshotInfo> sInfos = db.query(snapInfo);
			for (WalrusSnapshotInfo sInfo : sInfos) {
				totalSnapshotSize += sInfo.getSize();
			}
			if ((totalSnapshotSize + (int) (snapSize / WalrusProperties.G)) > WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE) {
				db.rollback();
				throw new EntityTooLargeException(snapshotId);
			}
			db.commit();
		}

		boolean createBucket = true;
		EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
		WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
		List<WalrusSnapshotInfo> snapInfos = db.query(snapshotInfo);
		if (snapInfos.size() > 0) {
			db.rollback();
			throw new EntityAlreadyExistsException(snapshotId);
		}
		db.commit();

		// set snapshot props
		// read and store it
		// convert to a PutObject request

		String userId = request.getUserId();
		if (createBucket) {
			CreateBucketType createBucketRequest = new CreateBucketType();
			createBucketRequest.setUserId(userId);
			createBucketRequest.setBucket(bucketName);
			createBucketRequest
					.setEffectiveUserId(request.getEffectiveUserId());
			try {
				walrusManager.createBucket(createBucketRequest);
			} catch (EucalyptusCloudException ex) {
				if (!(ex instanceof BucketAlreadyExistsException || ex instanceof BucketAlreadyOwnedByYouException)) {
					throw ex;
				}
			}
		}

		// put happens synchronously
		PutObjectType putObjectRequest = new PutObjectType();
		putObjectRequest.setUserId(userId);
		putObjectRequest.setBucket(bucketName);
		putObjectRequest.setKey(snapshotId);
		putObjectRequest.setRandomKey(request.getRandomKey());
		putObjectRequest.setEffectiveUserId(request.getEffectiveUserId());
		try {
			PutObjectResponseType putObjectResponseType = walrusManager
					.putObject(putObjectRequest);
			reply.setEtag(putObjectResponseType.getEtag());
			reply.setLastModified(putObjectResponseType.getLastModified());
			reply.setStatusMessage(putObjectResponseType.getStatusMessage());
			int snapshotSize = (int) (putObjectResponseType.getSize() / WalrusProperties.G);

			// change state
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

	public GetWalrusSnapshotResponseType getSnapshot(
			GetWalrusSnapshotType request) throws EucalyptusCloudException {
		GetWalrusSnapshotResponseType reply = (GetWalrusSnapshotResponseType) request
				.getReply();
		String snapshotId = request.getKey();
		EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
		WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
		List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);
		if (snapshotInfos.size() > 0) {
			WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
			String bucketName = foundSnapshotInfo.getSnapshotBucket();
			db.commit();
			GetObjectType getObjectType = new GetObjectType();
			getObjectType.setBucket(bucketName);
			getObjectType.setUserId(request.getUserId());
			getObjectType.setEffectiveUserId(request.getEffectiveUserId());
			getObjectType.setKey(snapshotId);
			getObjectType.setChannel(request.getChannel());
			getObjectType.setDeleteAfterGet(false);
			getObjectType.setGetData(true);
			getObjectType.setInlineData(false);
			getObjectType.setGetMetaData(false);
			getObjectType.setIsCompressed(false);
			try {
				walrusManager.getObject(getObjectType);
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex, ex);
				throw ex;
			}
		} else {
			db.rollback();
			throw new NoSuchEntityException(snapshotId);
		}
		return reply;
	}

	public DeleteWalrusSnapshotResponseType deleteWalrusSnapshot(
			DeleteWalrusSnapshotType request) throws EucalyptusCloudException {
		DeleteWalrusSnapshotResponseType reply = (DeleteWalrusSnapshotResponseType) request
				.getReply();
		String snapshotId = request.getKey();

		// Load the entire snapshot tree and then remove the snapshot
		EntityWrapper<WalrusSnapshotInfo> db = new EntityWrapper<WalrusSnapshotInfo>();
		WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
		List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);

		// Delete is idempotent.
		reply.set_return(true);
		if (snapshotInfos.size() > 0) {
			WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
			// remove the snapshot in the background
			db.delete(foundSnapshotInfo);
			db.commit();
			SnapshotDeleter snapshotDeleter = new SnapshotDeleter(request
					.getUserId(), 
					request.getEffectiveUserId(),
					foundSnapshotInfo.getSnapshotBucket(),
					snapshotId);
			snapshotDeleter.start();
		} else {
			db.rollback();
			throw new NoSuchSnapshotException(snapshotId);
		}
		return reply;
	}

	private class SnapshotDeleter extends Thread {
		private String userId;
		private String effectiveUserId;
		private String bucketName;
		private String snapshotId;

		public SnapshotDeleter(String userId, 
				String effectiveUserId,
				String bucketName,
				String snapshotId) {
			this.userId = userId;
			this.effectiveUserId = effectiveUserId;
			this.bucketName = bucketName;
			this.snapshotId = snapshotId;
		}

		public void run() {
			DeleteObjectType deleteObjectType = new DeleteObjectType();
			deleteObjectType.setBucket(bucketName);
			deleteObjectType.setKey(snapshotId);
			deleteObjectType.setEffectiveUserId(effectiveUserId);
			deleteObjectType.setUserId(userId);

			try {
				walrusManager.deleteObject(deleteObjectType);
				DeleteBucketType deleteBucketType = new DeleteBucketType();
				deleteBucketType.setBucket(bucketName);
				deleteBucketType.setEffectiveUserId(effectiveUserId);
				deleteBucketType.setUserId(userId);
				walrusManager.deleteBucket(deleteBucketType);
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex, ex);
				return;
			}
		}
	}

}
