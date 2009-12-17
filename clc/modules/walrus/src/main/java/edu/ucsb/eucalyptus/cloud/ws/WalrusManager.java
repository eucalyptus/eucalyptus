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
package edu.ucsb.eucalyptus.cloud.ws;
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.CredentialProvider;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.NeedsDeferredInitialization;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.eucalyptus.ws.handlers.WalrusRESTBinding;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyOwnedByYouException;
import edu.ucsb.eucalyptus.cloud.BucketNotEmptyException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.InvalidRangeException;
import edu.ucsb.eucalyptus.cloud.InvalidTargetBucketForLoggingException;
import edu.ucsb.eucalyptus.cloud.NoSuchBucketException;
import edu.ucsb.eucalyptus.cloud.NoSuchEntityException;
import edu.ucsb.eucalyptus.cloud.NotModifiedException;
import edu.ucsb.eucalyptus.cloud.PreconditionFailedException;
import edu.ucsb.eucalyptus.cloud.TooManyBucketsException;
import edu.ucsb.eucalyptus.cloud.entities.BucketInfo;
import edu.ucsb.eucalyptus.cloud.entities.GrantInfo;
import edu.ucsb.eucalyptus.cloud.entities.ImageCacheInfo;
import edu.ucsb.eucalyptus.cloud.entities.MetaDataInfo;
import edu.ucsb.eucalyptus.cloud.entities.ObjectInfo;
import edu.ucsb.eucalyptus.cloud.entities.TorrentInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusSnapshotInfo;
import edu.ucsb.eucalyptus.msgs.AccessControlListType;
import edu.ucsb.eucalyptus.msgs.AccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.AddObjectResponseType;
import edu.ucsb.eucalyptus.msgs.AddObjectType;
import edu.ucsb.eucalyptus.msgs.BucketListEntry;
import edu.ucsb.eucalyptus.msgs.CanonicalUserType;
import edu.ucsb.eucalyptus.msgs.CopyObjectResponseType;
import edu.ucsb.eucalyptus.msgs.CopyObjectType;
import edu.ucsb.eucalyptus.msgs.CreateBucketResponseType;
import edu.ucsb.eucalyptus.msgs.CreateBucketType;
import edu.ucsb.eucalyptus.msgs.DeleteBucketResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteBucketType;
import edu.ucsb.eucalyptus.msgs.DeleteObjectResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteObjectType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetObjectExtendedResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectExtendedType;
import edu.ucsb.eucalyptus.msgs.GetObjectResponseType;
import edu.ucsb.eucalyptus.msgs.GetObjectType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;
import edu.ucsb.eucalyptus.msgs.Group;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsList;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsResponseType;
import edu.ucsb.eucalyptus.msgs.ListAllMyBucketsType;
import edu.ucsb.eucalyptus.msgs.ListBucketResponseType;
import edu.ucsb.eucalyptus.msgs.ListBucketType;
import edu.ucsb.eucalyptus.msgs.ListEntry;
import edu.ucsb.eucalyptus.msgs.MetaDataEntry;
import edu.ucsb.eucalyptus.msgs.PostObjectResponseType;
import edu.ucsb.eucalyptus.msgs.PostObjectType;
import edu.ucsb.eucalyptus.msgs.PrefixEntry;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineType;
import edu.ucsb.eucalyptus.msgs.PutObjectResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectType;
import edu.ucsb.eucalyptus.msgs.RemoveARecordType;
import edu.ucsb.eucalyptus.msgs.SetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetBucketLoggingStatusResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketLoggingStatusType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.Status;
import edu.ucsb.eucalyptus.msgs.TargetGrants;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusMonitor;
import edu.ucsb.eucalyptus.cloud.BucketLogData;

@NeedsDeferredInitialization
public class WalrusManager {
	private static Logger LOG = Logger.getLogger( WalrusManager.class );

	private StorageManager storageManager;
	private WalrusImageManager walrusImageManager;
	private static WalrusStatistics walrusStatistics = null;
	public static void deferredInitializer() {
		walrusStatistics = new WalrusStatistics();
	}

	public WalrusManager(StorageManager storageManager, WalrusImageManager walrusImageManager) {
		this.storageManager = storageManager;
		this.walrusImageManager = walrusImageManager;
	}

	public void initialize() throws EucalyptusCloudException {
		check();
	}

	public void check() throws EucalyptusCloudException {
		File bukkitDir = new File(WalrusProperties.bucketRootDirectory);
		if(!bukkitDir.exists()) {
			if(!bukkitDir.mkdirs()) {
				LOG.fatal("Unable to make bucket root directory: " + WalrusProperties.bucketRootDirectory);
				throw new EucalyptusCloudException("Invalid bucket root directory");
			}
		} else if(!bukkitDir.canWrite()) {
			LOG.fatal("Cannot write to bucket root directory: " + WalrusProperties.bucketRootDirectory);
			throw new EucalyptusCloudException("Invalid bucket root directory");
		}
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo();
		List<BucketInfo> bucketInfos = db.query(bucketInfo);
		for(BucketInfo bucket : bucketInfos) {
			if(!storageManager.bucketExists(bucket.getBucketName()))
				bucket.setHidden(true);
			else
				bucket.setHidden(false);
		}
		db.commit();
	}

