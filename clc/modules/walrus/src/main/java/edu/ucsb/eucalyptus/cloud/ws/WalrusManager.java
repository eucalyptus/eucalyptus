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
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.eucalyptus.ws.handlers.WalrusRESTBinding;

import edu.ucsb.eucalyptus.cloud.AccessDeniedException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyExistsException;
import edu.ucsb.eucalyptus.cloud.BucketAlreadyOwnedByYouException;
import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.cloud.BucketNotEmptyException;
import edu.ucsb.eucalyptus.cloud.EntityTooLargeException;
import edu.ucsb.eucalyptus.cloud.InlineDataTooLargeException;
import edu.ucsb.eucalyptus.cloud.InvalidBucketNameException;
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
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.TorrentInfo;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
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
import edu.ucsb.eucalyptus.msgs.DeleteMarkerEntry;
import edu.ucsb.eucalyptus.msgs.DeleteObjectResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteObjectType;
import edu.ucsb.eucalyptus.msgs.DeleteVersionResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteVersionType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketLocationType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketLoggingStatusType;
import edu.ucsb.eucalyptus.msgs.GetBucketVersioningStatusResponseType;
import edu.ucsb.eucalyptus.msgs.GetBucketVersioningStatusType;
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
import edu.ucsb.eucalyptus.msgs.ListVersionsResponseType;
import edu.ucsb.eucalyptus.msgs.ListVersionsType;
import edu.ucsb.eucalyptus.msgs.LoggingEnabled;
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
import edu.ucsb.eucalyptus.msgs.SetBucketVersioningStatusResponseType;
import edu.ucsb.eucalyptus.msgs.SetBucketVersioningStatusType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTBucketAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyResponseType;
import edu.ucsb.eucalyptus.msgs.SetRESTObjectAccessControlPolicyType;
import edu.ucsb.eucalyptus.msgs.Status;
import edu.ucsb.eucalyptus.msgs.TargetGrants;
import edu.ucsb.eucalyptus.msgs.UpdateARecordType;
import edu.ucsb.eucalyptus.msgs.VersionEntry;
import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusDataMessenger;
import edu.ucsb.eucalyptus.util.WalrusDataQueue;
import edu.ucsb.eucalyptus.util.WalrusMonitor;

public class WalrusManager {
	private static Logger LOG = Logger.getLogger(WalrusManager.class);

	private StorageManager storageManager;
	private WalrusImageManager walrusImageManager;
	private static WalrusStatistics walrusStatistics = null;

	public static void configure() {
		walrusStatistics = new WalrusStatistics();
	}

	public WalrusManager(StorageManager storageManager,
			WalrusImageManager walrusImageManager) {
		this.storageManager = storageManager;
		this.walrusImageManager = walrusImageManager;
	}

	public void initialize() throws EucalyptusCloudException {
		check();
	}

	public void check() throws EucalyptusCloudException {
		File bukkitDir = new File(WalrusInfo.getWalrusInfo().getStorageDir());
		if (!bukkitDir.exists()) {
			if (!bukkitDir.mkdirs()) {
				LOG.fatal("Unable to make bucket root directory: "
						+ WalrusInfo.getWalrusInfo().getStorageDir());
				throw new EucalyptusCloudException(
				"Invalid bucket root directory");
			}
		} else if (!bukkitDir.canWrite()) {
			LOG.fatal("Cannot write to bucket root directory: "
					+ WalrusInfo.getWalrusInfo().getStorageDir());
			throw new EucalyptusCloudException("Invalid bucket root directory");
		}
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo();
		List<BucketInfo> bucketInfos = db.query(bucketInfo);
		for (BucketInfo bucket : bucketInfos) {
			if (!storageManager.bucketExists(bucket.getBucketName()))
				bucket.setHidden(true);
			else
				bucket.setHidden(false);
		}
		db.commit();
	}

