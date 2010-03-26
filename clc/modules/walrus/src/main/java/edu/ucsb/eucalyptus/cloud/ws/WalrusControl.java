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
 * Author: Neil Soman neil@eucalyptus.com
 */

package edu.ucsb.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.NeedsDeferredInitialization;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.NotImplementedException;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.msgs.AddObjectResponseType;
import edu.ucsb.eucalyptus.msgs.AddObjectType;
import edu.ucsb.eucalyptus.msgs.CacheImageResponseType;
import edu.ucsb.eucalyptus.msgs.CacheImageType;
import edu.ucsb.eucalyptus.msgs.CheckImageResponseType;
import edu.ucsb.eucalyptus.msgs.CheckImageType;
import edu.ucsb.eucalyptus.msgs.CopyObjectResponseType;
import edu.ucsb.eucalyptus.msgs.CopyObjectType;
import edu.ucsb.eucalyptus.msgs.CreateBucketResponseType;
import edu.ucsb.eucalyptus.msgs.CreateBucketType;
import edu.ucsb.eucalyptus.msgs.DeleteBucketResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteBucketType;
import edu.ucsb.eucalyptus.msgs.DeleteObjectResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteObjectType;
import edu.ucsb.eucalyptus.msgs.DeleteVersionResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVersionType;
import edu.ucsb.eucalyptus.msgs.DeleteWalrusSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteWalrusSnapshotType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageResponseType;
import edu.ucsb.eucalyptus.msgs.FlushCachedImageType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusType;
import edu.ucsb.eucalyptus.msgs.GetBucketVersioningStatusResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketVersioningStatusType;
import edu.ucsb.eucalyptus.msgs.GetDecryptedImageResponseType;
import edu.ucsb.eucalyptus.msgs.GetDecryptedImageType;
import edu.ucsb.eucalyptus.msgs.GetObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectExtendedResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectExtendedType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetWalrusSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.GetWalrusSnapshotType;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsResponseType;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsType;
import edu.ucsb.eucalyptus.msgs.ListBucketResponseType;
import edu.ucsb.eucalyptus.msgs.ListBucketType;
import edu.ucsb.eucalyptus.msgs.ListVersionsResponseType;
import edu.ucsb.eucalyptus.msgs.ListVersionsType;
import edu.ucsb.eucalyptus.msgs.PostObjectResponseType;
import edu.ucsb.eucalyptus.msgs.PostObjectType;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineType;
import edu.ucsb.eucalyptus.msgs.PutObjectResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectType;
import edu.ucsb.eucalyptus.msgs.SetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetBucketLoggingStatusResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketLoggingStatusType;
import edu.ucsb.eucalyptus.msgs.SetBucketVersioningStatusResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketVersioningStatusType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.StoreSnapshotResponseType;
import edu.ucsb.eucalyptus.msgs.StoreSnapshotType;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileSystemStorageManager;
import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;

@NeedsDeferredInitialization(component = Component.walrus)
public class WalrusControl {

	private static Logger LOG = Logger.getLogger( WalrusControl.class );

	private static WalrusDataMessenger imageMessenger = new WalrusDataMessenger();
	private static StorageManager storageManager;
	private static WalrusManager walrusManager;
	private static WalrusBlockStorageManager walrusBlockStorageManager;
	private static WalrusImageManager walrusImageManager;

	public static void deferredInitializer() {
		configure();
		storageManager = new FileSystemStorageManager(WalrusProperties.bucketRootDirectory);
		walrusImageManager = new WalrusImageManager(storageManager, imageMessenger);
		walrusManager = new WalrusManager(storageManager, walrusImageManager);
		walrusBlockStorageManager = new WalrusBlockStorageManager(storageManager, walrusManager);
		String limits = System.getProperty(WalrusProperties.USAGE_LIMITS_PROPERTY);
		if(limits != null) {
			WalrusProperties.shouldEnforceUsageLimits = Boolean.parseBoolean(limits);
		}
		try {
			walrusManager.check();
		} catch(EucalyptusCloudException ex) {
			LOG.error("Error initializing walrus", ex);
			SystemUtil.shutdownWithError(ex.getMessage());
		}
		Tracker.initialize();
		if(System.getProperty("euca.virtualhosting.disable") != null) {
			WalrusProperties.enableVirtualHosting = false;
		}
	}

	public WalrusControl() {}

	public static <T> EntityWrapper<T> getEntityWrapper( ) {
		return new EntityWrapper<T>( WalrusProperties.DB_NAME );
	}
	
	private static void configure() {
		WalrusInfo walrusInfo = getConfig();
		WalrusProperties.NAME = walrusInfo.getName();
		WalrusProperties.MAX_BUCKETS_PER_USER = walrusInfo.getStorageMaxBucketsPerUser();
		WalrusProperties.MAX_BUCKET_SIZE = walrusInfo.getStorageMaxBucketSizeInMB() * WalrusProperties.M;
		WalrusProperties.bucketRootDirectory = walrusInfo.getStorageDir();
		WalrusProperties.IMAGE_CACHE_SIZE = walrusInfo.getStorageMaxCacheSizeInMB() * WalrusProperties.M;
		WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE = walrusInfo.getStorageMaxTotalSnapshotSizeInGb();
	}