	public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException {
		ListAllMyBucketsResponseType reply = (ListAllMyBucketsResponseType) request.getReply();
		String userId = request.getUserId();

		if(userId == null) {
			throw new AccessDeniedException("no such user");
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo searchBucket = new BucketInfo();
		searchBucket.setOwnerId(userId);
		searchBucket.setHidden(false);
		List<BucketInfo> bucketInfoList = db.query(searchBucket);

		ArrayList<BucketListEntry> buckets = new ArrayList<BucketListEntry>();

		for(BucketInfo bucketInfo: bucketInfoList) {
			if(request.isAdministrator()) {
				EntityWrapper<WalrusSnapshotInfo> dbSnap = db.recast(WalrusSnapshotInfo.class);
				WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
				walrusSnapInfo.setSnapshotBucket(bucketInfo.getBucketName());
				List<WalrusSnapshotInfo> walrusSnaps = dbSnap.query(walrusSnapInfo);
				if(walrusSnaps.size() > 0)
					continue;
			}
			buckets.add(new BucketListEntry(bucketInfo.getBucketName(), DateUtils.format(bucketInfo.getCreationDate().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z"));
		}
		try {
			CanonicalUserType owner = new CanonicalUserType(CredentialProvider.getQueryId(userId), userId);
			ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
			reply.setOwner(owner);
			bucketList.setBuckets(buckets);
			reply.setBucketList(bucketList);	
		} catch(Exception ex) {
			db.rollback();
			LOG.error(ex);
			throw new AccessDeniedException("User: " + userId + " not found");
		}
		db.commit();
		return reply;
	}

	public CreateBucketResponseType createBucket(CreateBucketType request) throws EucalyptusCloudException {
		CreateBucketResponseType reply = (CreateBucketResponseType) request.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String locationConstraint = request.getLocationConstraint();

		if(userId == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}

		AccessControlListType accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();

		if(WalrusProperties.shouldEnforceUsageLimits && !request.isAdministrator()) {
			BucketInfo searchBucket = new BucketInfo();
			searchBucket.setOwnerId(userId);
			List<BucketInfo> bucketList = db.query(searchBucket);
			if(bucketList.size() >= WalrusProperties.MAX_BUCKETS_PER_USER) {
				db.rollback();
				throw new TooManyBucketsException(bucketName);
			}
		}

		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if(bucketList.size() > 0) {
			if(bucketList.get(0).getOwnerId().equals(userId)) {
				//bucket already exists and you created it
				db.rollback();
				throw new BucketAlreadyOwnedByYouException(bucketName);
			}
			//bucket already exists
			db.rollback();
			throw new BucketAlreadyExistsException(bucketName);
		}   else {
			//create bucket and set its acl
			BucketInfo bucket = new BucketInfo(userId, bucketName, new Date());
			ArrayList<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
			bucket.addGrants(userId, grantInfos, accessControlList);
			bucket.setGrants(grantInfos);
			bucket.setBucketSize(0L);
			bucket.setLoggingEnabled(false);
			bucket.setHidden(false);
			if(locationConstraint != null)
				bucket.setLocation(locationConstraint);
			else
				bucket.setLocation("US");
			db.add(bucket);
			//call the storage manager to save the bucket to disk
			try {
				storageManager.createBucket(bucketName);
				if(WalrusProperties.trackUsageStatistics) 
					walrusStatistics.incrementBucketCount();
			} catch (IOException ex) {
				LOG.error(ex);
				db.rollback();
				throw new EucalyptusCloudException("Unable to create bucket: " + bucketName);
			}
		}
		db.commit();

		if(WalrusProperties.enableVirtualHosting) {
			UpdateARecordType updateARecord = new UpdateARecordType();
			updateARecord.setUserId(userId);
			URI walrusUri;
			String address = null;
			try {
				walrusUri = new URI(EucalyptusProperties.getWalrusUrl());
				address = walrusUri.getHost();
			} catch (URISyntaxException e) {
				throw new EucalyptusCloudException("Could not get Walrus URL");
			}
			String zone = WalrusProperties.WALRUS_SUBDOMAIN + ".";
			updateARecord.setAddress(address);
			updateARecord.setName(bucketName + "." + zone);
			updateARecord.setTtl(604800);
			updateARecord.setZone(zone);
			try {
				ServiceDispatcher.lookupSingle(Component.dns).send(updateARecord);
				LOG.info("Mapping " + updateARecord.getName() + " to " + address);
			} catch(Exception ex) {
				LOG.error("Could not update DNS record", ex);
			}
		}

		reply.setBucket(bucketName);
		return reply;
	}

	public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
		DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo searchBucket = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(searchBucket);

		if(bucketList.size() > 0) {
			BucketInfo bucketFound = bucketList.get(0);
			BucketLogData logData = bucketFound.getLoggingEnabled() ? request.getLogData() : null;
			if (bucketFound.canWrite(userId)) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObject = new ObjectInfo();
				searchObject.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.query(searchObject);
				if(objectInfos.size() == 0) {
					//asychronously flush any images in this bucket
					EntityWrapper<ImageCacheInfo> dbIC = db.recast(ImageCacheInfo.class);
					ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
					searchImageCacheInfo.setBucketName(bucketName);
					List<ImageCacheInfo> foundImageCacheInfos = dbIC.query(searchImageCacheInfo);

					if(foundImageCacheInfos.size() > 0) {
						ImageCacheInfo foundImageCacheInfo = foundImageCacheInfos.get(0);
						walrusImageManager.startImageCacheFlusher(bucketName, foundImageCacheInfo.getManifestName());
					}

					db.delete(bucketFound);
					//Actually remove the bucket from the backing store
					try {
						storageManager.deleteBucket(bucketName);
						if(WalrusProperties.trackUsageStatistics) 
							walrusStatistics.decrementBucketCount();
					} catch (IOException ex) {
						//set exception code in reply
						LOG.error(ex);
					}

					if(WalrusProperties.enableVirtualHosting) {
						RemoveARecordType removeARecordType = new RemoveARecordType();
						removeARecordType.setUserId(userId);
						String zone = WalrusProperties.WALRUS_SUBDOMAIN + ".";
						removeARecordType.setName(bucketName + "." + zone);
						removeARecordType.setZone(zone);
						try {
							ServiceDispatcher.lookupSingle(Component.dns).send(removeARecordType);
							LOG.info("Removing mapping for " + removeARecordType.getName());
						} catch(Exception ex) {
							LOG.error("Could not update DNS record", ex);
						}
					}

					Status status = new Status();
					status.setCode(204);
					status.setDescription("No Content");
					reply.setStatus(status);
					if(logData != null) {
						updateLogData(bucketFound, logData);
						reply.setLogData(logData);
					}
				} else {
					db.rollback();
					throw new BucketNotEmptyException(bucketName, logData);
				}
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request.getReply();

		String bucketName = request.getBucket();
		String userId = request.getUserId();
		String ownerId = null;

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		AccessControlListType accessControlList = new AccessControlListType();
		BucketLogData logData;

		if (bucketList.size() > 0) {
			//construct access control policy from grant infos
			BucketInfo bucket = bucketList.get(0);
			logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			List<GrantInfo> grantInfos = bucket.getGrants();
			if (bucket.canReadACP(userId)) {
				if(logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}				
				ownerId = bucket.getOwnerId();
				ArrayList<Grant> grants = new ArrayList<Grant>();
				bucket.readPermissions(grants);
				for (GrantInfo grantInfo: grantInfos) {
					String uId = grantInfo.getUserId();
					try {
						if(uId != null) {
							User grantUserInfo = CredentialProvider.getUser( uId );
							addPermission(grants, grantUserInfo, grantInfo);
						} else {
							addPermission(grants, grantInfo);
						}
					} catch ( NoSuchUserException e ) {
						db.rollback( );
						throw new AccessDeniedException("Bucket", bucketName, logData);
					}
				}
				accessControlList.setGrants(grants);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
		try {
			User ownerUserInfo = CredentialProvider.getUser( ownerId );
			accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo.getQueryId(), ownerUserInfo.getUserName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch ( NoSuchUserException e ) {
			db.rollback( );
			throw new AccessDeniedException("Bucket", bucketName, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}


	private static void addPermission(ArrayList<Grant>grants, User userInfo, GrantInfo grantInfo) {
		CanonicalUserType user = new CanonicalUserType(userInfo.getQueryId( ), userInfo.getUserName( ));

		if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.isWriteACP()) {
			grants.add(new Grant(new Grantee(user), "FULL_CONTROL"));
			return;
		}

		if (grantInfo.canRead()) {
			grants.add(new Grant(new Grantee(user), "READ"));
		}

		if (grantInfo.canWrite()) {
			grants.add(new Grant(new Grantee(user), "WRITE"));
		}

		if (grantInfo.canReadACP()) {
			grants.add(new Grant(new Grantee(user), "READ_ACP"));
		}

		if (grantInfo.isWriteACP()) {
			grants.add(new Grant(new Grantee(user), "WRITE_ACP"));
		}
	}

	private static void addPermission(ArrayList<Grant>grants, GrantInfo grantInfo) {
		if(grantInfo.getGrantGroup() != null) {
			Group group = new Group(grantInfo.getGrantGroup());

			if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.isWriteACP()) {
				grants.add(new Grant(new Grantee(group), "FULL_CONTROL"));
				return;
			}

			if (grantInfo.canRead()) {
				grants.add(new Grant(new Grantee(group), "READ"));
			}

			if (grantInfo.canWrite()) {
				grants.add(new Grant(new Grantee(group), "WRITE"));
			}

			if (grantInfo.canReadACP()) {
				grants.add(new Grant(new Grantee(group), "READ_ACP"));
			}

			if (grantInfo.isWriteACP()) {
				grants.add(new Grant(new Grantee(group), "WRITE_ACP"));
			}
		}
	}

	public PutObjectResponseType putObject(PutObjectType request) throws EucalyptusCloudException {
		PutObjectResponseType reply = (PutObjectResponseType) request.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		Long oldBucketSize = 0L;

		String md5 = "";
		Date lastModified = null;

		AccessControlListType accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		String key = bucketName + "." + objectKey;
		String randomKey = request.getRandomKey();
		WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (bucket.canWrite(userId)) {
				if(logData != null)
					reply.setLogData(logData);

				ObjectInfo foundObject = null;
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObject = new ObjectInfo();
				searchObject.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.query(searchObject);
				for (ObjectInfo objectInfo: objectInfos) {
					if (objectInfo.getObjectKey().equals(objectKey)) {
						//key (object) exists. check perms
						if (!objectInfo.canWrite(userId)) {
							db.rollback();
							messenger.removeQueue(key, randomKey);
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						foundObject = objectInfo;
						oldBucketSize = -foundObject.getSize();
						break;
					}
				}
				//write object to bucket
				String objectName;
				if (foundObject == null) {
					//not found. create an object info
					foundObject = new ObjectInfo(bucketName, objectKey);
					foundObject.setOwnerId(userId);
					List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
					foundObject.addGrants(userId, grantInfos, accessControlList);
					foundObject.setGrants(grantInfos);
					objectName = objectKey.replaceAll("/", "-") + Hashes.getRandom(4);
					foundObject.setObjectName(objectName);
					dbObject.add(foundObject);
				} else {
					//object already exists. see if we can modify acl
					if (foundObject.canWriteACP(userId)) {
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						foundObject.addGrants(userId, grantInfos, accessControlList);
						foundObject.setGrants(grantInfos);
					}
					objectName = foundObject.getObjectName();
					if(WalrusProperties.enableTorrents) {
						EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
						TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
						List<TorrentInfo> torrentInfos = dbTorrent.query(torrentInfo);
						if(torrentInfos.size() > 0) {
							TorrentInfo foundTorrentInfo = torrentInfos.get(0);
							TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
							if(torrentClient != null) {
								torrentClient.bye();
							}
							dbTorrent.delete(foundTorrentInfo);
						}
					} else {
						LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
					}
				}
				foundObject.setObjectKey(objectKey);
				foundObject.replaceMetaData(request.getMetaData());
				db.commit();
				//writes are unconditional
				LinkedBlockingQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

				try {
					WalrusDataMessage dataMessage;
					String tempObjectName = objectName;
					MessageDigest digest = null;
					long size = 0;
					FileIO fileIO = null;
					while ((dataMessage = putQueue.take())!=null) {
						if(WalrusDataMessage.isStart(dataMessage)) {
							tempObjectName = objectName + "." + Hashes.getRandom(12);
							digest = Hashes.Digest.MD5.get();
							try {
								fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
							} catch(Exception ex) {
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(ex);
							}
						} else if(WalrusDataMessage.isEOF(dataMessage)) {
							//commit object
							try {
								if(fileIO != null)
									fileIO.finish();
								storageManager.renameObject(bucketName, tempObjectName, objectName);
							} catch (IOException ex) {
								LOG.error(ex);
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(objectKey);
							}
							if(digest != null)
								md5 = Hashes.bytesToHex(digest.digest());
							lastModified = new Date();
							dbObject = WalrusControl.getEntityWrapper();
							objectInfos = dbObject.query(new ObjectInfo(bucketName, objectKey));
							if(objectInfos.size() > 0) {
								foundObject = objectInfos.get(0);
								foundObject.setEtag(md5);
								foundObject.setSize(size);
								foundObject.setLastModified(lastModified);
								foundObject.setStorageClass("STANDARD");
								foundObject.setContentType(request.getContentType());
								foundObject.setContentDisposition(request.getContentDisposition());
								reply.setSize(size);
								if(WalrusProperties.shouldEnforceUsageLimits && !request.isAdministrator()) {
									Long bucketSize = bucket.getBucketSize();
									long newSize = bucketSize + oldBucketSize + size;
									if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
										messenger.removeQueue(key, randomKey);
										dbObject.rollback();
										throw new EntityTooLargeException("Key", objectKey);
									}
									bucket.setBucketSize(newSize);
								}
								if(WalrusProperties.trackUsageStatistics) {
									walrusStatistics.updateBytesIn(size);
									walrusStatistics.updateSpaceUsed(size);
								}
								if(logData != null) {
									logData.setObjectSize(size);
									updateLogData(bucket, logData);
								}
								dbObject.commit();
								if(logData != null) {
									logData.setTurnAroundTime(Long.parseLong(new String(dataMessage.getPayload())));
								}
							} else {
								dbObject.rollback();
								throw new NoSuchEntityException("Could not find object: " + bucketName + "/" + objectKey, logData);
							}
							//restart all interrupted puts
							WalrusMonitor monitor = messenger.getMonitor(key);
							synchronized (monitor) {
								monitor.setLastModified(lastModified);
								monitor.setMd5(md5);
								monitor.notifyAll();
							}
							messenger.removeQueue(key, randomKey);
							messenger.removeMonitor(key);
							LOG.info("Transfer complete: " + key);
							break;

						} else if(WalrusDataMessage.isInterrupted(dataMessage)) {

							//there was a write after this one started
							//abort writing but wait until the other (last) writer has completed
							WalrusMonitor monitor = messenger.getMonitor(key);
							synchronized (monitor) {
								monitor.wait();
								lastModified = monitor.getLastModified();
								md5 = monitor.getMd5();
							}
							//ok we are done here
							if(fileIO != null)
								fileIO.finish();
							ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, -1L);
							objectDeleter.start();
							LOG.info("Transfer interrupted: "+ key);
							break;
						} else {
							assert(WalrusDataMessage.isData(dataMessage));
							byte[] data = dataMessage.getPayload();
							//start writing object (but do not commit yet)
							try {
								if(fileIO != null)
									fileIO.write(data);
							} catch (IOException ex) {
								LOG.error(ex);
							}
							//calculate md5 on the fly
							size += data.length;
							if(digest != null)
								digest.update(data);
						}
					}
				} catch (InterruptedException ex) {
					LOG.error(ex, ex);
					messenger.removeQueue(key, randomKey);
					throw new EucalyptusCloudException("Transfer interrupted: " + key + "." + randomKey);
				}
			} else {
				db.rollback();
				messenger.removeQueue(key, randomKey);
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		}   else {
			db.rollback();
			messenger.removeQueue(key, randomKey);
			throw new NoSuchBucketException(bucketName);
		}
		reply.setEtag(md5);
		reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		return reply;
	}

	public PostObjectResponseType postObject(PostObjectType request) throws EucalyptusCloudException {
		PostObjectResponseType reply = (PostObjectResponseType) request.getReply();

		String bucketName = request.getBucket();
		String key = request.getKey();

		PutObjectType putObject = new PutObjectType();
		putObject.setUserId(request.getUserId());
		putObject.setBucket(bucketName);
		putObject.setKey(key);
		putObject.setRandomKey(request.getRandomKey());
		putObject.setAccessControlList(request.getAccessControlList());
		putObject.setContentType(request.getContentType());
		putObject.setContentLength(request.getContentLength());
		putObject.setAccessKeyID(request.getAccessKeyID());
		putObject.setEffectiveUserId(request.getEffectiveUserId());
		putObject.setCredential(request.getCredential());
		putObject.setIsCompressed(request.getIsCompressed());
		putObject.setMetaData(request.getMetaData());
		putObject.setStorageClass(request.getStorageClass());

		PutObjectResponseType putObjectResponse = putObject(putObject);

		String etag = putObjectResponse.getEtag();
		reply.setEtag(etag);
		reply.setLastModified(putObjectResponse.getLastModified());
		reply.set_return(putObjectResponse.get_return());
		reply.setMetaData(putObjectResponse.getMetaData());
		reply.setErrorCode(putObjectResponse.getErrorCode());
		reply.setStatusMessage(putObjectResponse.getStatusMessage());
		reply.setLogData(putObjectResponse.getLogData());

		String successActionRedirect = request.getSuccessActionRedirect();
		if(successActionRedirect != null) {
			try {
				java.net.URI addrUri = new URL(successActionRedirect).toURI();
				InetAddress.getByName(addrUri.getHost());
			} catch(Exception ex) {
				LOG.warn(ex);
			}
			String paramString = "bucket=" + bucketName + "&key=" + key + "&etag=quot;" + etag + "quot;";
			reply.setRedirectUrl(successActionRedirect + "?" + paramString);
		} else {
			Integer successActionStatus = request.getSuccessActionStatus();
			if(successActionStatus != null) {
				if((successActionStatus == 200) || (successActionStatus == 201)) {
					reply.setSuccessCode(successActionStatus);
					if(successActionStatus == 200) {
						return reply;
					} else {
						reply.setBucket(bucketName);
						reply.setKey(key);
						reply.setLocation(EucalyptusProperties.getWalrusUrl() + 
								"/" + bucketName + "/" + key);
					}
				} else {
					reply.setSuccessCode(204);
					return reply;
				}
			}
		}
		return reply;
	}

	public PutObjectInlineResponseType putObjectInline(PutObjectInlineType request) throws EucalyptusCloudException {
		PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		String md5 = "";
		Long oldBucketSize = 0L;
		Date lastModified;

		AccessControlListType accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (bucket.canWrite(userId)) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);

				ObjectInfo foundObject = null;
				List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
				for (ObjectInfo objectInfo: objectInfos) {
					if (objectInfo.getObjectKey().equals(objectKey)) {
						//key (object) exists. check perms
						if (!objectInfo.canWrite(userId)) {
							db.rollback();
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						foundObject = objectInfo;
						oldBucketSize = -foundObject.getSize();
						break;
					}
				}
				//write object to bucket
				String objectName;
				if (foundObject == null) {
					//not found. create an object info
					foundObject = new ObjectInfo(bucketName, objectKey);
					foundObject.setOwnerId(userId);
					List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
					foundObject.addGrants(userId, grantInfos, accessControlList);
					foundObject.setGrants(grantInfos);
					objectName = objectKey.replaceAll("/", "-") + Hashes.getRandom(4);
					foundObject.setObjectName(objectName);
					dbObject.add(foundObject);
				} else {
					//object already exists. see if we can modify acl
					if (foundObject.canWriteACP(userId)) {
						List<GrantInfo> grantInfos = foundObject.getGrants();
						foundObject.addGrants(userId, grantInfos, accessControlList);
					}
					objectName = foundObject.getObjectName();
				}
				foundObject.setObjectKey(objectKey);
				try {
					//writes are unconditional
					byte[] base64Data = request.getBase64Data().getBytes();
					foundObject.setObjectName(objectName);
					try {
						FileIO fileIO = storageManager.prepareForWrite(bucketName, objectName);
						if(fileIO != null) {
							fileIO.write(base64Data);
							fileIO.finish();
						}
					} catch(Exception ex) {
						throw new EucalyptusCloudException(ex);
					}
					md5 = Hashes.getHexString(Hashes.Digest.MD5.get().digest(base64Data));
					foundObject.setEtag(md5);
					Long size = (long)base64Data.length;
					foundObject.setSize(size);
					if(WalrusProperties.shouldEnforceUsageLimits && !request.isAdministrator()) {
						Long bucketSize = bucket.getBucketSize();
						long newSize = bucketSize + oldBucketSize + size;
						if(newSize > WalrusProperties.MAX_BUCKET_SIZE) {
							db.rollback();
							throw new EntityTooLargeException("Key", objectKey, logData);
						}
						bucket.setBucketSize(newSize);
					}
					if(WalrusProperties.trackUsageStatistics) {
						walrusStatistics.updateBytesIn(size);
						walrusStatistics.updateSpaceUsed(size);
					}
					//Add meta data if specified
					if(request.getMetaData() != null)
						foundObject.replaceMetaData(request.getMetaData());

					//TODO: add support for other storage classes
					foundObject.setStorageClass("STANDARD");
					lastModified = new Date();
					foundObject.setLastModified(lastModified);
					if(logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(size);
						reply.setLogData(logData);
					}					
				} catch (Exception ex) {
					LOG.error(ex);
					db.rollback();
					throw new EucalyptusCloudException(bucketName);
				}
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		db.commit();

		reply.setEtag(md5);
		reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
		return reply;
	}

	public AddObjectResponseType addObject(AddObjectType request) throws EucalyptusCloudException {

		AddObjectResponseType reply = (AddObjectResponseType) request.getReply();
		String bucketName = request.getBucket();
		String key = request.getKey();
		String userId = request.getUserId();
		String objectName = request.getObjectName();

		AccessControlListType accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			if (bucket.canWrite(userId)) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
				for (ObjectInfo objectInfo: objectInfos) {
					if (objectInfo.getObjectKey().equals(key)) {
						//key (object) exists.
						db.rollback();
						throw new EucalyptusCloudException("object already exists " + key);
					}
				}
				//write object to bucket
				ObjectInfo objectInfo = new ObjectInfo(bucketName, key);
				objectInfo.setObjectName(objectName);
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.addGrants(userId, grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);
				dbObject.add(objectInfo);

				objectInfo.setObjectKey(key);
				objectInfo.setOwnerId(userId);
				objectInfo.setSize(storageManager.getSize(bucketName, objectName));
				objectInfo.setEtag(request.getEtag());
				objectInfo.setLastModified(new Date());
				objectInfo.setStorageClass("STANDARD");
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws EucalyptusCloudException {
		DeleteObjectResponseType reply = (DeleteObjectResponseType) request.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfos = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfos);

		if (bucketList.size() > 0) {
			BucketInfo bucketInfo = bucketList.get(0);
			BucketLogData logData = bucketInfo.getLoggingEnabled() ? request.getLogData() : null;
			ObjectInfo foundObject = null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0) {
				foundObject = objectInfos.get(0);
			}

			if (foundObject != null) {
				if (foundObject.canWrite(userId)) {
					dbObject.delete(foundObject);
					String objectName = foundObject.getObjectName();
					for (GrantInfo grantInfo: foundObject.getGrants()) {
						db.getEntityManager().remove(grantInfo);
					}
					Long size = foundObject.getSize();
					bucketInfo.setBucketSize(bucketInfo.getBucketSize() - size);
					ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, objectName, size);
					objectDeleter.start();
					reply.setCode("200");
					reply.setDescription("OK");
					if(logData != null) {
						updateLogData(bucketInfo, logData);
						reply.setLogData(logData);
					}					
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	private class ObjectDeleter extends Thread {
		String bucketName;
		String objectName;
		Long size;
		public ObjectDeleter(String bucketName, String objectName, Long size) {
			this.bucketName = bucketName;
			this.objectName = objectName;
			this.size = size;
		}

		public void run() {
			try {
				storageManager.deleteObject(bucketName, objectName);
				if(WalrusProperties.trackUsageStatistics && (size > 0))
					walrusStatistics.updateSpaceUsed(-size);
			} catch(IOException ex) {
				LOG.error(ex, ex);
			}
		}
	}

	public ListBucketResponseType listBucket(ListBucketType request) throws EucalyptusCloudException {
		ListBucketResponseType reply = (ListBucketResponseType) request.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();
		String prefix = request.getPrefix();
		if(prefix == null)
			prefix = "";

		String marker = request.getMarker();
		int maxKeys = -1;
		String maxKeysString = request.getMaxKeys();
		if(maxKeysString != null)
			maxKeys = Integer.parseInt(maxKeysString);
		else
			maxKeys = WalrusProperties.MAX_KEYS;

		String delimiter = request.getDelimiter();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		bucketInfo.setHidden(false);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		ArrayList<PrefixEntry> prefixes = new ArrayList<PrefixEntry>();

		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if(bucket.canRead(userId)) {
				if(logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}				
				if(request.isAdministrator()) {
					EntityWrapper<WalrusSnapshotInfo> dbSnap = db.recast(WalrusSnapshotInfo.class);
					WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
					walrusSnapInfo.setSnapshotBucket(bucketName);
					List<WalrusSnapshotInfo> walrusSnaps = dbSnap.query(walrusSnapInfo);
					if(walrusSnaps.size() > 0) {
						db.rollback();
						throw new NoSuchBucketException(bucketName);
					}
				}
				reply.setName(bucketName);
				reply.setIsTruncated(false);
				if(maxKeys >= 0)
					reply.setMaxKeys(maxKeys);
				reply.setPrefix(prefix);
				reply.setMarker(marker);
				if(delimiter != null)
					reply.setDelimiter(delimiter);
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
				if(objectInfos.size() > 0) {
					int howManyProcessed = 0;
					if(marker != null || objectInfos.size() < maxKeys)
						Collections.sort(objectInfos);
					ArrayList<ListEntry> contents = new ArrayList<ListEntry>();
					for(ObjectInfo objectInfo: objectInfos) {
						String objectKey = objectInfo.getObjectKey();
						if(marker != null) {
							if(objectKey.compareTo(marker) <= 0)
								continue;
						}
						if(prefix != null) {
							if(!objectKey.startsWith(prefix)) {
								continue;
							} else {
								if(delimiter != null) {
									String[] parts = objectKey.substring(prefix.length()).split(delimiter);
									if(parts.length > 1) {
										String prefixString = parts[0] + delimiter;
										boolean foundPrefix = false;
										for(PrefixEntry prefixEntry : prefixes) {
											if(prefixEntry.getPrefix().equals(prefixString)) {
												foundPrefix = true;
												break;
											}
										}
										if(!foundPrefix) {
											prefixes.add(new PrefixEntry(prefixString));
											if(maxKeys >= 0) {
												if(howManyProcessed++ >= maxKeys) {
													reply.setIsTruncated(true);
													break;
												}
											}
										}
										continue;
									}
								}
							}
						}
						if(maxKeys >= 0) {
							if(howManyProcessed++ >= maxKeys) {
								reply.setIsTruncated(true);
								break;
							}
						}
						ListEntry listEntry = new ListEntry();
						listEntry.setKey(objectKey);
						listEntry.setEtag(objectInfo.getEtag());
						listEntry.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
						listEntry.setStorageClass(objectInfo.getStorageClass());
						String displayName = objectInfo.getOwnerId();
						try {
							User userInfo = CredentialProvider.getUser( displayName );
							listEntry.setOwner(new CanonicalUserType(userInfo.getQueryId(), displayName));
						} catch ( NoSuchUserException e ) {
							db.rollback( );
							throw new AccessDeniedException("Bucket", bucketName, logData);
						}
						ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
						objectInfo.returnMetaData(metaData);
						reply.setMetaData(metaData);
						listEntry.setSize(objectInfo.getSize());
						listEntry.setStorageClass(objectInfo.getStorageClass());
						contents.add(listEntry);
					}
					reply.setContents(contents);
					if(prefix != null) {
						reply.setCommonPrefixes(prefixes);
					}
				}
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request.getReply();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();
		String ownerId = null;

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);
		BucketLogData logData;

		AccessControlListType accessControlList = new AccessControlListType();
		if (bucketList.size() > 0) {
			//construct access control policy from grant infos
			logData = bucketList.get(0).getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(objectInfo.canReadACP(userId)) {
					BucketInfo bucket = bucketList.get(0);
					if(logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(objectInfo.getSize());
						reply.setLogData(logData);
					}

					ownerId = objectInfo.getOwnerId();
					ArrayList<Grant> grants = new ArrayList<Grant>();
					List<GrantInfo> grantInfos = objectInfo.getGrants();
					for (GrantInfo grantInfo: grantInfos) {
						String uId = grantInfo.getUserId();
						try {
							User userInfo = CredentialProvider.getUser( uId );
							objectInfo.readPermissions(grants);
							addPermission(grants, userInfo, grantInfo);
						} catch ( NoSuchUserException e ) {
							throw new AccessDeniedException("Key", objectKey, logData);              
						}
					}
					accessControlList.setGrants(grants);
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
		try {
			User ownerUserInfo = CredentialProvider.getUser( ownerId );
			accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo.getQueryId(), ownerUserInfo.getUserName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch ( NoSuchUserException e ) {
			throw new AccessDeniedException("Key", objectKey, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}

	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		SetBucketAccessControlPolicyResponseType reply = (SetBucketAccessControlPolicyResponseType) request.getReply();
		String userId = request.getUserId();
		AccessControlListType accessControlList = request.getAccessControlList();
		String bucketName = request.getBucket();
		if(accessControlList == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (bucket.canWriteACP(userId)) {
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				bucket.resetGlobalGrants();
				bucket.addGrants(bucket.getOwnerId(), grantInfos, accessControlList);
				bucket.setGrants(grantInfos);
				reply.setCode("204");
				reply.setDescription("OK");
				if(logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}				
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public SetRESTBucketAccessControlPolicyResponseType setRESTBucketAccessControlPolicy(SetRESTBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		SetRESTBucketAccessControlPolicyResponseType reply = (SetRESTBucketAccessControlPolicyResponseType) request.getReply();
		String userId = request.getUserId();
		AccessControlPolicyType accessControlPolicy = request.getAccessControlPolicy();
		String bucketName = request.getBucket();
		if(accessControlPolicy == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}
		AccessControlListType accessControlList = accessControlPolicy.getAccessControlList();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (bucket.canWriteACP(userId)) {
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				bucket.resetGlobalGrants();
				bucket.addGrants(bucket.getOwnerId(), grantInfos, accessControlList);
				bucket.setGrants(grantInfos);
				reply.setCode("204");
				reply.setDescription("OK");
				if(logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}			
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}


	public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		SetObjectAccessControlPolicyResponseType reply = (SetObjectAccessControlPolicyResponseType) request.getReply();
		String userId = request.getUserId();
		AccessControlListType accessControlList = request.getAccessControlList();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (!objectInfo.canWriteACP(userId)) {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.resetGlobalGrants();
				objectInfo.addGrants(objectInfo.getOwnerId(), grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);

				if(WalrusProperties.enableTorrents) {
					if(!objectInfo.isGlobalRead()) {
						EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
						TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
						List<TorrentInfo> torrentInfos = dbTorrent.query(torrentInfo);
						if(torrentInfos.size() > 0) {
							TorrentInfo foundTorrentInfo = torrentInfos.get(0);
							TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
							if(torrentClient != null) {
								torrentClient.bye();
							}
							dbTorrent.delete(foundTorrentInfo);
						}
					}
				} else {
					LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
				}
				reply.setCode("204");
				reply.setDescription("OK");
				if(logData != null) {
					updateLogData(bucket, logData);
					logData.setObjectSize(objectInfo.getSize());
					reply.setLogData(logData);
				}				
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public SetRESTObjectAccessControlPolicyResponseType setRESTObjectAccessControlPolicy(SetRESTObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		SetRESTObjectAccessControlPolicyResponseType reply = (SetRESTObjectAccessControlPolicyResponseType) request.getReply();
		String userId = request.getUserId();
		AccessControlPolicyType accessControlPolicy = request.getAccessControlPolicy();
		if(accessControlPolicy == null) {
			throw new AccessDeniedException("Key", request.getKey());
		}
		AccessControlListType accessControlList = accessControlPolicy.getAccessControlList();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0)  {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (!objectInfo.canWriteACP(userId)) {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.resetGlobalGrants();
				objectInfo.addGrants(objectInfo.getOwnerId(), grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);

				if(WalrusProperties.enableTorrents) {
					if(!objectInfo.isGlobalRead()) {
						EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
						TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
						List<TorrentInfo> torrentInfos = dbTorrent.query(torrentInfo);
						if(torrentInfos.size() > 0) {
							TorrentInfo foundTorrentInfo = torrentInfos.get(0);
							TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
							if(torrentClient != null) {
								torrentClient.bye();
							}
							dbTorrent.delete(foundTorrentInfo);
						}
					}
				} else {
					LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
				}
				if(logData != null) {
					updateLogData(bucket, logData);
					logData.setObjectSize(objectInfo.getSize());
					reply.setLogData(logData);
				}				
				reply.setCode("204");
				reply.setDescription("OK");
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		}   else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public GetObjectResponseType getObject(GetObjectType request) throws EucalyptusCloudException {
		GetObjectResponseType reply = (GetObjectResponseType) request.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();
		Boolean deleteAfterGet = request.getDeleteAfterGet();
		if(deleteAfterGet == null)
			deleteAfterGet = false;

		Boolean getTorrent = request.getGetTorrent();
		if(getTorrent == null)
			getTorrent = false;

		Boolean getMetaData = request.getGetMetaData();
		if(getMetaData == null)
			getMetaData = false;

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if(objectInfo.canRead(userId)) {
					String objectName = objectInfo.getObjectName();
					DefaultHttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK ); 
					if(getMetaData) {
						List<MetaDataInfo> metaDataInfos = objectInfo.getMetaData();
						for(MetaDataInfo metaDataInfo : metaDataInfos) {
							httpResponse.addHeader(WalrusProperties.AMZ_META_HEADER_PREFIX + metaDataInfo.getName(), metaDataInfo.getValue());
						}
					}
					if(getTorrent) {
						if(objectInfo.isGlobalRead()) {
							if(!WalrusProperties.enableTorrents) {
								LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
								throw new EucalyptusCloudException("Torrents disabled");
							}
							EntityWrapper<TorrentInfo> dbTorrent = WalrusControl.getEntityWrapper();
							TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
							TorrentInfo foundTorrentInfo;
							String absoluteObjectPath = storageManager.getObjectPath(bucketName, objectName);
							try {
								foundTorrentInfo = dbTorrent.getUnique(torrentInfo);
							} catch (EucalyptusCloudException ex) {
								String torrentFile = objectName + ".torrent";
								String torrentFilePath = storageManager.getObjectPath(bucketName, torrentFile);
								TorrentCreator torrentCreator = new TorrentCreator(absoluteObjectPath, objectKey, objectName, torrentFilePath, WalrusProperties.getTrackerUrl());
								try {
									torrentCreator.create();
								} catch(Exception e) {
									LOG.error(e);
									throw new EucalyptusCloudException("could not create torrent file " + torrentFile);
								}
								torrentInfo.setTorrentFile(torrentFile);
								dbTorrent.add(torrentInfo);
								foundTorrentInfo = torrentInfo;
							}
							dbTorrent.commit();
							String torrentFile = foundTorrentInfo.getTorrentFile();
							String torrentFilePath = storageManager.getObjectPath(bucketName, torrentFile);
							TorrentClient torrentClient = new TorrentClient(torrentFilePath, absoluteObjectPath);
							Torrents.addClient(bucketName + objectKey, torrentClient);
							torrentClient.start();
							//send torrent
							String key = bucketName + "." + objectKey;
							String randomKey = key + "." + Hashes.getRandom(10);
							request.setRandomKey(randomKey);

							File torrent = new File(torrentFilePath);
							if(torrent.exists()) {
								Date lastModified = objectInfo.getLastModified();
								db.commit();
								long torrentLength = torrent.length();
								if(logData != null) {
									updateLogData(bucket, logData);
									logData.setObjectSize(torrentLength);
								}								
								storageManager.sendObject(request.getChannel(), httpResponse, bucketName, torrentFile, torrentLength, null, 
										DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z", 
										"application/x-bittorrent", "attachment; filename=" + objectKey + ".torrent;", request.getIsCompressed(),
										logData);
								if(WalrusProperties.trackUsageStatistics) {
									walrusStatistics.updateBytesOut(torrentLength);
								}
								return null;
							} else {
								db.rollback();
								String errorString = "Could not get torrent file " + torrentFilePath;
								LOG.error(errorString);
								throw new EucalyptusCloudException(errorString);
							}
						} else {
							db.rollback();
							throw new AccessDeniedException("Key", objectKey, logData);
						}
					}
					Date lastModified = objectInfo.getLastModified();
					Long size = objectInfo.getSize();
					String etag = objectInfo.getEtag();
					String contentType = objectInfo.getContentType();
					String contentDisposition = objectInfo.getContentDisposition();
					db.commit();
					if(logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(size);
					}								
					if(request.getGetData()) {
						if(request.getInlineData()) {
							try {
								byte[] bytes = new byte[102400/*TODO: NEIL WalrusQueryDispatcher.DATA_MESSAGE_SIZE*/];
								int bytesRead = 0;
								String base64Data = "";
								while((bytesRead = storageManager.readObject(bucketName, objectName, bytes, bytesRead)) > 0) {
									base64Data += new String(bytes, 0, bytesRead);
								}
								reply.setBase64Data(base64Data);
							} catch (IOException ex) {
								db.rollback();
								LOG.error(ex);
								throw new EucalyptusCloudException("Unable to read object: " + bucketName + "/" + objectName);
							}
						} else {
							//support for large objects
							if(WalrusProperties.trackUsageStatistics) {
								walrusStatistics.updateBytesOut(objectInfo.getSize());
							}
							storageManager.sendObject(request.getChannel(), httpResponse, bucketName, objectName, size, etag, 
									DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z", 
									contentType, contentDisposition, request.getIsCompressed(), logData);  
							return null;
						}
					} else {
						storageManager.sendHeaders(request.getChannel(), httpResponse, size, etag, 
								DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z", 
								contentType, contentDisposition, logData);
						return null;

					}
					reply.setEtag(etag);
					reply.setLastModified(DateUtils.format(lastModified, DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");
					reply.setSize(size);
					reply.setContentType(contentType);
					reply.setContentDisposition(contentDisposition);
					Status status = new Status();
					status.setCode(200);
					status.setDescription("OK");
					reply.setStatus(status);
					return reply;
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
	}

	public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
		GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request.getReply();
		Long byteRangeStart = request.getByteRangeStart();
		if(byteRangeStart == null) {
			byteRangeStart = 0L;
		}
		Long byteRangeEnd = request.getByteRangeEnd();
		if(byteRangeEnd == null) {
			byteRangeEnd = -1L;
		}
		Date ifModifiedSince = request.getIfModifiedSince();
		Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
		String ifMatch = request.getIfMatch();
		String ifNoneMatch = request.getIfNoneMatch();
		boolean returnCompleteObjectOnFailure = request.getReturnCompleteObjectOnConditionFailure();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();
		Status status = new Status();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);


		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);

				if(objectInfo.canRead(userId)) {
					String etag = objectInfo.getEtag();
					String objectName = objectInfo.getObjectName();
					if(byteRangeEnd == -1)
						byteRangeEnd = objectInfo.getSize();
					if((byteRangeStart > objectInfo.getSize()) || 
							(byteRangeStart > byteRangeEnd) ||
							(byteRangeEnd > objectInfo.getSize()) ||
							(byteRangeStart < 0 || byteRangeEnd < 0)) {
						throw new InvalidRangeException("Range: " + byteRangeStart + "-" + byteRangeEnd + "object: " + bucketName + "/" + objectKey);
					}
					DefaultHttpResponse httpResponse = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK ); 
					if(ifMatch != null) {
						if(!ifMatch.equals(etag) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new PreconditionFailedException(objectKey + " etag: " + etag);
						}

					}
					if(ifNoneMatch != null) {
						if(ifNoneMatch.equals(etag) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new NotModifiedException(objectKey + " ETag: " + etag);
						}
					}
					Date lastModified = objectInfo.getLastModified();
					if(ifModifiedSince != null) {
						if((ifModifiedSince.getTime() >= lastModified.getTime()) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new NotModifiedException(objectKey + " LastModified: " + lastModified.toString());
						}
					}
					if(ifUnmodifiedSince != null) {
						if((ifUnmodifiedSince.getTime() < lastModified.getTime()) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new PreconditionFailedException(objectKey + " lastModified: " + lastModified.toString());
						}
					}
					if(request.getGetMetaData()) {
						List<MetaDataInfo> metaDataInfos = objectInfo.getMetaData();
						for(MetaDataInfo metaDataInfo : metaDataInfos) {
							httpResponse.addHeader(WalrusProperties.AMZ_META_HEADER_PREFIX + metaDataInfo.getName(), metaDataInfo.getValue());
						}
					}
					Long size = objectInfo.getSize();
					String contentType = objectInfo.getContentType();
					String contentDisposition = objectInfo.getContentDisposition();
					db.commit();
					if(logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(size);
					}										
					if(request.getGetData()) {
						if(WalrusProperties.trackUsageStatistics) {
							walrusStatistics.updateBytesOut(size);
						}
						storageManager.sendObject(request.getChannel(), httpResponse, bucketName, objectName, byteRangeStart, byteRangeEnd, size, etag, 
								DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN + ".000Z"), 
								contentType, contentDisposition, request.getIsCompressed(), logData);  
						return null;
					} else {
						storageManager.sendHeaders(request.getChannel(), httpResponse, size, etag, 
								DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN + ".000Z"), 
								contentType, contentDisposition, logData);
						return null;
					}
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
	}

	public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request) throws EucalyptusCloudException {
		GetBucketLocationResponseType reply = (GetBucketLocationResponseType) request.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if(bucket.canRead(userId)) {
				if(logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}				
				String location = bucket.getLocation();
				if(location == null) {
					location = "NotSupported";
				}
				reply.setLocationConstraint(location);
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public CopyObjectResponseType copyObject(CopyObjectType request) throws EucalyptusCloudException {
		CopyObjectResponseType reply = (CopyObjectResponseType) request.getReply();
		String userId = request.getUserId();
		String sourceBucket = request.getSourceBucket();
		String sourceKey = request.getSourceObject();
		String destinationBucket = request.getDestinationBucket();
		String destinationKey = request.getDestinationObject();
		String metadataDirective = request.getMetadataDirective();
		AccessControlListType accessControlList = request.getAccessControlList();

		String copyIfMatch = request.getCopySourceIfMatch();
		String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
		Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
		Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();

		if(metadataDirective == null)
			metadataDirective = "COPY";
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(sourceBucket);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(sourceBucket, sourceKey);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if(objectInfos.size() > 0) {
				ObjectInfo sourceObjectInfo = objectInfos.get(0);
				if(sourceObjectInfo.canRead(userId)) {
					if(copyIfMatch != null) {
						if(!copyIfMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfMatch: " + copyIfMatch);
						}
					}
					if(copyIfNoneMatch != null) {
						if(copyIfNoneMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfNoneMatch: " + copyIfNoneMatch);
						}
					}
					if(copyIfUnmodifiedSince != null) {
						long unmodifiedTime = copyIfUnmodifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified().getTime();
						if(unmodifiedTime < objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfUnmodifiedSince: " + copyIfUnmodifiedSince.toString());
						}
					}
					if(copyIfModifiedSince != null) {
						long modifiedTime = copyIfModifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified().getTime();
						if(modifiedTime > objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfModifiedSince: " + copyIfModifiedSince.toString());
						}
					}
					BucketInfo destinationBucketInfo = new BucketInfo(destinationBucket);
					List<BucketInfo> destinationBuckets = db.query(destinationBucketInfo);
					if(destinationBuckets.size() > 0) {
						BucketInfo foundDestinationBucketInfo = destinationBuckets.get(0);
						if(foundDestinationBucketInfo.canWrite(userId)) {
							//all ok
							ObjectInfo destinationObjectInfo = null;
							String destinationObjectName;
							ObjectInfo destSearchObjectInfo = new ObjectInfo(destinationBucket, destinationKey);
							List<ObjectInfo> destinationObjectInfos = dbObject.query(destSearchObjectInfo);
							if(destinationObjectInfos.size() > 0) {
								destinationObjectInfo = destinationObjectInfos.get(0);
								if(!destinationObjectInfo.canWrite(userId)) {
									db.rollback();
									throw new AccessDeniedException("Key", destinationKey);
								}
							}

							if(destinationObjectInfo == null) {
								//not found. create a new one
								destinationObjectInfo = new ObjectInfo();
								List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
								destinationObjectInfo.setBucketName(destinationBucket);
								destinationObjectInfo.setObjectKey(destinationKey);
								destinationObjectInfo.addGrants(userId, grantInfos, accessControlList);
								destinationObjectInfo.setGrants(grantInfos);
								destinationObjectInfo.setObjectName(destinationKey.replaceAll("/", "-") + Hashes.getRandom(4));
								dbObject.add(destinationObjectInfo);
							} else {
								if (destinationObjectInfo.canWriteACP(userId)) {
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
									destinationObjectInfo.addGrants(userId, grantInfos, accessControlList);
									destinationObjectInfo.setGrants(grantInfos);
								}
							}
							destinationObjectInfo.setSize(sourceObjectInfo.getSize());
							destinationObjectInfo.setStorageClass(sourceObjectInfo.getStorageClass());
							destinationObjectInfo.setOwnerId(sourceObjectInfo.getOwnerId());
							destinationObjectInfo.setContentType(sourceObjectInfo.getContentType());
							destinationObjectInfo.setContentDisposition(sourceObjectInfo.getContentDisposition());
							String etag = sourceObjectInfo.getEtag();
							Date lastModified = sourceObjectInfo.getLastModified();
							destinationObjectInfo.setEtag(etag);
							destinationObjectInfo.setLastModified(lastModified);
							if(!metadataDirective.equals("REPLACE")) {
								destinationObjectInfo.setMetaData(sourceObjectInfo.cloneMetaData());
							} else {
								List<MetaDataEntry> metaData = request.getMetaData();
								if(metaData != null)
									destinationObjectInfo.replaceMetaData(metaData);
							}

							String sourceObjectName = sourceObjectInfo.getObjectName();
							destinationObjectName = destinationObjectInfo.getObjectName();

							try {
								storageManager.copyObject(sourceBucket, sourceObjectName, destinationBucket, destinationObjectName);
								if(WalrusProperties.trackUsageStatistics)
									walrusStatistics.updateSpaceUsed(sourceObjectInfo.getSize());
							} catch(Exception ex) {
								LOG.error(ex);
								db.rollback();
								throw new EucalyptusCloudException("Could not rename " + sourceObjectName + " to " + destinationObjectName);
							}
							reply.setEtag(etag);
							reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ISO8601_DATETIME_PATTERN) + ".000Z");

							db.commit();
							return reply;
						} else {
							db.rollback();
							throw new AccessDeniedException("Bucket", destinationBucket);
						}
					} else {
						db.rollback();
						throw new NoSuchBucketException(destinationBucket);
					}
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", sourceKey);
				}
			} else {
				db.rollback();
				throw new NoSuchEntityException(sourceKey);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(sourceBucket);
		}
	}

	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(SetBucketLoggingStatusType request) throws EucalyptusCloudException {
		SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request.getReply();
		String bucket = request.getBucket();
		String targetBucket = request.getLoggingEnabled().getTargetBucket();
		String targetPrefix = request.getLoggingEnabled().getTargetPrefix();
		List<Grant> targetGrantsList = null;
		TargetGrants targetGrants = request.getLoggingEnabled().getTargetGrants();
		if(targetGrants != null)
			targetGrantsList = targetGrants.getGrants();
		if(targetPrefix == null)
			targetPrefix = "";

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo, targetBucketInfo;
		try {
			bucketInfo = db.getUnique(new BucketInfo(bucket));
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		} 
		try {
			targetBucketInfo = db.getUnique(new BucketInfo(targetBucket));
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		} 
		if(!targetBucketInfo.hasLoggingPerms()) {
			db.rollback();
			throw new InvalidTargetBucketForLoggingException(targetBucket); 
		}
		bucketInfo.setTargetBucket(targetBucket);
		bucketInfo.setTargetPrefix(targetPrefix);
		bucketInfo.setLoggingEnabled(true);
		if(targetGrantsList != null) {
			targetBucketInfo.addGrants(targetGrantsList);
		}
		db.commit();		
		return reply;
	}

	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(GetBucketLoggingStatusType request) throws EucalyptusCloudException {
		GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request.getReply();
		String bucket = request.getBucket();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		try {
			BucketInfo bucketInfo = db.getUnique(new BucketInfo(bucket));
			if(bucketInfo.getLoggingEnabled()) {
				String targetBucket = bucketInfo.getTargetBucket();
				ArrayList<Grant> grants = new ArrayList<Grant>();
				try {
					BucketInfo targetBucketInfo = db.getUnique(new BucketInfo(targetBucket));
					List<GrantInfo> grantInfos = targetBucketInfo.getGrants();
					for (GrantInfo grantInfo: grantInfos) {
						String uId = grantInfo.getUserId();
						try {
							if(uId != null) {
								User grantUserInfo = CredentialProvider.getUser( uId );
								addPermission(grants, grantUserInfo, grantInfo);
							} else {
								addPermission(grants, grantInfo);
							}
						} catch ( NoSuchUserException e ) {
							db.rollback( );
							throw new AccessDeniedException("Bucket", targetBucket);
						}
					}
				} catch(EucalyptusCloudException ex) {
					db.rollback();
					throw new InvalidTargetBucketForLoggingException(targetBucket);
				}
				reply.getLoggingEnabled().setTargetBucket(bucketInfo.getTargetBucket());
				reply.getLoggingEnabled().setTargetPrefix(bucketInfo.getTargetPrefix());

				TargetGrants targetGrants = new TargetGrants();
				targetGrants.setGrants(grants);
				reply.getLoggingEnabled().setTargetGrants(targetGrants);
			}
		} catch(EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		} 
		db.commit();		
		return reply;
	}

	private void updateLogData(BucketInfo bucket, BucketLogData logData) {
		logData.setOwnerId(bucket.getOwnerId());
		logData.setTargetBucket(bucket.getTargetBucket());
		logData.setTargetPrefix(bucket.getTargetPrefix());
	}
}