	public ListAllMyBucketsResponseType listAllMyBuckets(
			ListAllMyBucketsType request) throws EucalyptusCloudException {
		ListAllMyBucketsResponseType reply = (ListAllMyBucketsResponseType) request
		.getReply();
		String userId = request.getUserId();

		if (userId == null) {
			throw new AccessDeniedException("no such user");
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo searchBucket = new BucketInfo();
		searchBucket.setOwnerId(userId);
		searchBucket.setHidden(false);
		List<BucketInfo> bucketInfoList = db.query(searchBucket);

		ArrayList<BucketListEntry> buckets = new ArrayList<BucketListEntry>();

		for (BucketInfo bucketInfo : bucketInfoList) {
			if (request.isAdministrator()) {
				EntityWrapper<WalrusSnapshotInfo> dbSnap = db
				.recast(WalrusSnapshotInfo.class);
				WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
				walrusSnapInfo.setSnapshotBucket(bucketInfo.getBucketName());
				List<WalrusSnapshotInfo> walrusSnaps = dbSnap
				.query(walrusSnapInfo);
				if (walrusSnaps.size() > 0)
					continue;
			}
			buckets.add(new BucketListEntry(bucketInfo.getBucketName(),
					DateUtils.format(bucketInfo.getCreationDate().getTime(),
							DateUtils.ISO8601_DATETIME_PATTERN)
							+ ".000Z"));
		}
		try {
			CanonicalUserType owner = new CanonicalUserType(Users
					.lookupUser(userId).getQueryId(), userId);
			ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
			reply.setOwner(owner);
			bucketList.setBuckets(buckets);
			reply.setBucketList(bucketList);
		} catch (Exception ex) {
			db.rollback();
			LOG.error(ex);
			throw new AccessDeniedException("User: " + userId + " not found");
		}
		db.commit();
		return reply;
	}

	public CreateBucketResponseType createBucket(CreateBucketType request)
	throws EucalyptusCloudException {
		CreateBucketResponseType reply = (CreateBucketResponseType) request
		.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String locationConstraint = request.getLocationConstraint();

		if (userId == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}

		AccessControlListType accessControlList = request
		.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		if(!checkBucketName(bucketName))
			throw new InvalidBucketNameException(bucketName);

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();

		if (WalrusProperties.shouldEnforceUsageLimits
				&& !request.isAdministrator()) {
			BucketInfo searchBucket = new BucketInfo();
			searchBucket.setOwnerId(userId);
			List<BucketInfo> bucketList = db.query(searchBucket);
			if (bucketList.size() >= WalrusInfo.getWalrusInfo().getStorageMaxBucketsPerUser()) {
				db.rollback();
				throw new TooManyBucketsException(bucketName);
			}
		}

		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			if (bucketList.get(0).getOwnerId().equals(userId)) {
				// bucket already exists and you created it
				db.rollback();
				throw new BucketAlreadyOwnedByYouException(bucketName);
			}
			// bucket already exists
			db.rollback();
			throw new BucketAlreadyExistsException(bucketName);
		} else {
			// create bucket and set its acl
			BucketInfo bucket = new BucketInfo(userId, bucketName, new Date());
			ArrayList<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
			bucket.addGrants(userId, grantInfos, accessControlList);
			bucket.setGrants(grantInfos);
			bucket.setBucketSize(0L);
			bucket.setLoggingEnabled(false);
			bucket.setVersioning(WalrusProperties.VersioningStatus.Disabled
					.toString());
			bucket.setHidden(false);
			if (locationConstraint != null)
				bucket.setLocation(locationConstraint);
			else
				bucket.setLocation("US");
			db.add(bucket);
			// call the storage manager to save the bucket to disk
			try {
				storageManager.createBucket(bucketName);
				if (WalrusProperties.trackUsageStatistics)
					walrusStatistics.incrementBucketCount();
			} catch (IOException ex) {
				LOG.error(ex);
				db.rollback();
				throw new EucalyptusCloudException("Unable to create bucket: "
						+ bucketName);
			}
		}
		db.commit();

		if(WalrusProperties.enableVirtualHosting) {
			if(checkDNSNaming(bucketName)) {
				UpdateARecordType updateARecord = new UpdateARecordType();
				updateARecord.setUserId(userId);
				URI walrusUri;
				String address = null;
				try {
					walrusUri = new URI(SystemConfiguration.getWalrusUrl());
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
			} else {
				LOG.error("Bucket: " + bucketName + " fails to meet DNS requirements. Unable to create DNS mapping.");
			}
		}

		reply.setBucket(bucketName);
		return reply;
	}

	private boolean checkBucketName(String bucketName) {
		if(!(bucketName.matches("^[A-Za-z0-9].*") || bucketName.contains(".") || 
				bucketName.contains("-")))
			return false;
		if(bucketName.length() < 3 || bucketName.length() > 255)
			return false;
		String[] addrParts = bucketName.split("\\.");
		boolean ipFormat = true;
		if(addrParts.length == 4) {
			for(String addrPart : addrParts) {
				try {
					Integer.parseInt(addrPart);
				} catch(NumberFormatException ex) {
					ipFormat = false;
					break;
				}
			}
		} else {
			ipFormat = false;
		}		
		if(ipFormat)
			return false;
		return true;
	}

	private boolean checkDNSNaming(String bucketName) {
		if(bucketName.contains("_"))
			return false;
		if(bucketName.length() < 3 || bucketName.length() > 63)
			return false;
		if(bucketName.endsWith("-"))
			return false;
		if(bucketName.contains("-." ) || bucketName.contains(".-"))
			return false;
		return true;
	}

	public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
		DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo searchBucket = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(searchBucket);

		if (bucketList.size() > 0) {
			BucketInfo bucketFound = bucketList.get(0);
			BucketLogData logData = bucketFound.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucketFound.canWrite(userId)) {
						EntityWrapper<ObjectInfo> dbObject = db
						.recast(ObjectInfo.class);
						ObjectInfo searchObject = new ObjectInfo();
						searchObject.setBucketName(bucketName);
						searchObject.setDeleted(false);
						List<ObjectInfo> objectInfos = dbObject.query(searchObject);
						if (objectInfos.size() == 0) {
							//check if the bucket contains any images
							EntityWrapper<ImageCacheInfo> dbIC = db
							.recast(ImageCacheInfo.class);
							ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
							searchImageCacheInfo.setBucketName(bucketName);
							List<ImageCacheInfo> foundImageCacheInfos = dbIC
							.query(searchImageCacheInfo);

							if (foundImageCacheInfos.size() > 0) {
								db.rollback();
								throw new BucketNotEmptyException(bucketName, logData);
							}
							//remove any delete markers
							ObjectInfo searchDeleteMarker = new ObjectInfo();
							searchDeleteMarker.setBucketName(bucketName);
							searchDeleteMarker.setDeleted(true);
							List<ObjectInfo> deleteMarkers = dbObject.query(searchDeleteMarker);
							for(ObjectInfo deleteMarker : deleteMarkers) {
								dbObject.delete(deleteMarker);
							}
							db.delete(bucketFound);
							// Actually remove the bucket from the backing store
							try {
								storageManager.deleteBucket(bucketName);
								if (WalrusProperties.trackUsageStatistics)
									walrusStatistics.decrementBucketCount();
							} catch (IOException ex) {
								// set exception code in reply
								LOG.error(ex);
							}

							if (WalrusProperties.enableVirtualHosting) {
								URI walrusUri;
								String address;
								RemoveARecordType removeARecordType = new RemoveARecordType();
								removeARecordType.setUserId(userId);
								String zone = WalrusProperties.WALRUS_SUBDOMAIN + ".";
								removeARecordType.setName(bucketName + "." + zone);
								removeARecordType.setZone(zone);
								try {
									walrusUri = new URI(SystemConfiguration.getWalrusUrl());
									address = walrusUri.getHost();
								} catch (URISyntaxException e) {
									db.rollback();
									throw new EucalyptusCloudException("Could not get Walrus URL");
								}
								removeARecordType.setAddress(address);
								try {
									ServiceDispatcher.lookupSingle(Component.dns).send(
											removeARecordType);
									LOG.info("Removing mapping for "
											+ removeARecordType.getName());
								} catch (Exception ex) {
									LOG.error("Could not update DNS record", ex);
								}
							}

							Status status = new Status();
							status.setCode(204);
							status.setDescription("No Content");
							reply.setStatus(status);
							if (logData != null) {
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

	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
	throws EucalyptusCloudException {
		GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request
		.getReply();

		String bucketName = request.getBucket();
		String userId = request.getUserId();
		String ownerId = null;

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		AccessControlListType accessControlList = new AccessControlListType();
		BucketLogData logData;

		if (bucketList.size() > 0) {
			// construct access control policy from grant infos
			BucketInfo bucket = bucketList.get(0);
			logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			List<GrantInfo> grantInfos = bucket.getGrants();
			if (bucket.canReadACP(userId)) {
				if (logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}
				ownerId = bucket.getOwnerId();
				ArrayList<Grant> grants = new ArrayList<Grant>();
				bucket.readPermissions(grants);
				for (GrantInfo grantInfo : grantInfos) {
					String uId = grantInfo.getUserId();
					try {
						if (uId != null) {
							User grantUserInfo = Users.lookupUser(uId);
							addPermission(grants, grantUserInfo, grantInfo);
						} else {
							addPermission(grants, grantInfo);
						}
					} catch (NoSuchUserException e) {
						db.rollback();
						throw new AccessDeniedException("Bucket", bucketName,
								logData);
					}
				}
				accessControlList.setGrants(grants);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
		try {
			User ownerUserInfo = Users.lookupUser(ownerId);
			accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo
					.getQueryId(), ownerUserInfo.getName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch (NoSuchUserException e) {
			db.rollback();
			throw new AccessDeniedException("Bucket", bucketName, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}

	private static void addPermission(ArrayList<Grant> grants, User userInfo,
			GrantInfo grantInfo) {
		CanonicalUserType user = new CanonicalUserType(userInfo.getQueryId(),
				userInfo.getName());

		if (grantInfo.canRead() && grantInfo.canWrite()
				&& grantInfo.canReadACP() && grantInfo.isWriteACP()) {
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

	private static void addPermission(ArrayList<Grant> grants,
			GrantInfo grantInfo) {
		if (grantInfo.getGrantGroup() != null) {
			Group group = new Group(grantInfo.getGrantGroup());

			if (grantInfo.canRead() && grantInfo.canWrite()
					&& grantInfo.canReadACP() && grantInfo.isWriteACP()) {
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

	public PutObjectResponseType putObject(PutObjectType request)
	throws EucalyptusCloudException {
		PutObjectResponseType reply = (PutObjectResponseType) request
		.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		Long oldBucketSize = 0L;

		String md5 = "";
		Date lastModified = null;

		AccessControlListType accessControlList = request
		.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		String key = bucketName + "." + objectKey;
		String randomKey = request.getRandomKey();
		WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canWrite(userId)) {
						if (logData != null)
							reply.setLogData(logData);
						String objectName;
						String versionId;
						ObjectInfo objectInfo = null;
						if (bucket.isVersioningEnabled()) {
							objectInfo = new ObjectInfo(bucketName, objectKey);
							objectInfo.setOwnerId(userId);
							List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
							objectInfo.addGrants(userId, grantInfos, accessControlList);
							objectInfo.setGrants(grantInfos);
							objectName = UUID.randomUUID().toString();
							objectInfo.setObjectName(objectName);
							objectInfo.setSize(0L);
							versionId = UUID.randomUUID().toString().replaceAll("-", "");
						} else {
							versionId = WalrusProperties.NULL_VERSION_ID;
							ObjectInfo searchObject = new ObjectInfo(bucketName, objectKey);
							searchObject.setVersionId(versionId);							
							EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
							try {
								ObjectInfo foundObject = dbObject.getUnique(searchObject);
								if (!foundObject.canWrite(userId)) {
									db.rollback();
									messenger.removeQueue(key, randomKey);
									throw new AccessDeniedException("Key", objectKey,
											logData);
								} 
								objectName = foundObject.getObjectName();
							} catch(AccessDeniedException ex) { 
								throw ex;
							} catch(EucalyptusCloudException ex) {
								objectInfo = new ObjectInfo(bucketName, objectKey);
								objectInfo.setOwnerId(userId);
								List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
								objectInfo.addGrants(userId, grantInfos, accessControlList);
								objectInfo.setGrants(grantInfos);
								objectName =  UUID.randomUUID().toString();
								objectInfo.setObjectName(objectName);
								objectInfo.setSize(0L);
							}
						}

						db.commit();
						// writes are unconditional
						WalrusDataQueue<WalrusDataMessage> putQueue = messenger
						.getQueue(key, randomKey);

						try {
							WalrusDataMessage dataMessage;
							String tempObjectName = objectName;
							MessageDigest digest = null;
							long size = 0;
							FileIO fileIO = null;
							while ((dataMessage = putQueue.take()) != null) {
								if(putQueue.getInterrupted()) {                                         
									if(WalrusDataMessage.isEOF(dataMessage)) {
										WalrusMonitor monitor = messenger.getMonitor(key);
										if(monitor.getLastModified() == null) {
											synchronized (monitor) {
												monitor.wait();
											}
										}
										lastModified = monitor.getLastModified();
										md5 = monitor.getMd5();
										//ok we are done here
										if(fileIO != null)
											fileIO.finish();
										ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, -1L);
										objectDeleter.start();
										LOG.info("Transfer interrupted: "+ key);
										messenger.removeQueue(key, randomKey);
										break;  
									}
									continue;
								}
								if (WalrusDataMessage.isStart(dataMessage)) {
									tempObjectName = UUID.randomUUID().toString();
									digest = Digest.MD5.get();
									try {
										fileIO = storageManager.prepareForWrite(
												bucketName, tempObjectName);
									} catch (Exception ex) {
										messenger.removeQueue(key, randomKey);
										throw new EucalyptusCloudException(ex);
									}
								} else if (WalrusDataMessage.isEOF(dataMessage)) {
									// commit object
									try {
										if (fileIO != null)
											fileIO.finish();
										storageManager.renameObject(bucketName,
												tempObjectName, objectName);
									} catch (IOException ex) {
										LOG.error(ex);
										messenger.removeQueue(key, randomKey);
										throw new EucalyptusCloudException(objectKey);
									}
									if (digest != null)
										md5 = Hashes.bytesToHex(digest.digest());
									lastModified = new Date();
									ObjectInfo searchObject = new ObjectInfo(bucketName, objectKey);
									searchObject.setVersionId(versionId);
									EntityWrapper<ObjectInfo> dbObject = WalrusControl.getEntityWrapper();
									List<ObjectInfo> objectInfos = dbObject.query(new ObjectInfo(bucketName, objectKey));
									for(ObjectInfo objInfo : objectInfos) {
										objInfo.setLast(false);
									}
									ObjectInfo foundObject;
									try {
										foundObject = dbObject.getUnique(searchObject);
										if (foundObject.canWriteACP(userId)) {
											List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
											foundObject.addGrants(userId, grantInfos,
													accessControlList);
											foundObject.setGrants(grantInfos);
										}
										if (WalrusProperties.enableTorrents) {
											EntityWrapper<TorrentInfo> dbTorrent = dbObject
											.recast(TorrentInfo.class);
											TorrentInfo torrentInfo = new TorrentInfo(bucketName,
													objectKey);
											List<TorrentInfo> torrentInfos = dbTorrent
											.query(torrentInfo);
											if (torrentInfos.size() > 0) {
												TorrentInfo foundTorrentInfo = torrentInfos.get(0);
												TorrentClient torrentClient = Torrents
												.getClient(bucketName + objectKey);
												if (torrentClient != null) {
													torrentClient.bye();
												}
												dbTorrent.delete(foundTorrentInfo);
											}
										} else {
											LOG
											.warn("Bittorrent support has been disabled. Please check pre-requisites");
										}
									} catch (EucalyptusCloudException ex) {
										if(objectInfo != null) {
											foundObject = objectInfo;
										} else {
											db.rollback();
											throw new EucalyptusCloudException("Unable to update object: " + bucketName + "/" + objectKey);
										}
									}
									foundObject.setVersionId(versionId);
									foundObject.replaceMetaData(request.getMetaData());
									foundObject.setEtag(md5);
									foundObject.setSize(size);
									foundObject.setLastModified(lastModified);
									foundObject.setStorageClass("STANDARD");
									foundObject.setContentType(request
											.getContentType());
									foundObject.setContentDisposition(request
											.getContentDisposition());
									foundObject.setLast(true);
									foundObject.setDeleted(false);
									reply.setSize(size);
									ObjectInfo deleteMarker = new ObjectInfo(bucketName, objectKey);
									deleteMarker.setDeleted(true);
									try {
										ObjectInfo foundDeleteMarker = dbObject.getUnique(deleteMarker);
										dbObject.delete(foundDeleteMarker);
									} catch(EucalyptusCloudException ex) {
										//no delete marker found.
										LOG.trace("No delete marker found for: " + bucketName + "/" + objectKey);
									}
									if (bucket.isVersioningEnabled()) {
										reply.setVersionId(versionId);
									}
									EntityWrapper<BucketInfo> dbBucket = dbObject.recast(BucketInfo.class);										
									try {
										bucket = dbBucket.getUnique(new BucketInfo(bucketName));
									} catch(EucalyptusCloudException e) {
										LOG.error(e);
										dbObject.rollback();
										throw new NoSuchBucketException(bucketName);
									}
									Long bucketSize = bucket.getBucketSize();
									long newSize = bucketSize + oldBucketSize
									+ size;
									if (WalrusProperties.shouldEnforceUsageLimits
											&& !request.isAdministrator()) {
										if (newSize > (WalrusInfo.getWalrusInfo().getStorageMaxBucketSizeInMB() * WalrusProperties.M)) {
											messenger.removeQueue(key, randomKey);
											dbObject.rollback();
											throw new EntityTooLargeException(
													"Key", objectKey);
										}
									}
									bucket.setBucketSize(newSize);
									if (WalrusProperties.trackUsageStatistics) {
										walrusStatistics.updateBytesIn(size);
										walrusStatistics.updateSpaceUsed(size);
									}
									if (logData != null) {
										logData.setObjectSize(size);
										updateLogData(bucket, logData);
									}
									if(objectInfo != null)
										dbObject.add(foundObject);
									dbObject.commit();
									if (logData != null) {
										logData.setTurnAroundTime(Long
												.parseLong(new String(dataMessage
														.getPayload())));
									}
									// restart all interrupted puts
									WalrusMonitor monitor = messenger.getMonitor(key);
									synchronized (monitor) {
										monitor.setLastModified(lastModified);
										monitor.setMd5(md5);
										monitor.notifyAll();
									}
									messenger.removeMonitor(key);
									messenger.removeQueue(key, randomKey);
									LOG.info("Transfer complete: " + key);
									break;
								} else {
									assert (WalrusDataMessage.isData(dataMessage));
									byte[] data = dataMessage.getPayload();
									// start writing object (but do not commit yet)
									try {
										if (fileIO != null)
											fileIO.write(data);
									} catch (IOException ex) {
										LOG.error(ex);
									}
									// calculate md5 on the fly
									size += data.length;
									if (digest != null)
										digest.update(data);
								}
							}
						} catch (InterruptedException ex) {
							LOG.error(ex, ex);
							messenger.removeQueue(key, randomKey);
							throw new EucalyptusCloudException("Transfer interrupted: "
									+ key + "." + randomKey);
						}
					} else {
						db.rollback();
						messenger.removeQueue(key, randomKey);
						throw new AccessDeniedException("Bucket", bucketName, logData);
					}
		} else {
			db.rollback();
			messenger.removeQueue(key, randomKey);
			throw new NoSuchBucketException(bucketName);
		}
		reply.setEtag(md5);
		reply.setLastModified(DateUtils.format(lastModified.getTime(),
				DateUtils.ISO8601_DATETIME_PATTERN)
				+ ".000Z");
		return reply;
	}

	public PostObjectResponseType postObject(PostObjectType request)
	throws EucalyptusCloudException {
		PostObjectResponseType reply = (PostObjectResponseType) request
		.getReply();

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
		if (successActionRedirect != null) {
			try {
				java.net.URI addrUri = new URL(successActionRedirect).toURI();
				InetAddress.getByName(addrUri.getHost());
			} catch (Exception ex) {
				LOG.warn(ex);
			}
			String paramString = "bucket=" + bucketName + "&key=" + key
			+ "&etag=quot;" + etag + "quot;";
			reply.setRedirectUrl(successActionRedirect + "?" + paramString);
		} else {
			Integer successActionStatus = request.getSuccessActionStatus();
			if (successActionStatus != null) {
				if ((successActionStatus == 200)
						|| (successActionStatus == 201)) {
					reply.setSuccessCode(successActionStatus);
					if (successActionStatus == 200) {
						return reply;
					} else {
						reply.setBucket(bucketName);
						reply.setKey(key);
						reply.setLocation(SystemConfiguration.getWalrusUrl()
								+ "/" + bucketName + "/" + key);
					}
				} else {
					reply.setSuccessCode(204);
					return reply;
				}
			}
		}
		return reply;
	}

	public PutObjectInlineResponseType putObjectInline(
			PutObjectInlineType request) throws EucalyptusCloudException {
		PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request
		.getReply();
		String userId = request.getUserId();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		String md5 = "";
		Long oldBucketSize = 0L;
		Date lastModified;

		AccessControlListType accessControlList = request
		.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canWrite(userId)) {
						EntityWrapper<ObjectInfo> dbObject = db
						.recast(ObjectInfo.class);
						ObjectInfo searchObjectInfo = new ObjectInfo();
						searchObjectInfo.setBucketName(bucketName);

						ObjectInfo foundObject = null;
						List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
						for (ObjectInfo objectInfo : objectInfos) {
							if (objectInfo.getObjectKey().equals(objectKey)) {
								// key (object) exists. check perms
								if (!objectInfo.canWrite(userId)) {
									db.rollback();
									throw new AccessDeniedException("Key", objectKey,
											logData);
								}
								foundObject = objectInfo;
								oldBucketSize = -foundObject.getSize();
								break;
							}
						}
						// write object to bucket
						String objectName;
						if (foundObject == null) {
							// not found. create an object info
							foundObject = new ObjectInfo(bucketName, objectKey);
							foundObject.setOwnerId(userId);
							List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
							foundObject
							.addGrants(userId, grantInfos, accessControlList);
							foundObject.setGrants(grantInfos);
							objectName = UUID.randomUUID().toString();
							foundObject.setObjectName(objectName);
							dbObject.add(foundObject);
						} else {
							// object already exists. see if we can modify acl
							if (foundObject.canWriteACP(userId)) {
								List<GrantInfo> grantInfos = foundObject.getGrants();
								foundObject.addGrants(userId, grantInfos,
										accessControlList);
							}
							objectName = foundObject.getObjectName();
						}
						foundObject.setObjectKey(objectKey);
						try {
							// writes are unconditional
							if (request.getBase64Data().getBytes().length > WalrusProperties.MAX_INLINE_DATA_SIZE) {
								db.rollback();
								throw new InlineDataTooLargeException(bucketName + "/"
										+ objectKey);
							}
							byte[] base64Data = Hashes.base64decode(
									request.getBase64Data()).getBytes();
							foundObject.setObjectName(objectName);
							try {
								FileIO fileIO = storageManager.prepareForWrite(
										bucketName, objectName);
								if (fileIO != null) {
									fileIO.write(base64Data);
									fileIO.finish();
								}
							} catch (Exception ex) {
								db.rollback();
								throw new EucalyptusCloudException(ex);
							}
							md5 = Hashes.getHexString(Digest.MD5.get().digest(
									base64Data));
							foundObject.setEtag(md5);
							Long size = (long) base64Data.length;
							foundObject.setSize(size);
							if (WalrusProperties.shouldEnforceUsageLimits
									&& !request.isAdministrator()) {
								Long bucketSize = bucket.getBucketSize();
								long newSize = bucketSize + oldBucketSize + size;
								if (newSize > (WalrusInfo.getWalrusInfo().getStorageMaxBucketSizeInMB() * WalrusProperties.M)) {
									db.rollback();
									throw new EntityTooLargeException("Key", objectKey,
											logData);
								}
								bucket.setBucketSize(newSize);
							}
							if (WalrusProperties.trackUsageStatistics) {
								walrusStatistics.updateBytesIn(size);
								walrusStatistics.updateSpaceUsed(size);
							}
							// Add meta data if specified
							if (request.getMetaData() != null)
								foundObject.replaceMetaData(request.getMetaData());

							// TODO: add support for other storage classes
							foundObject.setStorageClass("STANDARD");
							lastModified = new Date();
							foundObject.setLastModified(lastModified);
							if (logData != null) {
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
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		db.commit();

		reply.setEtag(md5);
		reply.setLastModified(DateUtils.format(lastModified.getTime(),
				DateUtils.ISO8601_DATETIME_PATTERN)
				+ ".000Z");
		return reply;
	}

	public AddObjectResponseType addObject(AddObjectType request)
	throws EucalyptusCloudException {

		AddObjectResponseType reply = (AddObjectResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String key = request.getKey();
		String userId = request.getUserId();
		String objectName = request.getObjectName();

		AccessControlListType accessControlList = request
		.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlListType();
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			if (bucket.canWrite(userId)) {
				EntityWrapper<ObjectInfo> dbObject = db
				.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
				for (ObjectInfo objectInfo : objectInfos) {
					if (objectInfo.getObjectKey().equals(key)) {
						// key (object) exists.
						db.rollback();
						throw new EucalyptusCloudException(
								"object already exists " + key);
					}
				}
				// write object to bucket
				ObjectInfo objectInfo = new ObjectInfo(bucketName, key);
				objectInfo.setObjectName(objectName);
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.addGrants(userId, grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);
				dbObject.add(objectInfo);

				objectInfo.setObjectKey(key);
				objectInfo.setOwnerId(userId);
				objectInfo.setSize(storageManager.getSize(bucketName,
						objectName));
				objectInfo.setEtag(request.getEtag());
				objectInfo.setLastModified(new Date());
				objectInfo.setStorageClass("STANDARD");
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public DeleteObjectResponseType deleteObject(DeleteObjectType request)
	throws EucalyptusCloudException {
		DeleteObjectResponseType reply = (DeleteObjectResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfos = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfos);

		if (bucketList.size() > 0) {
			BucketInfo bucketInfo = bucketList.get(0);
			BucketLogData logData = bucketInfo.getLoggingEnabled() ? request
					.getLogData() : null;

					if(bucketInfo.isVersioningEnabled()) {
						EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
						ObjectInfo searchDeletedObjectInfo = new ObjectInfo(bucketName, objectKey);
						searchDeletedObjectInfo.setDeleted(true);
						try {
							dbObject.getUnique(searchDeletedObjectInfo);
							db.rollback();
							throw new NoSuchEntityException(objectKey, logData);
						} catch(NoSuchEntityException ex) {
							throw ex;
						} catch(EucalyptusCloudException ex) {
							ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
							searchObjectInfo.setLast(true);
							List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
							for(ObjectInfo objInfo : objectInfos) {
								objInfo.setLast(false);
							}
							ObjectInfo deleteMarker = new ObjectInfo(bucketName,
									objectKey);
							deleteMarker.setDeleted(true);
							deleteMarker.setLast(true);
							deleteMarker.setOwnerId(userId);
							deleteMarker.setLastModified(new Date());
							deleteMarker.setVersionId(UUID.randomUUID().toString().replaceAll("-", ""));
							dbObject.add(deleteMarker);
							reply.setCode("200");
							reply.setDescription("OK");
						}
					} else {
						//versioning disabled or suspended.
						ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
						searchObjectInfo.setVersionId(WalrusProperties.NULL_VERSION_ID);
						EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
						List<ObjectInfo>objectInfos = dbObject.query(searchObjectInfo);
						if (objectInfos.size() > 0) {
							ObjectInfo nullObject = objectInfos.get(0);
							if(nullObject.canWrite(userId)) {
								dbObject.delete(nullObject);
								String objectName = nullObject.getObjectName();
								for (GrantInfo grantInfo : nullObject
										.getGrants()) {
									db.getEntityManager().remove(grantInfo);
								}
								Long size = nullObject.getSize();
								bucketInfo.setBucketSize(bucketInfo
										.getBucketSize()
										- size);
								ObjectDeleter objectDeleter = new ObjectDeleter(
										bucketName, objectName, size);
								objectDeleter.start();
								reply.setCode("200");
								reply.setDescription("OK");
								if (logData != null) {
									updateLogData(bucketInfo, logData);
									reply.setLogData(logData);
								}
								if(bucketInfo.isVersioningSuspended()) {
									//add delete marker
									ObjectInfo deleteMarker = new ObjectInfo(bucketName,
											objectKey);
									deleteMarker.setDeleted(true);
									deleteMarker.setLast(true);
									deleteMarker.setOwnerId(userId);
									deleteMarker.setLastModified(new Date());
									deleteMarker.setVersionId(UUID.randomUUID().toString().replaceAll("-", ""));
									dbObject.add(deleteMarker);
								}
							} else {
								db.rollback();
								throw new AccessDeniedException("Key", objectKey, logData);
							}
						} else {
							db.rollback();
							throw new NoSuchEntityException(objectKey, logData);
						}
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
				if (WalrusProperties.trackUsageStatistics && (size > 0))
					walrusStatistics.updateSpaceUsed(-size);
			} catch (IOException ex) {
				LOG.error(ex, ex);
			}
		}
	}

	public ListBucketResponseType listBucket(ListBucketType request)
	throws EucalyptusCloudException {
		ListBucketResponseType reply = (ListBucketResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();
		String prefix = request.getPrefix();
		if (prefix == null)
			prefix = "";

		String marker = request.getMarker();
		int maxKeys = -1;
		String maxKeysString = request.getMaxKeys();
		if (maxKeysString != null)
			maxKeys = Integer.parseInt(maxKeysString);
		else
			maxKeys = WalrusProperties.MAX_KEYS;

		String delimiter = request.getDelimiter();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		bucketInfo.setHidden(false);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		ArrayList<PrefixEntry> prefixes = new ArrayList<PrefixEntry>();

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canRead(userId)) {
						if (logData != null) {
							updateLogData(bucket, logData);
							reply.setLogData(logData);
						}
						if (request.isAdministrator()) {
							EntityWrapper<WalrusSnapshotInfo> dbSnap = db
							.recast(WalrusSnapshotInfo.class);
							WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
							walrusSnapInfo.setSnapshotBucket(bucketName);
							List<WalrusSnapshotInfo> walrusSnaps = dbSnap
							.query(walrusSnapInfo);
							if (walrusSnaps.size() > 0) {
								db.rollback();
								throw new NoSuchBucketException(bucketName);
							}
						}
						reply.setName(bucketName);
						reply.setIsTruncated(false);
						if (maxKeys >= 0)
							reply.setMaxKeys(maxKeys);
						reply.setPrefix(prefix);
						reply.setMarker(marker);
						if (delimiter != null)
							reply.setDelimiter(delimiter);
						EntityWrapper<ObjectInfo> dbObject = db
						.recast(ObjectInfo.class);
						ObjectInfo searchObjectInfo = new ObjectInfo();
						searchObjectInfo.setBucketName(bucketName);
						searchObjectInfo.setDeleted(false);
						searchObjectInfo.setLast(true);
						List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
						if (objectInfos.size() > 0) {
							int howManyProcessed = 0;
							if (marker != null || objectInfos.size() < maxKeys)
								Collections.sort(objectInfos);
							ArrayList<ListEntry> contents = new ArrayList<ListEntry>();
							for (ObjectInfo objectInfo : objectInfos) {
								String objectKey = objectInfo.getObjectKey();
								if (marker != null) {
									if (objectKey.compareTo(marker) <= 0)
										continue;
								}
								if (prefix != null) {
									if (!objectKey.startsWith(prefix)) {
										continue;
									} else {
										if (delimiter != null) {
											String[] parts = objectKey.substring(
													prefix.length()).split(delimiter);
											if (parts.length > 1) {
												String prefixString = parts[0]
												                            + delimiter;
												boolean foundPrefix = false;
												for (PrefixEntry prefixEntry : prefixes) {
													if (prefixEntry.getPrefix().equals(
															prefixString)) {
														foundPrefix = true;
														break;
													}
												}
												if (!foundPrefix) {
													prefixes.add(new PrefixEntry(
															prefixString));
													if (maxKeys >= 0) {
														if (howManyProcessed++ >= maxKeys) {
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
								if (maxKeys >= 0) {
									if (howManyProcessed++ >= maxKeys) {
										reply.setIsTruncated(true);
										break;
									}
								}
								ListEntry listEntry = new ListEntry();
								listEntry.setKey(objectKey);
								listEntry.setEtag(objectInfo.getEtag());
								listEntry.setLastModified(DateUtils.format(objectInfo
										.getLastModified().getTime(),
										DateUtils.ISO8601_DATETIME_PATTERN)
										+ ".000Z");
								listEntry.setStorageClass(objectInfo.getStorageClass());
								String displayName = objectInfo.getOwnerId();
								try {
									User userInfo = Users.lookupUser(displayName);
									listEntry.setOwner(new CanonicalUserType(userInfo
											.getQueryId(), displayName));
								} catch (NoSuchUserException e) {
									db.rollback();
									throw new AccessDeniedException("Bucket",
											bucketName, logData);
								}
								ArrayList<MetaDataEntry> metaData = new ArrayList<MetaDataEntry>();
								objectInfo.returnMetaData(metaData);
								reply.setMetaData(metaData);
								listEntry.setSize(objectInfo.getSize());
								listEntry.setStorageClass(objectInfo.getStorageClass());
								contents.add(listEntry);
							}
							reply.setContents(contents);
							if (prefixes.size() > 0) {
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

	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
			GetObjectAccessControlPolicyType request)
	throws EucalyptusCloudException {
		GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request
		.getReply();

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
			// construct access control policy from grant infos
			BucketInfo bucket = bucketList.get(0);
			logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					if(bucket.isVersioningEnabled()) {
						if(request.getVersionId() == null)
							searchObjectInfo.setLast(true);
					}
					String versionId = request.getVersionId() != null ? request.getVersionId() : WalrusProperties.NULL_VERSION_ID;
					searchObjectInfo.setVersionId(versionId);
					searchObjectInfo.setDeleted(false);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);
						if (objectInfo.canReadACP(userId)) {
							if (logData != null) {
								updateLogData(bucket, logData);
								logData.setObjectSize(objectInfo.getSize());
								reply.setLogData(logData);
							}

							ownerId = objectInfo.getOwnerId();
							ArrayList<Grant> grants = new ArrayList<Grant>();
							List<GrantInfo> grantInfos = objectInfo.getGrants();
							for (GrantInfo grantInfo : grantInfos) {
								String uId = grantInfo.getUserId();
								try {
									User userInfo = Users.lookupUser(uId);
									objectInfo.readPermissions(grants);
									addPermission(grants, userInfo, grantInfo);
								} catch (NoSuchUserException e) {
									throw new AccessDeniedException("Key", objectKey,
											logData);
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
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		AccessControlPolicyType accessControlPolicy = new AccessControlPolicyType();
		try {
			User ownerUserInfo = Users.lookupUser(ownerId);
			accessControlPolicy.setOwner(new CanonicalUserType(ownerUserInfo
					.getQueryId(), ownerUserInfo.getName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch (NoSuchUserException e) {
			throw new AccessDeniedException("Key", objectKey, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}

	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
			SetBucketAccessControlPolicyType request)
	throws EucalyptusCloudException {
		SetBucketAccessControlPolicyResponseType reply = (SetBucketAccessControlPolicyResponseType) request
		.getReply();
		String userId = request.getUserId();
		AccessControlListType accessControlList = request
		.getAccessControlList();
		String bucketName = request.getBucket();
		if (accessControlList == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canWriteACP(userId)) {
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						bucket.resetGlobalGrants();
						bucket.addGrants(bucket.getOwnerId(), grantInfos,
								accessControlList);
						bucket.setGrants(grantInfos);
						reply.setCode("204");
						reply.setDescription("OK");
						if (logData != null) {
							updateLogData(bucket, logData);
							reply.setLogData(logData);
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

	public SetRESTBucketAccessControlPolicyResponseType setRESTBucketAccessControlPolicy(
			SetRESTBucketAccessControlPolicyType request)
	throws EucalyptusCloudException {
		SetRESTBucketAccessControlPolicyResponseType reply = (SetRESTBucketAccessControlPolicyResponseType) request
		.getReply();
		String userId = request.getUserId();
		AccessControlPolicyType accessControlPolicy = request
		.getAccessControlPolicy();
		String bucketName = request.getBucket();
		if (accessControlPolicy == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}
		AccessControlListType accessControlList = accessControlPolicy
		.getAccessControlList();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canWriteACP(userId)) {
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						bucket.resetGlobalGrants();
						bucket.addGrants(bucket.getOwnerId(), grantInfos,
								accessControlList);
						bucket.setGrants(grantInfos);
						reply.setCode("204");
						reply.setDescription("OK");
						if (logData != null) {
							updateLogData(bucket, logData);
							reply.setLogData(logData);
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

	public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
			SetObjectAccessControlPolicyType request)
	throws EucalyptusCloudException {
		SetObjectAccessControlPolicyResponseType reply = (SetObjectAccessControlPolicyResponseType) request
		.getReply();
		String userId = request.getUserId();
		AccessControlListType accessControlList = request
		.getAccessControlList();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					if(bucket.isVersioningEnabled()) {
						if(request.getVersionId() == null)
							searchObjectInfo.setLast(true);
					}
					String versionId = request.getVersionId() != null ? request.getVersionId() : WalrusProperties.NULL_VERSION_ID;
					searchObjectInfo.setVersionId(versionId);
					searchObjectInfo.setDeleted(false);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);
						if (!objectInfo.canWriteACP(userId)) {
							db.rollback();
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						objectInfo.resetGlobalGrants();
						objectInfo.addGrants(objectInfo.getOwnerId(), grantInfos,
								accessControlList);
						objectInfo.setGrants(grantInfos);

						if (WalrusProperties.enableTorrents) {
							if (!objectInfo.isGlobalRead()) {
								EntityWrapper<TorrentInfo> dbTorrent = db
								.recast(TorrentInfo.class);
								TorrentInfo torrentInfo = new TorrentInfo(bucketName,
										objectKey);
								List<TorrentInfo> torrentInfos = dbTorrent
								.query(torrentInfo);
								if (torrentInfos.size() > 0) {
									TorrentInfo foundTorrentInfo = torrentInfos.get(0);
									TorrentClient torrentClient = Torrents
									.getClient(bucketName + objectKey);
									if (torrentClient != null) {
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
						if (logData != null) {
							updateLogData(bucket, logData);
							logData.setObjectSize(objectInfo.getSize());
							reply.setLogData(logData);
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

	public SetRESTObjectAccessControlPolicyResponseType setRESTObjectAccessControlPolicy(
			SetRESTObjectAccessControlPolicyType request)
	throws EucalyptusCloudException {
		SetRESTObjectAccessControlPolicyResponseType reply = (SetRESTObjectAccessControlPolicyResponseType) request
		.getReply();
		String userId = request.getUserId();
		AccessControlPolicyType accessControlPolicy = request
		.getAccessControlPolicy();
		if (accessControlPolicy == null) {
			throw new AccessDeniedException("Key", request.getKey());
		}
		AccessControlListType accessControlList = accessControlPolicy
		.getAccessControlList();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					if(bucket.isVersioningEnabled()) {
						if(request.getVersionId() == null)
							searchObjectInfo.setLast(true);
					}
					String versionId = request.getVersionId() != null ? request.getVersionId() : WalrusProperties.NULL_VERSION_ID;
					searchObjectInfo.setVersionId(versionId);
					searchObjectInfo.setDeleted(false);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);
						if (!objectInfo.canWriteACP(userId)) {
							db.rollback();
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						objectInfo.resetGlobalGrants();
						objectInfo.addGrants(objectInfo.getOwnerId(), grantInfos,
								accessControlList);
						objectInfo.setGrants(grantInfos);

						if (WalrusProperties.enableTorrents) {
							if (!objectInfo.isGlobalRead()) {
								EntityWrapper<TorrentInfo> dbTorrent = db
								.recast(TorrentInfo.class);
								TorrentInfo torrentInfo = new TorrentInfo(bucketName,
										objectKey);
								List<TorrentInfo> torrentInfos = dbTorrent
								.query(torrentInfo);
								if (torrentInfos.size() > 0) {
									TorrentInfo foundTorrentInfo = torrentInfos.get(0);
									TorrentClient torrentClient = Torrents
									.getClient(bucketName + objectKey);
									if (torrentClient != null) {
										torrentClient.bye();
									}
									dbTorrent.delete(foundTorrentInfo);
								}
							}
						} else {
							LOG
							.warn("Bittorrent support has been disabled. Please check pre-requisites");
						}
						if (logData != null) {
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
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
		return reply;
	}

	public GetObjectResponseType getObject(GetObjectType request)
	throws EucalyptusCloudException {
		GetObjectResponseType reply = (GetObjectResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();
		Boolean deleteAfterGet = request.getDeleteAfterGet();
		if (deleteAfterGet == null)
			deleteAfterGet = false;

		Boolean getTorrent = request.getGetTorrent();
		if (getTorrent == null)
			getTorrent = false;

		Boolean getMetaData = request.getGetMetaData();
		if (getMetaData == null)
			getMetaData = false;

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					boolean versioning = false;
					if (bucket.isVersioningEnabled())
						versioning = true;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					searchObjectInfo.setVersionId(request.getVersionId());
					searchObjectInfo.setDeleted(false);
					if(request.getVersionId() == null)
						searchObjectInfo.setLast(true);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);
						if (objectInfo.canRead(userId)) {
							String objectName = objectInfo.getObjectName();
							DefaultHttpResponse httpResponse = new DefaultHttpResponse(
									HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
							if (getMetaData) {
								List<MetaDataInfo> metaDataInfos = objectInfo
								.getMetaData();
								for (MetaDataInfo metaDataInfo : metaDataInfos) {
									httpResponse.addHeader(
											WalrusProperties.AMZ_META_HEADER_PREFIX
											+ metaDataInfo.getName(),
											metaDataInfo.getValue());
								}
							}
							if (getTorrent) {
								if (objectInfo.isGlobalRead()) {
									if (!WalrusProperties.enableTorrents) {
										LOG
										.warn("Bittorrent support has been disabled. Please check pre-requisites");
										throw new EucalyptusCloudException(
										"Torrents disabled");
									}
									EntityWrapper<TorrentInfo> dbTorrent = WalrusControl
									.getEntityWrapper();
									TorrentInfo torrentInfo = new TorrentInfo(
											bucketName, objectKey);
									TorrentInfo foundTorrentInfo;
									String absoluteObjectPath = storageManager
									.getObjectPath(bucketName, objectName);
									try {
										foundTorrentInfo = dbTorrent
										.getUnique(torrentInfo);
									} catch (EucalyptusCloudException ex) {
										String torrentFile = objectName + ".torrent";
										String torrentFilePath = storageManager
										.getObjectPath(bucketName, torrentFile);
										TorrentCreator torrentCreator = new TorrentCreator(
												absoluteObjectPath, objectKey,
												objectName, torrentFilePath,
												WalrusProperties.getTrackerUrl());
										try {
											torrentCreator.create();
										} catch (Exception e) {
											LOG.error(e);
											throw new EucalyptusCloudException(
													"could not create torrent file "
													+ torrentFile);
										}
										torrentInfo.setTorrentFile(torrentFile);
										dbTorrent.add(torrentInfo);
										foundTorrentInfo = torrentInfo;
									}
									dbTorrent.commit();
									String torrentFile = foundTorrentInfo
									.getTorrentFile();
									String torrentFilePath = storageManager
									.getObjectPath(bucketName, torrentFile);
									TorrentClient torrentClient = new TorrentClient(
											torrentFilePath, absoluteObjectPath);
									Torrents.addClient(bucketName + objectKey,
											torrentClient);
									torrentClient.start();
									// send torrent
									String key = bucketName + "." + objectKey;
									String randomKey = key + "." + Hashes.getRandom(10);
									request.setRandomKey(randomKey);

									File torrent = new File(torrentFilePath);
									if (torrent.exists()) {
										Date lastModified = objectInfo
										.getLastModified();
										db.commit();
										long torrentLength = torrent.length();
										if (logData != null) {
											updateLogData(bucket, logData);
											logData.setObjectSize(torrentLength);
										}
										storageManager
										.sendObject(
												request,
												httpResponse,
												bucketName,
												torrentFile,
												torrentLength,
												null,
												DateUtils
												.format(
														lastModified
														.getTime(),
														DateUtils.ISO8601_DATETIME_PATTERN)
														+ ".000Z",
														"application/x-bittorrent",
														"attachment; filename="
														+ objectKey
														+ ".torrent;", request
														.getIsCompressed(),
														null, logData);
										if (WalrusProperties.trackUsageStatistics) {
											walrusStatistics
											.updateBytesOut(torrentLength);
										}
										return null;
									} else {
										db.rollback();
										String errorString = "Could not get torrent file "
											+ torrentFilePath;
										LOG.error(errorString);
										throw new EucalyptusCloudException(errorString);
									}
								} else {
									db.rollback();
									throw new AccessDeniedException("Key", objectKey,
											logData);
								}
							}
							Date lastModified = objectInfo.getLastModified();
							Long size = objectInfo.getSize();
							String etag = objectInfo.getEtag();
							String contentType = objectInfo.getContentType();
							String contentDisposition = objectInfo
							.getContentDisposition();
							db.commit();
							if (logData != null) {
								updateLogData(bucket, logData);
								logData.setObjectSize(size);
							}
							String versionId = null;
							if (versioning) {
								versionId = objectInfo.getVersionId();
							}
							if (request.getGetData()) {
								if (request.getInlineData()) {
									if ((size * 4) > WalrusProperties.MAX_INLINE_DATA_SIZE) {
										throw new InlineDataTooLargeException(
												bucketName + "/" + objectKey);
									}
									byte[] bytes = new byte[102400];
									int bytesRead = 0, offset = 0;
									String base64Data = "";
									try {
										FileIO fileIO = storageManager.prepareForRead(
												bucketName, objectName);
										while ((bytesRead = fileIO.read(offset)) > 0) {
											ByteBuffer buffer = fileIO.getBuffer();
											if(buffer != null) {
												buffer.get(bytes, 0, bytesRead);
												base64Data += new String(bytes, 0,
														bytesRead);
												offset += bytesRead;
											}
										}
										fileIO.finish();
									} catch (Exception e) {
										LOG.error(e, e);
										throw new EucalyptusCloudException(e);
									}
									reply
									.setBase64Data(Hashes
											.base64encode(base64Data));
								} else {
									// support for large objects
									if (WalrusProperties.trackUsageStatistics) {
										walrusStatistics.updateBytesOut(objectInfo
												.getSize());
									}
									storageManager.sendObject(request,
											httpResponse, bucketName, objectName, size,
											etag, DateUtils.format(lastModified
													.getTime(),
													DateUtils.ISO8601_DATETIME_PATTERN)
													+ ".000Z", contentType,
													contentDisposition, request
													.getIsCompressed(), versionId,
													logData);
									return null;
								}
							} else {
								storageManager.sendHeaders(request,
										httpResponse, size, etag, DateUtils.format(
												lastModified.getTime(),
												DateUtils.ISO8601_DATETIME_PATTERN)
												+ ".000Z", contentType,
												contentDisposition, versionId, logData);
								return null;

							}
							reply.setEtag(etag);
							reply.setLastModified(DateUtils.format(lastModified,
									DateUtils.ISO8601_DATETIME_PATTERN)
									+ ".000Z");
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

	public GetObjectExtendedResponseType getObjectExtended(
			GetObjectExtendedType request) throws EucalyptusCloudException {
		GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request
		.getReply();
		Long byteRangeStart = request.getByteRangeStart();
		if (byteRangeStart == null) {
			byteRangeStart = 0L;
		}
		Long byteRangeEnd = request.getByteRangeEnd();
		if (byteRangeEnd == null) {
			byteRangeEnd = -1L;
		}
		Date ifModifiedSince = request.getIfModifiedSince();
		Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
		String ifMatch = request.getIfMatch();
		String ifNoneMatch = request.getIfNoneMatch();
		boolean returnCompleteObjectOnFailure = request
		.getReturnCompleteObjectOnConditionFailure();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();
		Status status = new Status();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					boolean versioning = false;
					if (bucket.isVersioningEnabled())
						versioning = true;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						ObjectInfo objectInfo = objectInfos.get(0);

						if (objectInfo.canRead(userId)) {
							String etag = objectInfo.getEtag();
							String objectName = objectInfo.getObjectName();
							if (byteRangeEnd == -1)
								byteRangeEnd = objectInfo.getSize();
							if ((byteRangeStart > objectInfo.getSize())
									|| (byteRangeStart > byteRangeEnd)
									|| (byteRangeEnd > objectInfo.getSize())
									|| (byteRangeStart < 0 || byteRangeEnd < 0)) {
								throw new InvalidRangeException("Range: "
										+ byteRangeStart + "-" + byteRangeEnd
										+ "object: " + bucketName + "/" + objectKey);
							}
							DefaultHttpResponse httpResponse = new DefaultHttpResponse(
									HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
							if (ifMatch != null) {
								if (!ifMatch.equals(etag)
										&& !returnCompleteObjectOnFailure) {
									db.rollback();
									throw new PreconditionFailedException(objectKey
											+ " etag: " + etag);
								}

							}
							if (ifNoneMatch != null) {
								if (ifNoneMatch.equals(etag)
										&& !returnCompleteObjectOnFailure) {
									db.rollback();
									throw new NotModifiedException(objectKey
											+ " ETag: " + etag);
								}
							}
							Date lastModified = objectInfo.getLastModified();
							if (ifModifiedSince != null) {
								if ((ifModifiedSince.getTime() >= lastModified
										.getTime())
										&& !returnCompleteObjectOnFailure) {
									db.rollback();
									throw new NotModifiedException(objectKey
											+ " LastModified: "
											+ lastModified.toString());
								}
							}
							if (ifUnmodifiedSince != null) {
								if ((ifUnmodifiedSince.getTime() < lastModified
										.getTime())
										&& !returnCompleteObjectOnFailure) {
									db.rollback();
									throw new PreconditionFailedException(objectKey
											+ " lastModified: "
											+ lastModified.toString());
								}
							}
							if (request.getGetMetaData()) {
								List<MetaDataInfo> metaDataInfos = objectInfo
								.getMetaData();
								for (MetaDataInfo metaDataInfo : metaDataInfos) {
									httpResponse.addHeader(
											WalrusProperties.AMZ_META_HEADER_PREFIX
											+ metaDataInfo.getName(),
											metaDataInfo.getValue());
								}
							}
							Long size = objectInfo.getSize();
							String contentType = objectInfo.getContentType();
							String contentDisposition = objectInfo
							.getContentDisposition();
							db.commit();
							if (logData != null) {
								updateLogData(bucket, logData);
								logData.setObjectSize(size);
							}
							String versionId = null;
							if (versioning) {
								versionId = objectInfo.getVersionId() != null ? objectInfo
										.getVersionId()
										: WalrusProperties.NULL_VERSION_ID;
							}
							if (request.getGetData()) {
								if (WalrusProperties.trackUsageStatistics) {
									walrusStatistics.updateBytesOut(size);
								}
								storageManager.sendObject(request,
										httpResponse, bucketName, objectName,
										byteRangeStart, byteRangeEnd, size, etag,
										DateUtils.format(lastModified.getTime(),
												DateUtils.ISO8601_DATETIME_PATTERN
												+ ".000Z"), contentType,
												contentDisposition, request.getIsCompressed(),
												versionId, logData);
								return null;
							} else {
								storageManager.sendHeaders(request,
										httpResponse, size, etag, DateUtils.format(
												lastModified.getTime(),
												DateUtils.ISO8601_DATETIME_PATTERN
												+ ".000Z"), contentType,
												contentDisposition, versionId, logData);
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

	public GetBucketLocationResponseType getBucketLocation(
			GetBucketLocationType request) throws EucalyptusCloudException {
		GetBucketLocationResponseType reply = (GetBucketLocationResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canRead(userId)) {
						if (logData != null) {
							updateLogData(bucket, logData);
							reply.setLogData(logData);
						}
						String location = bucket.getLocation();
						if (location == null) {
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

	public CopyObjectResponseType copyObject(CopyObjectType request)
	throws EucalyptusCloudException {
		CopyObjectResponseType reply = (CopyObjectResponseType) request
		.getReply();
		String userId = request.getUserId();
		String sourceBucket = request.getSourceBucket();
		String sourceKey = request.getSourceObject();
		String sourceVersionId = request.getSourceVersionId();
		String destinationBucket = request.getDestinationBucket();
		String destinationKey = request.getDestinationObject();
		String metadataDirective = request.getMetadataDirective();
		AccessControlListType accessControlList = request
		.getAccessControlList();

		String copyIfMatch = request.getCopySourceIfMatch();
		String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
		Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
		Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();

		if (metadataDirective == null)
			metadataDirective = "COPY";
		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(sourceBucket);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(sourceBucket,
					sourceKey);
			searchObjectInfo.setVersionId(sourceVersionId);
			if(sourceVersionId == null)
				searchObjectInfo.setLast(true);
			List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo sourceObjectInfo = objectInfos.get(0);
				if (sourceObjectInfo.canRead(userId)) {
					if (copyIfMatch != null) {
						if (!copyIfMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey
									+ " CopySourceIfMatch: " + copyIfMatch);
						}
					}
					if (copyIfNoneMatch != null) {
						if (copyIfNoneMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey
									+ " CopySourceIfNoneMatch: "
									+ copyIfNoneMatch);
						}
					}
					if (copyIfUnmodifiedSince != null) {
						long unmodifiedTime = copyIfUnmodifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified()
						.getTime();
						if (unmodifiedTime < objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey
									+ " CopySourceIfUnmodifiedSince: "
									+ copyIfUnmodifiedSince.toString());
						}
					}
					if (copyIfModifiedSince != null) {
						long modifiedTime = copyIfModifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified()
						.getTime();
						if (modifiedTime > objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey
									+ " CopySourceIfModifiedSince: "
									+ copyIfModifiedSince.toString());
						}
					}
					BucketInfo destinationBucketInfo = new BucketInfo(
							destinationBucket);
					List<BucketInfo> destinationBuckets = db
					.query(destinationBucketInfo);
					if (destinationBuckets.size() > 0) {
						BucketInfo foundDestinationBucketInfo = destinationBuckets
						.get(0);
						if (foundDestinationBucketInfo.canWrite(userId)) {
							// all ok
							String destinationVersionId = sourceVersionId;
							ObjectInfo destinationObjectInfo = null;
							String destinationObjectName;
							ObjectInfo destSearchObjectInfo = new ObjectInfo(
									destinationBucket, destinationKey);
							if(foundDestinationBucketInfo.isVersioningEnabled()) {
								if(sourceVersionId != null)
									destinationVersionId = sourceVersionId;
								else 
									destinationVersionId = UUID.randomUUID().toString().replaceAll("-", "");
							} else {
								destinationVersionId = WalrusProperties.NULL_VERSION_ID;
							}
							destSearchObjectInfo.setVersionId(destinationVersionId);
							List<ObjectInfo> destinationObjectInfos = dbObject
							.query(destSearchObjectInfo);
							if (destinationObjectInfos.size() > 0) {
								destinationObjectInfo = destinationObjectInfos
								.get(0);
								if (!destinationObjectInfo.canWrite(userId)) {
									db.rollback();
									throw new AccessDeniedException("Key",
											destinationKey);
								}
							}
							boolean addNew = false;						
							if (destinationObjectInfo == null) {
								// not found. create a new one
								addNew = true;
								destinationObjectInfo = new ObjectInfo();
								List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
								destinationObjectInfo
								.setBucketName(destinationBucket);
								destinationObjectInfo
								.setObjectKey(destinationKey);
								destinationObjectInfo.addGrants(userId,
										grantInfos, accessControlList);
								destinationObjectInfo.setGrants(grantInfos);
								destinationObjectInfo
								.setObjectName(UUID.randomUUID().toString());
							} else {
								if (destinationObjectInfo.canWriteACP(userId)) {
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
									destinationObjectInfo.addGrants(userId,
											grantInfos, accessControlList);
									destinationObjectInfo.setGrants(grantInfos);
								}
							}
							destinationObjectInfo.setSize(sourceObjectInfo
									.getSize());
							destinationObjectInfo
							.setStorageClass(sourceObjectInfo
									.getStorageClass());
							destinationObjectInfo.setOwnerId(sourceObjectInfo
									.getOwnerId());
							destinationObjectInfo
							.setContentType(sourceObjectInfo
									.getContentType());
							destinationObjectInfo
							.setContentDisposition(sourceObjectInfo
									.getContentDisposition());
							String etag = sourceObjectInfo.getEtag();
							Date lastModified = sourceObjectInfo
							.getLastModified();
							destinationObjectInfo.setEtag(etag);
							destinationObjectInfo.setLastModified(lastModified);
							destinationObjectInfo.setVersionId(destinationVersionId);
							destinationObjectInfo.setLast(true);
							destinationObjectInfo.setDeleted(false);
							if (!metadataDirective.equals("REPLACE")) {
								destinationObjectInfo
								.setMetaData(sourceObjectInfo
										.cloneMetaData());
							} else {
								List<MetaDataEntry> metaData = request
								.getMetaData();
								if (metaData != null)
									destinationObjectInfo
									.replaceMetaData(metaData);
							}

							String sourceObjectName = sourceObjectInfo
							.getObjectName();
							destinationObjectName = destinationObjectInfo
							.getObjectName();

							try {
								storageManager.copyObject(sourceBucket,
										sourceObjectName, destinationBucket,
										destinationObjectName);
								if (WalrusProperties.trackUsageStatistics)
									walrusStatistics
									.updateSpaceUsed(sourceObjectInfo
											.getSize());
							} catch (Exception ex) {
								LOG.error(ex);
								db.rollback();
								throw new EucalyptusCloudException(
										"Could not rename " + sourceObjectName
										+ " to "
										+ destinationObjectName);
							}
							if(addNew)
								dbObject.add(destinationObjectInfo);

							//get rid of delete marker
							ObjectInfo deleteMarker = new ObjectInfo(destinationBucket, destinationKey);
							deleteMarker.setDeleted(true);
							try {
								ObjectInfo foundDeleteMarker = dbObject.getUnique(deleteMarker);
								dbObject.delete(foundDeleteMarker);
							} catch(EucalyptusCloudException ex) {
								//no delete marker found.
								LOG.trace("No delete marker found for: " + destinationBucket + "/" + destinationKey);
							}

							reply.setEtag(etag);
							reply.setLastModified(DateUtils.format(lastModified
									.getTime(),
									DateUtils.ISO8601_DATETIME_PATTERN)
									+ ".000Z");

							if(foundDestinationBucketInfo.isVersioningEnabled()) {
								reply.setCopySourceVersionId(sourceVersionId);
								reply.setVersionId(destinationVersionId);
							}							
							db.commit();
							return reply;
						} else {
							db.rollback();
							throw new AccessDeniedException("Bucket",
									destinationBucket);
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

	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(
			SetBucketLoggingStatusType request) throws EucalyptusCloudException {
		SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request
		.getReply();
		String bucket = request.getBucket();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo, targetBucketInfo;
		try {
			bucketInfo = db.getUnique(new BucketInfo(bucket));
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}

		if (request.getLoggingEnabled() != null) {
			String targetBucket = request.getLoggingEnabled().getTargetBucket();
			String targetPrefix = request.getLoggingEnabled().getTargetPrefix();
			List<Grant> targetGrantsList = null;
			TargetGrants targetGrants = request.getLoggingEnabled()
			.getTargetGrants();
			if (targetGrants != null)
				targetGrantsList = targetGrants.getGrants();
			if (targetPrefix == null)
				targetPrefix = "";
			try {
				targetBucketInfo = db.getUnique(new BucketInfo(targetBucket));
			} catch (EucalyptusCloudException ex) {
				db.rollback();
				throw new NoSuchBucketException(targetBucket);
			}
			if (!targetBucketInfo.hasLoggingPerms()) {
				db.rollback();
				throw new InvalidTargetBucketForLoggingException(targetBucket);
			}
			bucketInfo.setTargetBucket(targetBucket);
			bucketInfo.setTargetPrefix(targetPrefix);
			bucketInfo.setLoggingEnabled(true);
			if (targetGrantsList != null) {
				targetBucketInfo.addGrants(targetGrantsList);
			}
		} else {
			bucketInfo.setLoggingEnabled(false);
		}
		db.commit();
		return reply;
	}

	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(
			GetBucketLoggingStatusType request) throws EucalyptusCloudException {
		GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request
		.getReply();
		String bucket = request.getBucket();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		try {
			BucketInfo bucketInfo = db.getUnique(new BucketInfo(bucket));
			if (bucketInfo.getLoggingEnabled()) {
				String targetBucket = bucketInfo.getTargetBucket();
				ArrayList<Grant> grants = new ArrayList<Grant>();
				try {
					BucketInfo targetBucketInfo = db.getUnique(new BucketInfo(
							targetBucket));
					List<GrantInfo> grantInfos = targetBucketInfo.getGrants();
					for (GrantInfo grantInfo : grantInfos) {
						String uId = grantInfo.getUserId();
						try {
							if (uId != null) {
								User grantUserInfo = Users.lookupUser(uId);
								addPermission(grants, grantUserInfo, grantInfo);
							} else {
								addPermission(grants, grantInfo);
							}
						} catch (NoSuchUserException e) {
							db.rollback();
							throw new AccessDeniedException("Bucket",
									targetBucket);
						}
					}
				} catch (EucalyptusCloudException ex) {
					db.rollback();
					throw new InvalidTargetBucketForLoggingException(
							targetBucket);
				}
				LoggingEnabled loggingEnabled = new LoggingEnabled();
				loggingEnabled.setTargetBucket(bucketInfo.getTargetBucket());
				loggingEnabled.setTargetPrefix(bucketInfo.getTargetPrefix());

				TargetGrants targetGrants = new TargetGrants();
				targetGrants.setGrants(grants);
				loggingEnabled.setTargetGrants(targetGrants);
				reply.setLoggingEnabled(loggingEnabled);
			}
		} catch (EucalyptusCloudException ex) {
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

	public GetBucketVersioningStatusResponseType getBucketVersioningStatus(
			GetBucketVersioningStatusType request)
	throws EucalyptusCloudException {
		GetBucketVersioningStatusResponseType reply = (GetBucketVersioningStatusResponseType) request
		.getReply();
		String bucket = request.getBucket();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		try {
			BucketInfo bucketInfo = db.getUnique(new BucketInfo(bucket));
			if (bucketInfo.getVersioning() != null) {
				String status = bucketInfo.getVersioning();
				if (WalrusProperties.VersioningStatus.Disabled.toString()
						.equals(status))
					reply
					.setVersioningStatus(WalrusProperties.VersioningStatus.Suspended
							.toString());
				else
					reply.setVersioningStatus(status);
			}
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}
		db.commit();
		return reply;
	}

	public SetBucketVersioningStatusResponseType setBucketVersioningStatus(
			SetBucketVersioningStatusType request)
	throws EucalyptusCloudException {
		SetBucketVersioningStatusResponseType reply = (SetBucketVersioningStatusResponseType) request
		.getReply();
		String bucket = request.getBucket();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo;
		try {
			bucketInfo = db.getUnique(new BucketInfo(bucket));
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}

		if (request.getVersioningStatus() != null) {
			String status = request.getVersioningStatus();
			if (WalrusProperties.VersioningStatus.Enabled.toString().equals(
					status))
				bucketInfo
				.setVersioning(WalrusProperties.VersioningStatus.Enabled
						.toString());
			else if (WalrusProperties.VersioningStatus.Suspended.toString()
					.equals(status)
					&& WalrusProperties.VersioningStatus.Enabled.toString()
					.equals(bucketInfo.getVersioning()))
				bucketInfo
				.setVersioning(WalrusProperties.VersioningStatus.Suspended
						.toString());
		}
		db.commit();
		return reply;
	}

	public ListVersionsResponseType listVersions(ListVersionsType request)
	throws EucalyptusCloudException {
		ListVersionsResponseType reply = (ListVersionsResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String userId = request.getUserId();
		String prefix = request.getPrefix();
		if (prefix == null)
			prefix = "";

		String keyMarker = request.getKeyMarker();
		String versionIdMarker = request.getVersionIdMarker();

		int maxKeys = -1;
		String maxKeysString = request.getMaxKeys();
		if (maxKeysString != null)
			maxKeys = Integer.parseInt(maxKeysString);
		else
			maxKeys = WalrusProperties.MAX_KEYS;

		String delimiter = request.getDelimiter();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		bucketInfo.setHidden(false);
		List<BucketInfo> bucketList = db.query(bucketInfo);

		ArrayList<PrefixEntry> prefixes = new ArrayList<PrefixEntry>();

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request
					.getLogData() : null;
					if (bucket.canRead(userId)) {
						if (bucket.isVersioningDisabled()) {
							db.rollback();
							throw new EucalyptusCloudException(
									"Versioning has not been enabled for bucket: "
									+ bucketName);
						}
						if (logData != null) {
							updateLogData(bucket, logData);
							reply.setLogData(logData);
						}
						if (request.isAdministrator()) {
							EntityWrapper<WalrusSnapshotInfo> dbSnap = db
							.recast(WalrusSnapshotInfo.class);
							WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
							walrusSnapInfo.setSnapshotBucket(bucketName);
							List<WalrusSnapshotInfo> walrusSnaps = dbSnap
							.query(walrusSnapInfo);
							if (walrusSnaps.size() > 0) {
								db.rollback();
								throw new NoSuchBucketException(bucketName);
							}
						}
						reply.setName(bucketName);
						reply.setIsTruncated(false);
						if (maxKeys >= 0)
							reply.setMaxKeys(maxKeys);
						reply.setPrefix(prefix);
						reply.setKeyMarker(keyMarker);
						reply.setVersionIdMarker(versionIdMarker);
						if (delimiter != null)
							reply.setDelimiter(delimiter);
						EntityWrapper<ObjectInfo> dbObject = db
						.recast(ObjectInfo.class);
						ObjectInfo searchObjectInfo = new ObjectInfo();
						searchObjectInfo.setBucketName(bucketName);
						List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
						if (objectInfos.size() > 0) {
							int howManyProcessed = 0;
							if (keyMarker != null || objectInfos.size() < maxKeys)
								Collections.sort(objectInfos);
							ArrayList<VersionEntry> versions = new ArrayList<VersionEntry>();
							ArrayList<DeleteMarkerEntry> deleteMarkers = new ArrayList<DeleteMarkerEntry>();

							for (ObjectInfo objectInfo : objectInfos) {
								String objectKey = objectInfo.getObjectKey();

								if(keyMarker != null) { if(objectKey.compareTo(keyMarker)
										<= 0) continue; } else if (versionIdMarker != null) {
											if(!objectInfo.getVersionId().equals(versionIdMarker))
												continue;
										}

								if (prefix != null) {
									if (!objectKey.startsWith(prefix)) {
										continue;
									} else {
										if (delimiter != null) {
											String[] parts = objectKey.substring(
													prefix.length()).split(delimiter);
											if (parts.length > 1) {
												String prefixString = parts[0]
												                            + delimiter;
												boolean foundPrefix = false;
												for (PrefixEntry prefixEntry : prefixes) {
													if (prefixEntry.getPrefix().equals(
															prefixString)) {
														foundPrefix = true;
														break;
													}
												}
												if (!foundPrefix) {
													prefixes.add(new PrefixEntry(
															prefixString));
													if (maxKeys >= 0) {
														if (howManyProcessed++ >= maxKeys) {
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
								if (maxKeys >= 0) {
									if (howManyProcessed++ >= maxKeys) {
										reply.setIsTruncated(true);
										break;
									}
								}
								if (!objectInfo.getDeleted()) {
									VersionEntry versionEntry = new VersionEntry();
									versionEntry.setKey(objectKey);
									versionEntry
									.setVersionId(objectInfo.getVersionId());
									versionEntry.setEtag(objectInfo.getEtag());
									versionEntry.setLastModified(DateUtils.format(
											objectInfo.getLastModified().getTime(),
											DateUtils.ISO8601_DATETIME_PATTERN)
											+ ".000Z");
									String displayName = objectInfo.getOwnerId();
									try {
										User userInfo = Users.lookupUser(displayName);
										versionEntry.setOwner(new CanonicalUserType(
												userInfo.getQueryId(), displayName));
									} catch (NoSuchUserException e) {
										db.rollback();
										throw new AccessDeniedException("Bucket",
												bucketName, logData);
									}
									versionEntry.setSize(objectInfo.getSize());
									versionEntry.setStorageClass(objectInfo
											.getStorageClass());
									versionEntry.setIsLatest(objectInfo.getLast());
									versions.add(versionEntry);
								} else {
									DeleteMarkerEntry deleteMarkerEntry = new DeleteMarkerEntry();
									deleteMarkerEntry.setKey(objectKey);
									deleteMarkerEntry.setVersionId(objectInfo
											.getVersionId());
									deleteMarkerEntry.setLastModified(DateUtils.format(
											objectInfo.getLastModified().getTime(),
											DateUtils.ISO8601_DATETIME_PATTERN)
											+ ".000Z");
									String displayName = objectInfo.getOwnerId();
									try {
										User userInfo = Users.lookupUser(displayName);
										deleteMarkerEntry
										.setOwner(new CanonicalUserType(
												userInfo.getQueryId(),
												displayName));
									} catch (NoSuchUserException e) {
										db.rollback();
										throw new AccessDeniedException("Bucket",
												bucketName, logData);
									}
									deleteMarkerEntry.setIsLatest(objectInfo.getLast());
									deleteMarkers.add(deleteMarkerEntry);
								}
							}
							reply.setVersions(versions);
							reply.setDeleteMarkers(deleteMarkers);
							if (prefix != null) {
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

	public DeleteVersionResponseType deleteVersion(DeleteVersionType request)
	throws EucalyptusCloudException {
		DeleteVersionResponseType reply = (DeleteVersionResponseType) request
		.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String userId = request.getUserId();

		EntityWrapper<BucketInfo> db = WalrusControl.getEntityWrapper();
		BucketInfo bucketInfos = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.query(bucketInfos);

		if (bucketList.size() > 0) {
			BucketInfo bucketInfo = bucketList.get(0);
			BucketLogData logData = bucketInfo.getLoggingEnabled() ? request
					.getLogData() : null;
					ObjectInfo foundObject = null;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
					if(request.getVersionid() == null) {
						db.rollback();
						throw new EucalyptusCloudException("versionId is null");
					}
					searchObjectInfo.setVersionId(request.getVersionid());
					List<ObjectInfo> objectInfos = dbObject.query(searchObjectInfo);
					if (objectInfos.size() > 0) {
						foundObject = objectInfos.get(0);
					}

					if (foundObject != null) {
						if (foundObject.canWrite(userId)) {
							dbObject.delete(foundObject);
							if(!foundObject.getDeleted()) {
								String objectName = foundObject.getObjectName();							 
								for (GrantInfo grantInfo : foundObject.getGrants()) {
									db.getEntityManager().remove(grantInfo);
								}
								Long size = foundObject.getSize();
								bucketInfo.setBucketSize(bucketInfo.getBucketSize() - size);
								ObjectDeleter objectDeleter = new ObjectDeleter(bucketName,
										objectName, size);
								objectDeleter.start();
							}
							reply.setCode("200");
							reply.setDescription("OK");
							if (logData != null) {
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
}