	private static WalrusInfo getConfig() {
		EntityWrapper<WalrusInfo> db = WalrusControl.getEntityWrapper();
		WalrusInfo walrusInfo;
		try {
			walrusInfo = db.getUnique(new WalrusInfo());
      db.commit();
		} catch(EucalyptusCloudException ex) {
			walrusInfo = new WalrusInfo(WalrusProperties.NAME, 
					WalrusProperties.bucketRootDirectory, 
					WalrusProperties.MAX_BUCKETS_PER_USER, 
					(int)(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M),
					(int)(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties.M),
					WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE);
			db.add(walrusInfo);
      db.commit();
		}
		return walrusInfo;
	}

	private static void updateConfig() {
		EntityWrapper<WalrusInfo> db = WalrusControl.getEntityWrapper();
		WalrusInfo walrusInfo;
		try {
			walrusInfo = db.getUnique(new WalrusInfo());
			walrusInfo.setName(WalrusProperties.NAME);
			walrusInfo.setStorageDir(WalrusProperties.bucketRootDirectory);
			walrusInfo.setStorageMaxBucketsPerUser(WalrusProperties.MAX_BUCKETS_PER_USER);
			walrusInfo.setStorageMaxBucketSizeInMB((int)(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M));
			walrusInfo.setStorageMaxCacheSizeInMB((int)(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties.M));
			walrusInfo.setStorageMaxTotalSnapshotSizeInGb(WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE);
      db.commit();
		} catch(EucalyptusCloudException ex) {
			walrusInfo = new WalrusInfo(WalrusProperties.NAME, 
					WalrusProperties.bucketRootDirectory, 
					WalrusProperties.MAX_BUCKETS_PER_USER, 
					(int)(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M),
					(int)(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties.M),
					WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE);
			db.add(walrusInfo);
      db.commit();
		} 
	}


	public UpdateWalrusConfigurationResponseType UpdateWalrusConfiguration(UpdateWalrusConfigurationType request) throws EucalyptusCloudException {
		UpdateWalrusConfigurationResponseType reply = (UpdateWalrusConfigurationResponseType) request.getReply();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		String name = request.getName();
		if(name != null)
			WalrusProperties.NAME = name;
		String rootDir = request.getBucketRootDirectory();
		if(rootDir != null) {
			WalrusProperties.bucketRootDirectory = rootDir;
			storageManager.setRootDirectory(rootDir);
		}
		Integer maxBucketsPerUser = request.getMaxBucketsPerUser();
		if(maxBucketsPerUser != null)
			WalrusProperties.MAX_BUCKETS_PER_USER = maxBucketsPerUser;
		Long maxBucketSize = request.getMaxBucketSize();
		if(maxBucketSize != null)
			WalrusProperties.MAX_BUCKET_SIZE = maxBucketSize * WalrusProperties.M;    	
		Long imageCacheSize = request.getImageCacheSize();
		if(imageCacheSize != null)
			WalrusProperties.IMAGE_CACHE_SIZE = imageCacheSize * WalrusProperties.M;
		Integer totalSnapshotSize = request.getTotalSnapshotSize();
		if(totalSnapshotSize != null)
			WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE = totalSnapshotSize;
		walrusManager.check();
		updateConfig();
		return reply;
	}

	public GetWalrusConfigurationResponseType GetWalrusConfiguration(GetWalrusConfigurationType request) throws EucalyptusCloudException {
		GetWalrusConfigurationResponseType reply = (GetWalrusConfigurationResponseType) request.getReply();
		if(Component.eucalyptus.name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		String name = request.getName();
		if(WalrusProperties.NAME.equals(name)) {
			reply.setBucketRootDirectory(WalrusProperties.bucketRootDirectory);
			reply.setMaxBucketsPerUser(WalrusProperties.MAX_BUCKETS_PER_USER);
			reply.setMaxBucketSize(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M);
			reply.setImageCacheSize(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties. M);
			reply.setTotalSnapshotSize(WalrusProperties.MAX_TOTAL_SNAPSHOT_SIZE);
		}
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

	public AddObjectResponseType AddObject (AddObjectType request) throws EucalyptusCloudException {
		return walrusManager.addObject(request);
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

	public SetRESTBucketAccessControlPolicyResponseType SetRESTBucketAccessControlPolicy(SetRESTBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return walrusManager.setRESTBucketAccessControlPolicy(request);
	}

	public SetRESTObjectAccessControlPolicyResponseType SetRESTObjectAccessControlPolicy(SetRESTObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return walrusManager.setRESTObjectAccessControlPolicy(request);
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
		return walrusManager.getBucketLoggingStatus(request);
	}

	public SetBucketLoggingStatusResponseType SetBucketLoggingStatus(SetBucketLoggingStatusType request) throws EucalyptusCloudException {
		return walrusManager.setBucketLoggingStatus(request);
	}

	public GetBucketVersioningStatusResponseType GetBucketVersioningStatus(GetBucketVersioningStatusType request) throws EucalyptusCloudException {
		return walrusManager.getBucketVersioningStatus(request);
	}

	public SetBucketVersioningStatusResponseType SetBucketVersioningStatus(SetBucketVersioningStatusType request) throws EucalyptusCloudException {
		return walrusManager.setBucketVersioningStatus(request);
	}
	
	public ListVersionsResponseType ListVersions(ListVersionsType request) throws EucalyptusCloudException {
		return walrusManager.listVersions(request);
	}
	
	public DeleteVersionResponseType DeleteVersion(DeleteVersionType request) throws EucalyptusCloudException {
		return walrusManager.deleteVersion(request);
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
