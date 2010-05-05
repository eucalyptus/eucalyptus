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

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.storage.StorageManager;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.WalrusProperties;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.util.EucalyptusCloudException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activity.InvalidActivityException;

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
	}

	public void cleanFailedCachedImages() {
		EntityWrapper<ImageCacheInfo> db = WalrusControl.getEntityWrapper();
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
		} else {
			throw new InvalidArgumentException("Snapshot size");
		}

		int snapshotSize = (int)(snapSize / WalrusProperties.G);
		if (WalrusProperties.shouldEnforceUsageLimits) {
			int totalSnapshotSize = 0;
			WalrusSnapshotInfo snapInfo = new WalrusSnapshotInfo();
			EntityWrapper<WalrusSnapshotInfo> db = WalrusControl.getEntityWrapper();
			List<WalrusSnapshotInfo> sInfos = db.query(snapInfo);
			for (WalrusSnapshotInfo sInfo : sInfos) {
				totalSnapshotSize += sInfo.getSize();
			}
			if ((totalSnapshotSize + snapshotSize) > WalrusInfo.getWalrusInfo().getStorageMaxTotalSnapshotSizeInGb()) {
				db.rollback();
				throw new EntityTooLargeException(snapshotId);
			}
			db.commit();
		}

		boolean createBucket = true;
		EntityWrapper<WalrusSnapshotInfo> db = WalrusControl.getEntityWrapper();
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
			snapshotInfo = new WalrusSnapshotInfo(snapshotId);
			db = WalrusControl.getEntityWrapper();
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
		EntityWrapper<WalrusSnapshotInfo> db = WalrusControl.getEntityWrapper();
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
		return null;
	}

	public DeleteWalrusSnapshotResponseType deleteWalrusSnapshot(
			DeleteWalrusSnapshotType request) throws EucalyptusCloudException {
		DeleteWalrusSnapshotResponseType reply = (DeleteWalrusSnapshotResponseType) request
		.getReply();
		String snapshotId = request.getKey();

		// Load the entire snapshot tree and then remove the snapshot
		EntityWrapper<WalrusSnapshotInfo> db = WalrusControl.getEntityWrapper();
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

	public GetWalrusSnapshotSizeResponseType getWalrusSnapshotSize(GetWalrusSnapshotSizeType request) throws EucalyptusCloudException {
		GetWalrusSnapshotSizeResponseType reply = (GetWalrusSnapshotSizeResponseType) request.getReply();
		String snapshotId = request.getKey();
		EntityWrapper<WalrusSnapshotInfo> db = WalrusControl.getEntityWrapper();
		WalrusSnapshotInfo snapshotInfo = new WalrusSnapshotInfo(snapshotId);
		List<WalrusSnapshotInfo> snapshotInfos = db.query(snapshotInfo);
		int size = 0;
		if (snapshotInfos.size() > 0) {
			WalrusSnapshotInfo foundSnapshotInfo = snapshotInfos.get(0);
			size = foundSnapshotInfo.getSize();
		}
		db.commit();
		Channel channel = request.getChannel();
		if(channel != null) {
			DefaultHttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK ); 
			httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(0));
			httpResponse.addHeader("SnapshotSize", String.valueOf(size));
			channel.write(httpResponse);
		} else {
			throw new EucalyptusCloudException("Could not get channel to write response");
		}
		return reply;
	}

}
