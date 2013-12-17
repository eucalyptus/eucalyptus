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

package com.eucalyptus.walrus;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.UUID;
import java.util.HashMap;

import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.util.DateUtils;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.Criteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.reporting.event.S3ObjectEvent.S3ObjectAction;
import com.eucalyptus.storage.common.fs.FileIO;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.DeleteMarkerEntry;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Status;
import com.eucalyptus.storage.msgs.s3.TargetGrants;
import com.eucalyptus.storage.msgs.s3.VersionEntry;
import com.eucalyptus.storage.msgs.s3.KeyEntry;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Lookups;
import com.eucalyptus.walrus.bittorrent.TorrentClient;
import com.eucalyptus.walrus.bittorrent.TorrentCreator;
import com.eucalyptus.walrus.bittorrent.Torrents;
import com.eucalyptus.walrus.entities.BucketInfo;
import com.eucalyptus.walrus.entities.GrantInfo;
import com.eucalyptus.walrus.entities.ImageCacheInfo;
import com.eucalyptus.walrus.entities.MetaDataInfo;
import com.eucalyptus.walrus.entities.ObjectInfo;
import com.eucalyptus.walrus.entities.TorrentInfo;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.entities.WalrusSnapshotInfo;
import com.eucalyptus.walrus.exceptions.AccessDeniedException;
import com.eucalyptus.walrus.exceptions.BucketAlreadyExistsException;
import com.eucalyptus.walrus.exceptions.BucketNotEmptyException;
import com.eucalyptus.walrus.exceptions.ContentMismatchException;
import com.eucalyptus.walrus.exceptions.EntityTooLargeException;
import com.eucalyptus.walrus.exceptions.HeadAccessDeniedException;
import com.eucalyptus.walrus.exceptions.HeadNoSuchBucketException;
import com.eucalyptus.walrus.exceptions.HeadNoSuchEntityException;
import com.eucalyptus.walrus.exceptions.InlineDataTooLargeException;
import com.eucalyptus.walrus.exceptions.InvalidBucketNameException;
import com.eucalyptus.walrus.exceptions.InvalidRangeException;
import com.eucalyptus.walrus.exceptions.InvalidTargetBucketForLoggingException;
import com.eucalyptus.walrus.exceptions.NoSuchBucketException;
import com.eucalyptus.walrus.exceptions.NoSuchEntityException;
import com.eucalyptus.walrus.exceptions.NotModifiedException;
import com.eucalyptus.walrus.exceptions.PreconditionFailedException;
import com.eucalyptus.walrus.exceptions.TooManyBucketsException;
import com.eucalyptus.walrus.exceptions.InvalidArgumentException;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.AddObjectResponseType;
import com.eucalyptus.walrus.msgs.AddObjectType;
import com.eucalyptus.walrus.msgs.CopyObjectResponseType;
import com.eucalyptus.walrus.msgs.CopyObjectType;
import com.eucalyptus.walrus.msgs.CreateBucketResponseType;
import com.eucalyptus.walrus.msgs.CreateBucketType;
import com.eucalyptus.walrus.msgs.DeleteBucketResponseType;
import com.eucalyptus.walrus.msgs.DeleteBucketType;
import com.eucalyptus.walrus.msgs.DeleteObjectResponseType;
import com.eucalyptus.walrus.msgs.DeleteObjectType;
import com.eucalyptus.walrus.msgs.DeleteVersionResponseType;
import com.eucalyptus.walrus.msgs.DeleteVersionType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetBucketLocationResponseType;
import com.eucalyptus.walrus.msgs.GetBucketLocationType;
import com.eucalyptus.walrus.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.walrus.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.walrus.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.walrus.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedType;
import com.eucalyptus.walrus.msgs.GetObjectResponseType;
import com.eucalyptus.walrus.msgs.GetObjectType;
import com.eucalyptus.walrus.msgs.HeadBucketResponseType;
import com.eucalyptus.walrus.msgs.HeadBucketType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsType;
import com.eucalyptus.walrus.msgs.ListBucketResponseType;
import com.eucalyptus.walrus.msgs.ListBucketType;
import com.eucalyptus.walrus.msgs.ListVersionsResponseType;
import com.eucalyptus.walrus.msgs.ListVersionsType;
import com.eucalyptus.walrus.msgs.PostObjectResponseType;
import com.eucalyptus.walrus.msgs.PostObjectType;
import com.eucalyptus.walrus.msgs.PutObjectInlineResponseType;
import com.eucalyptus.walrus.msgs.PutObjectInlineType;
import com.eucalyptus.walrus.msgs.PutObjectResponseType;
import com.eucalyptus.walrus.msgs.PutObjectType;
import com.eucalyptus.walrus.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.walrus.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.walrus.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.walrus.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.walrus.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.SetRESTBucketAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.SetRESTBucketAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.SetRESTObjectAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.SetRESTObjectAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.UploadPartResponseType;
import com.eucalyptus.walrus.msgs.UploadPartType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadType;
import com.eucalyptus.walrus.msgs.Part;

import com.eucalyptus.walrus.msgs.WalrusDataMessage;
import com.eucalyptus.walrus.msgs.WalrusDataMessenger;
import com.eucalyptus.walrus.msgs.WalrusDataQueue;
import com.eucalyptus.walrus.msgs.WalrusMonitor;
import com.eucalyptus.walrus.pipeline.WalrusRESTBinding;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import edu.ucsb.eucalyptus.util.SystemUtil;

public class WalrusFSManager extends WalrusManager {
	private static Logger LOG = Logger.getLogger(WalrusManager.class);

	private StorageManager storageManager;
	private WalrusImageManager walrusImageManager;

	public static void configure() {
	}

	public WalrusFSManager(StorageManager storageManager,
			WalrusImageManager walrusImageManager) {
		this.storageManager = storageManager;
		this.walrusImageManager = walrusImageManager;
	}

	@Override
	public void initialize() throws EucalyptusCloudException {
		check();
	}

	@Override
	public void check() throws EucalyptusCloudException {
		String bukkitDir = WalrusInfo.getWalrusInfo().getStorageDir();
		File bukkits = new File(WalrusInfo.getWalrusInfo().getStorageDir());
		if (!bukkits.exists()) {
			if (!bukkits.mkdirs()) {
				LOG.fatal("Unable to make bucket root directory: " + bukkitDir);
				throw new EucalyptusCloudException("Invalid bucket root directory");
			}
		} else if (!bukkits.canWrite()) {
			LOG.fatal("Cannot write to bucket root directory: " + bukkitDir);
			throw new EucalyptusCloudException("Invalid bucket root directory");
		}
		try {
			SystemUtil.setEucaReadWriteOnly(bukkitDir);
		} catch (EucalyptusCloudException ex) {
			LOG.fatal(ex);
		}

	}

	private boolean bucketHasSnapshots(String bucketName) throws Exception {
		EntityWrapper<WalrusSnapshotInfo> dbSnap = null;

		try {
			dbSnap = EntityWrapper.get(WalrusSnapshotInfo.class);
			WalrusSnapshotInfo walrusSnapInfo = new WalrusSnapshotInfo();
			walrusSnapInfo.setSnapshotBucket(bucketName);

			Criteria snapCount = dbSnap.createCriteria(WalrusSnapshotInfo.class).add(Example.create(walrusSnapInfo)).setProjection(Projections.rowCount());
			snapCount.setReadOnly(true);
			Long rowCount = (Long) snapCount.uniqueResult();
			dbSnap.rollback();
			if (rowCount != null && rowCount.longValue() > 0) {
				return true;
			}
			return false;
		} catch (Exception e) {
			if (dbSnap != null) {
				dbSnap.rollback();
			}
			throw e;
		}
	}

	@Override
	public ListAllMyBucketsResponseType listAllMyBuckets(
			ListAllMyBucketsType request) throws EucalyptusCloudException {
		LOG.info("Handling ListAllBuckets request");
		ListAllMyBucketsResponseType reply = (ListAllMyBucketsResponseType) request
				.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		if (account == null) {
			throw new AccessDeniedException("no such account");
		}
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo searchBucket = new BucketInfo();
			searchBucket.setOwnerId(account.getAccountNumber());
			searchBucket.setHidden(false);

			List<BucketInfo> bucketInfoList = db.queryEscape(searchBucket);

			ArrayList<BucketListEntry> buckets = new ArrayList<BucketListEntry>();

			for (BucketInfo bucketInfo : bucketInfoList) {
				if (ctx.hasAdministrativePrivileges()) {
					try {
						// TODO: zhill -- we should modify the bucket schema to
						// indicate if the bucket is a snapshot bucket, or use a
						// seperate type for snap containers
						if (bucketHasSnapshots(bucketInfo.getBucketName())) {
							continue;
						}
					} catch (Exception e) {
						LOG.debug(e, e);
						continue;
					}
				}
				
				if (ctx.hasAdministrativePrivileges()
						|| Lookups.checkPrivilege(PolicySpec.S3_LISTALLMYBUCKETS, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET,
								bucketInfo.getBucketName(), bucketInfo.getOwnerId())) {

					buckets.add(new BucketListEntry(bucketInfo.getBucketName(), DateUtils.format(bucketInfo.getCreationDate().getTime(),
							DateUtils.ALT_ISO8601_DATE_PATTERN)));
				}
			}
			db.commit();

			try {
				CanonicalUser owner = new CanonicalUser(account.getCanonicalId(), account.getName());
				ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
				reply.setOwner(owner);
				bucketList.setBuckets(buckets);
				reply.setBucketList(bucketList);
			} catch (Exception ex) {
				LOG.error(ex);
				throw new AccessDeniedException("Account: " + account.getName() + " not found", ex);
			}
		} catch (EucalyptusCloudException e) {
			db.rollback();
			throw e;
		} catch (Exception e) {
			LOG.debug(e, e);
			db.rollback();
		}
		return reply;
	}

	/**
	 * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise returns 404 if not found or 403 if no accesss.
	 * 
	 * @param request
	 * @return
	 * @throws EucalyptusCloudException
	 */
	@Override
	public HeadBucketResponseType headBucket(HeadBucketType request) throws EucalyptusCloudException {
		HeadBucketResponseType reply = (HeadBucketResponseType) request.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String bucketName = request.getBucket();
		EntityTransaction db = Entities.get(BucketInfo.class);
		try {
			BucketInfo bucket = Entities.uniqueResult(new BucketInfo(bucketName));
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canRead(account.getAccountNumber()) && (bucket.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_LISTBUCKET,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				return reply;
			} else {
				// Insufficient access, return 403
				throw new HeadAccessDeniedException(bucketName);
			}
		} catch (NoSuchElementException e) {
			// Bucket not found return 404
			throw new HeadNoSuchBucketException(bucketName);
		} catch (TransactionException e) {
			LOG.error("DB transaction error looking up bucket " + bucketName + ": " + e.getMessage());
			LOG.debug("DB tranction exception looking up bucket " + bucketName, e);
			throw new EucalyptusCloudException("Internal error doing db lookup for " + bucketName, e);
		} finally {
			// Nothing to commit, always rollback.
			db.rollback();
		}
	}

	@Override
	public CreateBucketResponseType createBucket(CreateBucketType request)
			throws EucalyptusCloudException {
		CreateBucketResponseType reply = (CreateBucketResponseType) request
				.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		
		String bucketName = request.getBucket();
		String locationConstraint = request.getLocationConstraint();

		if (account == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		}

		AccessControlList accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlList();
		}

		if (!checkBucketName(bucketName))
			throw new InvalidBucketNameException(bucketName);

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);

		if (WalrusProperties.shouldEnforceUsageLimits && !Contexts.lookup().hasAdministrativePrivileges()) {
			BucketInfo searchBucket = new BucketInfo();
			searchBucket.setOwnerId(account.getAccountNumber());
			List<BucketInfo> bucketList = db.queryEscape(searchBucket);
			if (bucketList.size() >= WalrusInfo.getWalrusInfo().getStorageMaxBucketsPerAccount()) {
				db.rollback();
				throw new TooManyBucketsException(bucketName);
			}
		}

		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			if (bucketList.get(0).getOwnerId().equals(account.getAccountNumber())) {
				// bucket already exists and you created it
				// s3 just happily indicates that this operations succeeded in this case
				db.rollback();
			} else {
				// bucket already exists
				db.rollback();
				throw new BucketAlreadyExistsException(bucketName);
			}
		} else {
			if (ctx.hasAdministrativePrivileges()
					|| (Permissions.isAuthorized(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, "", ctx.getAccount(), PolicySpec.S3_CREATEBUCKET,
							ctx.getUser()) && Permissions.canAllocate(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, "", PolicySpec.S3_CREATEBUCKET,
							ctx.getUser(), 1L))) {
				// create bucket and set its acl
				BucketInfo bucket = new BucketInfo(account.getAccountNumber(), ctx.getUser().getUserId(), bucketName, new Date());
				ArrayList<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				bucket.addGrants(account.getAccountNumber(), grantInfos, accessControlList);
				bucket.setGrants(grantInfos);
				bucket.setBucketSize(0L);
				bucket.setLoggingEnabled(false);
				bucket.setVersioning(WalrusProperties.VersioningStatus.Disabled.toString());
				bucket.setHidden(false);
				if (locationConstraint != null && locationConstraint.length() > 0) {
					bucket.setLocation(locationConstraint);
				} else {
					bucket.setLocation(null);
				}

				// call the storage manager to save the bucket to disk
				try {
					db.add(bucket);
					db.commit();
					storageManager.createBucket(bucketName);
				} catch (IOException ex) {
					LOG.error(ex, ex);
					throw new BucketAlreadyExistsException(bucketName);
				} catch (Exception ex) {
					LOG.error(ex, ex);
					db.rollback();
					if (Exceptions.isCausedBy(ex, ConstraintViolationException.class)) {
						throw new BucketAlreadyExistsException(bucketName);
					} else {
						throw new EucalyptusCloudException("Unable to create bucket: " + bucketName);
					}
				}

				/* Send an event to reporting to report this S3 usage. */
				// reportWalrusEvent(genBucketEvent(ctx, true));

				// fireBucketUsageEvent(S3BucketEvent.forS3BucketCreate(),
				// bucket.getNaturalId(), bucket.getBucketName(),
				// ctx.getUserFullName(), bucket.getBucketSize() );

			} else {
				LOG.error("Not authorized to create bucket by " + ctx.getUserFullName());
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName);
			}
		}
		reply.setBucket(bucketName);
		return reply;
	}

	/* These functions are for reporting S3 usage */

	private boolean checkBucketName(String bucketName) {
		if (!bucketName.matches("^[A-Za-z0-9][A-Za-z0-9._-]+"))
			return false;
		if (bucketName.length() < 3 || bucketName.length() > 255)
			return false;
		if (!checkBucketNameIsNotIp(bucketName)) {
			return false;
		}
		return true;
	}

	private boolean checkBucketNameIsNotIp(String bucketName) {
		if (!WalrusInfo.getWalrusInfo().getBucketNamesRequireDnsCompliance().booleanValue()) {
			return true;
		}
		String[] addrParts = bucketName.split("\\.");
		boolean ipFormat = true;
		if (addrParts.length == 4) {
			for (String addrPart : addrParts) {
				try {
					Integer.parseInt(addrPart);
				} catch (NumberFormatException ex) {
					ipFormat = false;
					break;
				}
			}
		} else {
			ipFormat = false;
		}
		if (ipFormat)
			return false;
		return true;
	}

	private boolean checkDNSNaming(String bucketName) {
		if (!bucketName.matches("^[a-z0-9][a-z0-9.-]+"))
			return false;
		if (bucketName.length() < 3 || bucketName.length() > 63)
			return false;
		if (bucketName.endsWith("-"))
			return false;
		if (bucketName.contains(".."))
			return false;
		if (bucketName.contains("-.") || bucketName.contains(".-"))
			return false;
		return true;
	}

	@Override
	public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
		DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
		String bucketName = request.getBucket();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo searchBucket = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(searchBucket);

		if (bucketList.size() > 0) {
			BucketInfo bucketFound = bucketList.get(0);
			BucketLogData logData = bucketFound.getLoggingEnabled() ? request.getLogData() : null;
			if (ctx.hasAdministrativePrivileges()
					|| (Lookups.checkPrivilege(PolicySpec.S3_DELETEBUCKET, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName,
							bucketFound.getOwnerId()))) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObject = new ObjectInfo();
				searchObject.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObject);
				if (objectInfos.size() == 0) {
					// check if the bucket contains any images
					EntityWrapper<ImageCacheInfo> dbIC = db.recast(ImageCacheInfo.class);
					ImageCacheInfo searchImageCacheInfo = new ImageCacheInfo();
					searchImageCacheInfo.setBucketName(bucketName);
					List<ImageCacheInfo> foundImageCacheInfos = dbIC.queryEscape(searchImageCacheInfo);

					if (foundImageCacheInfos.size() > 0) {
						db.rollback();
						throw new BucketNotEmptyException(bucketName, logData);
					}

					db.delete(bucketFound);
					// Actually remove the bucket from the backing store
					try {
						storageManager.deleteBucket(bucketName);
						/* Send an event to reporting to report this S3 usage. */

						// fireBucketUsageEvent(S3BucketAction.BUCKETDELETE,
						// bucketFound.getNaturalId(),
						// bucketFound.getBucketName(), ctx.getUserFullName(),
						// bucketFound.getBucketSize());

					} catch (Exception ex) {
						// set exception code in reply
						LOG.error(ex);
					}

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
		
		reply.setStatus(HttpResponseStatus.NO_CONTENT);
		reply.setStatusMessage("NO CONTENT");
		db.commit();
		return reply;
	}

	@Override
	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
			throws EucalyptusCloudException {
		GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request
				.getReply();

		String bucketName = request.getBucket();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String ownerId = null;

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		AccessControlList accessControlList = new AccessControlList();
		BucketLogData logData;

		if (bucketList.size() > 0) {
			// construct access control policy from grant infos
			BucketInfo bucket = bucketList.get(0);
			logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			List<GrantInfo> grantInfos = bucket.getGrants();
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canReadACP(account.getAccountNumber()) && (bucket.isGlobalReadACP() || Lookups.checkPrivilege(PolicySpec.S3_GETBUCKETACL,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				if (logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}
				ownerId = bucket.getOwnerId();
				ArrayList<Grant> grants = new ArrayList<Grant>();
				bucket.readPermissions(grants);

				// Construct the grant list from the returned infos
				addGrants(grants, grantInfos);

				accessControlList.setGrants(grants);
			} else {
				LOG.error("Not authorized to get bucket ACL by " + ctx.getUserFullName());
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}

		AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
		try {
			Account ownerInfo = Accounts.lookupAccountById(ownerId);
			accessControlPolicy.setOwner(new CanonicalUser(ownerInfo.getCanonicalId(), ownerInfo.getName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch (AuthException e) {
			db.rollback();
			throw new AccessDeniedException("Bucket", bucketName, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}

	private static void addPermission(ArrayList<Grant> grants, Account account, GrantInfo grantInfo) throws AuthException {
		CanonicalUser user = new CanonicalUser(account.getCanonicalId(), account.getName());

		if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.canWriteACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.FULL_CONTROL.toString()));
			return;
		}

		if (grantInfo.canRead()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.READ.toString()));
		}

		if (grantInfo.canWrite()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.WRITE.toString()));
		}

		if (grantInfo.canReadACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.READ_ACP.toString()));
		}

		if (grantInfo.canWriteACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.WRITE_ACP.toString()));
		}
	}

	private static void addPermission(ArrayList<Grant> grants, CanonicalUser user, GrantInfo grantInfo) throws AuthException, IllegalArgumentException {
		if (user == null) {
			throw new IllegalArgumentException("Cannot add grant for null user");
		}

		if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.canWriteACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.FULL_CONTROL.toString()));
			return;
		}

		if (grantInfo.canRead()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.READ.toString()));
		}

		if (grantInfo.canWrite()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.WRITE.toString()));
		}

		if (grantInfo.canReadACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.READ_ACP.toString()));
		}

		if (grantInfo.canWriteACP()) {
			grants.add(new Grant(new Grantee(user), WalrusProperties.Permission.WRITE_ACP.toString()));
		}
	}

	private static void addPermission(ArrayList<Grant> grants, GrantInfo grantInfo) {
		if (grantInfo.getGrantGroup() != null) {
			Group group = new Group(grantInfo.getGrantGroup());

			if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.canWriteACP()) {
				grants.add(new Grant(new Grantee(group), WalrusProperties.Permission.FULL_CONTROL.toString()));
				return;
			}

			if (grantInfo.canRead()) {
				grants.add(new Grant(new Grantee(group), WalrusProperties.Permission.READ.toString()));
			}

			if (grantInfo.canWrite()) {
				grants.add(new Grant(new Grantee(group), WalrusProperties.Permission.WRITE.toString()));
			}

			if (grantInfo.canReadACP()) {
				grants.add(new Grant(new Grantee(group), WalrusProperties.Permission.READ_ACP.toString()));
			}

			if (grantInfo.canWriteACP()) {
				grants.add(new Grant(new Grantee(group), WalrusProperties.Permission.WRITE_ACP.toString()));
			}
		}
	}

	@Override
	public PutObjectResponseType putObject(PutObjectType request) throws EucalyptusCloudException {
		PutObjectResponseType reply = (PutObjectResponseType) request.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Long oldBucketSize = 0L;
		String md5 = "";
		Date lastModified = null;

		AccessControlList accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlList();
		}

		String key = bucketName + "." + objectKey;
		String randomKey = request.getRandomKey();
		WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			long objSize = 0;
			try {
				objSize = Long.valueOf(request.getContentLength());
			} catch (NumberFormatException e) {
				LOG.error("Invalid content length " + request.getContentLength());
				// TODO(wenye): should handle this properly.
				objSize = 1L;
			}
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				if (logData != null) {
					reply.setLogData(logData);
				}
				String objectName = null;
				String versionId = null;
				Long oldObjectSize = 0L;
				ObjectInfo objectInfo = null;
				if (bucket.isVersioningEnabled()) {
					// If versioning, add new object with new version id and
					// make it the 'latest' version.
					objectInfo = new ObjectInfo(bucketName, objectKey);
					objectInfo.setOwnerId(account.getAccountNumber());
					List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
					objectInfo.addGrants(account.getAccountNumber(), bucket.getOwnerId(), grantInfos, accessControlList);
					objectInfo.setGrants(grantInfos);
					objectName = UUID.randomUUID().toString();
					objectInfo.setObjectName(objectName);
					objectInfo.setSize(0L);
					versionId = UUID.randomUUID().toString().replaceAll("-", "");
					reply.setVersionId(versionId);
				} else {
					// If no versioning enabled, put using a null version id,
					// this will replace any previous 'null' versioned object
					// but not one with a version id.
					versionId = WalrusProperties.NULL_VERSION_ID;
					ObjectInfo searchObject = new ObjectInfo(bucketName, objectKey);
					searchObject.setVersionId(versionId);
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					try {
						ObjectInfo foundObject = dbObject.getUniqueEscape(searchObject);
						if (!foundObject.canWrite(account.getAccountNumber())) {
							// Found existing object, but don't have write
							// access
							db.rollback();
							messenger.removeQueue(key, randomKey);
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						objectName = foundObject.getObjectName();
						oldObjectSize = foundObject.getSize() == null ? 0L : foundObject.getSize();
						// Fix for EUCA-2275:
						// If an existing object is overwritten, the size
						// difference must be taken into account. Size of the
						// already existing object was ignored before
						oldBucketSize = -oldObjectSize;
					} catch (AccessDeniedException ex) {
						throw ex;
					} catch (EucalyptusCloudException ex) {
						// No existing object found
						objectInfo = new ObjectInfo(bucketName, objectKey);
						objectInfo.setOwnerId(account.getAccountNumber());
						List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
						objectInfo.addGrants(account.getAccountNumber(), bucket.getOwnerId(), grantInfos, accessControlList);
						objectInfo.setGrants(grantInfos);
						objectName = UUID.randomUUID().toString();
						objectInfo.setObjectName(objectName);
						objectInfo.setSize(0L);
					}
				}

				String bucketOwnerId = bucket.getOwnerId();
				db.commit();
				// writes are unconditional
				WalrusDataQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);

				try {
					WalrusDataMessage dataMessage;
					String tempObjectName = objectName;
					MessageDigest digest = null;
					long size = 0;
					FileIO fileIO = null;
					while ((dataMessage = putQueue.take()) != null) {
						if (putQueue.getInterrupted()) {
							if (WalrusDataMessage.isEOF(dataMessage)) {
								WalrusMonitor monitor = messenger.getMonitor(key);
								if (monitor.getLastModified() == null) {
									LOG.trace("Monitor wait: " + key + " random: " + randomKey);
									synchronized (monitor) {
										monitor.wait();
									}
								}
								LOG.trace("Monitor resume: " + key + " random: " + randomKey);
								lastModified = monitor.getLastModified();
								md5 = monitor.getMd5();
								// ok we are done here
								if (fileIO != null) {
									fileIO.finish();
								}
								ObjectDeleter objectDeleter = new ObjectDeleter(bucketName,
										tempObjectName,
										null,
										null,
										-1L,
										ctx.getUser().getName(),
										ctx.getUser().getUserId(),
										ctx.getAccount().getName(),
										ctx.getAccount().getAccountNumber());
								Threads.lookup(Walrus.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
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
								fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
							} catch (Exception ex) {
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(ex);
							}
						} else if (WalrusDataMessage.isEOF(dataMessage)) {
							if (digest != null) {
								md5 = Hashes.bytesToHex(digest.digest());
							} else {
								WalrusMonitor monitor = messenger.getMonitor(key);
								md5 = monitor.getMd5();
								lastModified = monitor.getLastModified();
								if (md5 == null) {
									LOG.error("ETag did not match for: " + randomKey + " Computed MD5 is null");
									throw new ContentMismatchException(bucketName + "/" + objectKey);
								}
								break;
							}
							String contentMD5 = request.getContentMD5();
							if (contentMD5 != null) {
								String contentMD5AsHex = Hashes.bytesToHex(Base64.decode(contentMD5));
								if (!contentMD5AsHex.equals(md5)) {
									if (fileIO != null) {
										fileIO.finish();
									}
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									LOG.error("ETag did not match for: " + randomKey + " Expected: " + contentMD5AsHex + " Computed: " + md5);
									throw new ContentMismatchException(bucketName + "/" + objectKey);
								}
							}

							// Fix for EUCA-2275:
							// Moved up policy and bucket size checks on the
							// temporary object. The temp object is committed
							// (renamed) only after it clears the checks.
							// If any of the checks fail, temp object is cleaned
							// up and the process errors out. If the PUT request
							// is overwriting an existing object, the object is
							// left untouched.
							// So the fix ensures proper clean up of temp files
							// (no orphaned files) and does not overwrite
							// existing data when policy or bucket size checks
							// fail

							if (!ctx.hasAdministrativePrivileges()
									&& !Permissions.canAllocate(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, bucketName, PolicySpec.S3_PUTOBJECT,
											ctx.getUser(), oldBucketSize + size)) {
								// dbObject.rollback();
								cleanupTempObject(ctx, bucketName, tempObjectName);
								messenger.removeQueue(key, randomKey);
								LOG.error("Quota exceeded for Walrus putObject");
								throw new EntityTooLargeException("Key", objectKey);
							}
							boolean success = false;
							int retryCount = 0;
							do {
								try {
									incrementBucketSize(bucketName, objectKey, oldBucketSize, size);
									success = true;
								} catch (EntityTooLargeException ex) {
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									// dbObject.rollback();
									throw ex;
								} catch (NoSuchBucketException ex) {
									// dbObject.rollback();
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									throw ex;
								} catch (RollbackException ex) {
									retryCount++;
									LOG.trace("retrying update: " + bucketName);
								} catch (EucalyptusCloudException ex) {
									// dbObject.rollback();
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									throw ex;
								}
							} while (!success && (retryCount < 5));

							// commit object
							try {
								if (fileIO != null) {
									fileIO.finish();
								}
								storageManager.renameObject(bucketName, tempObjectName, objectName);
							} catch (IOException ex) {
								LOG.error(ex);
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(objectKey);
							}
							lastModified = new Date();
							ObjectInfo searchObject = new ObjectInfo(bucketName, objectKey);
							searchObject.setVersionId(versionId);
							EntityWrapper<ObjectInfo> dbObject = EntityWrapper.get(ObjectInfo.class);
							ObjectInfo foundObject;
							try {
								foundObject = dbObject.getUniqueEscape(searchObject);
								// If its a delete marker, fall through the administrative privileges and ACP check
								if (foundObject.getDeleted() || ctx.hasAdministrativePrivileges() || foundObject.canWriteACP(account.getAccountNumber())) {
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
									foundObject.addGrants(account.getAccountNumber(), bucketOwnerId, grantInfos, accessControlList);
									foundObject.setGrants(grantInfos);
								}
								if (WalrusProperties.enableTorrents) {
									EntityWrapper<TorrentInfo> dbTorrent = dbObject.recast(TorrentInfo.class);
									TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
									List<TorrentInfo> torrentInfos = dbTorrent.queryEscape(torrentInfo);
									if (torrentInfos.size() > 0) {
										TorrentInfo foundTorrentInfo = torrentInfos.get(0);
										TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
										if (torrentClient != null) {
											torrentClient.bye();
										}
										dbTorrent.delete(foundTorrentInfo);
									}
								} else {
									LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
								}
							} catch (EucalyptusCloudException ex) {
								if (objectInfo != null) {
									foundObject = objectInfo;
								} else {
									dbObject.rollback();
									throw new EucalyptusCloudException("Unable to update object: " + bucketName + "/" + objectKey);
								}
							}
							foundObject.setVersionId(versionId);
							foundObject.replaceMetaData(request.getMetaData());
							foundObject.setEtag(md5);
							foundObject.setSize(size);
							foundObject.setLastModified(lastModified);
							foundObject.setStorageClass("STANDARD");
							foundObject.setContentType(request.getContentType());
							foundObject.setContentDisposition(request.getContentDisposition());
							foundObject.setLast(true);
							foundObject.setDeleted(false);
							reply.setSize(size);
							if (logData != null) {
								logData.setObjectSize(size);
								updateLogData(bucket, logData);
							}
							if (objectInfo != null) {
								dbObject.add(foundObject);
							}
							success = false;
							try {
								dbObject.commit();
								success = true;
							} catch (RollbackException ex) {
								dbObject.rollback();
								LOG.error(ex, ex);
							}

							dbObject = EntityWrapper.get(ObjectInfo.class);
							List<ObjectInfo> objectInfos = dbObject.queryEscape(new ObjectInfo(bucketName, objectKey));
							for (ObjectInfo objInfo : objectInfos) {
								if (!success) {
									if (objInfo.getLast()) {
										lastModified = objInfo.getLastModified();
										md5 = objInfo.getEtag();
									}
									success = true;
								}
								if (!versionId.equals(objInfo.getVersionId())) {
									objInfo.setLast(false);
								}
							}
							dbObject.commit();

							if (logData != null) {
								logData.setTurnAroundTime(Long.parseLong(new String(dataMessage.getPayload())));
							}
							// restart all interrupted puts
							WalrusMonitor monitor = messenger.getMonitor(key);
							synchronized (monitor) {
								monitor.setLastModified(lastModified);
								monitor.setMd5(md5);
								monitor.notifyAll();
							}
							// messenger.removeMonitor(key);
							messenger.clearQueues(key);
							messenger.removeQueue(key, randomKey);
							LOG.info("Transfer complete: " + key);

							try {
								fireObjectCreationEvent(bucketName, objectKey, versionId, ctx.getUser().getUserId(), size, oldObjectSize);
							} catch (Exception ex) {
								LOG.debug("Failed to fire reporting event for walrus PUT object operation", ex);
							}

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
							if (digest != null) {
								digest.update(data);
							}
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
		} else {
			db.rollback();
			messenger.removeQueue(key, randomKey);
			throw new NoSuchBucketException(bucketName);
		}

		reply.setEtag(md5);
		reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN));
		return reply;
	}

	private void cleanupTempObject(Context ctx, String bucketName,
			String tempObjectName) {
		ObjectDeleter objectDeleter = new ObjectDeleter(bucketName,
				tempObjectName,
				null,
				null,
				-1L,
				ctx.getUser().getName(),
				ctx.getUser().getUserId(),
				ctx.getAccount().getName(),
				ctx.getAccount().getAccountNumber());
		Threads.lookup(Walrus.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
	}

	private void incrementBucketSize(String bucketName, String objectKey, Long oldBucketSize, Long size) throws EucalyptusCloudException, RollbackException,
			NoSuchBucketException, EntityTooLargeException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo searchBucket = new BucketInfo(bucketName);
			BucketInfo bucket = null;
			try {
				bucket = db.getUniqueEscape(searchBucket);
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex);
				throw new NoSuchBucketException(bucketName);
			}
			Long bucketSize = bucket.getBucketSize();
			long newSize = bucketSize + oldBucketSize + size;
			if (WalrusProperties.shouldEnforceUsageLimits && !Contexts.lookup().hasAdministrativePrivileges()) {
				if (newSize > (WalrusInfo.getWalrusInfo().getStorageMaxBucketSizeInMB() * WalrusProperties.M)) {
					throw new EntityTooLargeException("Key", objectKey);
				}
			}
			bucket.setBucketSize(newSize);
			db.commit();
		} catch (RollbackException ex) {
			throw ex;
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw ex;
		}
	}

	@Override
	public PostObjectResponseType postObject(PostObjectType request)
			throws EucalyptusCloudException {
		PostObjectResponseType reply = (PostObjectResponseType) request
				.getReply();

		String bucketName = request.getBucket();
		String key = request.getKey();

		PutObjectType putObject = new PutObjectType();
		putObject.setUserId(Contexts.lookup().getUserFullName().getUserId());
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
			String paramString = "bucket=" + bucketName + "&key=" + key + "&etag=quot;" + etag + "quot;";
			reply.setRedirectUrl(successActionRedirect + "?" + paramString);
		} else {
			Integer successActionStatus = request.getSuccessActionStatus();
			if (successActionStatus != null) {
				if ((successActionStatus == 200) || (successActionStatus == 201)) {
					reply.setSuccessCode(successActionStatus);
					if (successActionStatus == 200) {
						return reply;
					} else {
						reply.setBucket(bucketName);
						reply.setKey(key);
						reply.setLocation(Topology.lookup(Walrus.class).getUri().getHost() + "/" + bucketName + "/" + key);
					}
				} else {
					reply.setSuccessCode(204);
					return reply;
				}
			}
		}
		return reply;
	}

	@Override
	public PutObjectInlineResponseType putObjectInline(
			PutObjectInlineType request) throws EucalyptusCloudException {
		PutObjectInlineResponseType reply = (PutObjectInlineResponseType) request
				.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		String md5 = "";
		Long oldBucketSize = 0L;
		Date lastModified;

		AccessControlList accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlList();
		}

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			long objSize = 0;
			try {
				objSize = Long.valueOf(request.getContentLength());
			} catch (NumberFormatException e) {
				LOG.error("Invalid content length " + request.getContentLength());
				// TODO(wenye): should handle this properly.
				objSize = 1L;
			}
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);

				ObjectInfo foundObject = null;
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				for (ObjectInfo objectInfo : objectInfos) {
					if (objectInfo.getObjectKey().equals(objectKey)) {
						// key (object) exists. check perms
						if (!objectInfo.canWrite(account.getAccountNumber())) {
							db.rollback();
							throw new AccessDeniedException("Key", objectKey, logData);
						}
						foundObject = objectInfo;
						oldBucketSize = -foundObject.getSize();
						break;
					}
				}
				// write object to bucket
				String objectName;
				Long oldObjectSize = 0L;
				if (foundObject == null) {
					// not found. create an object info
					foundObject = new ObjectInfo(bucketName, objectKey);
					foundObject.setOwnerId(account.getAccountNumber());
					List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
					foundObject.addGrants(account.getAccountNumber(), bucket.getOwnerId(), grantInfos, accessControlList);
					foundObject.setGrants(grantInfos);
					objectName = UUID.randomUUID().toString();
					foundObject.setObjectName(objectName);
					dbObject.add(foundObject);
				} else {
					// object already exists. see if we can modify acl
					if (ctx.hasAdministrativePrivileges() || foundObject.canWriteACP(account.getAccountNumber())) {
						List<GrantInfo> grantInfos = foundObject.getGrants();
						foundObject.addGrants(account.getAccountNumber(), bucket.getOwnerId(), grantInfos, accessControlList);
					}
					objectName = foundObject.getObjectName();
					oldObjectSize = foundObject.getSize();
				}
				foundObject.setObjectKey(objectKey);
				try {
					// writes are unconditional
					if (request.getBase64Data().getBytes().length > WalrusProperties.MAX_INLINE_DATA_SIZE) {
						db.rollback();
						throw new InlineDataTooLargeException(bucketName + "/" + objectKey);
					}
					byte[] base64Data = Hashes.base64decode(request.getBase64Data()).getBytes();
					foundObject.setObjectName(objectName);
					Long size = (long) base64Data.length;

					// Fix for EUCA-2275:
					// Moved up policy and bucket size checks on the temporary
					// object. The object is committed (written) only after it
					// clears the checks.
					// So the fix ensures that no files are orphaned and does
					// not overwrite existing data when policy or bucket size
					// checks fail

					if (!ctx.hasAdministrativePrivileges()
							&& !Permissions.canAllocate(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, bucketName, PolicySpec.S3_PUTOBJECT,
									ctx.getUser(), oldBucketSize + size)) {
						db.rollback();
						LOG.error("Quota exceeded in Walrus putObject");
						throw new EntityTooLargeException("Key", objectKey, logData);
					}
					boolean success = false;
					int retryCount = 0;
					do {
						try {
							incrementBucketSize(bucketName, objectKey, oldBucketSize, size);
							success = true;
						} catch (EntityTooLargeException ex) {
							db.rollback();
							throw ex;
						} catch (NoSuchBucketException ex) {
							db.rollback();
							throw ex;
						} catch (RollbackException ex) {
							retryCount++;
							LOG.trace("retrying update: " + bucketName);
						} catch (EucalyptusCloudException ex) {
							db.rollback();
							throw ex;
						}
					} while (!success && (retryCount < 5));

					try {
						FileIO fileIO = storageManager.prepareForWrite(bucketName, objectName);
						if (fileIO != null) {
							fileIO.write(base64Data);
							fileIO.finish();
						}
					} catch (Exception ex) {
						db.rollback();
						throw new EucalyptusCloudException(ex);
					}
					md5 = Hashes.getHexString(Digest.MD5.get().digest(base64Data));
					foundObject.setEtag(md5);
					foundObject.setSize(size);
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

					try {
						fireObjectCreationEvent(foundObject.getBucketName(), foundObject.getObjectKey(), foundObject.getVersionId(), ctx.getUser().getUserId(),
								foundObject.getSize(), oldObjectSize);
					} catch (Exception ex) {
						LOG.debug("Failed to fire reporting event for walrus inline PUT object operation", ex);
					}

					/* SOAP */
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
		reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN));
		return reply;
	}

	@Override
	public AddObjectResponseType addObject(AddObjectType request)
			throws EucalyptusCloudException {

		AddObjectResponseType reply = (AddObjectResponseType) request.getReply();
		String bucketName = request.getBucket();
		String key = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String objectName = request.getObjectName();

		AccessControlList accessControlList = request.getAccessControlList();
		if (accessControlList == null) {
			accessControlList = new AccessControlList();
		}

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucket.getBucketName(), null)))) {
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo();
				searchObjectInfo.setBucketName(bucketName);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				for (ObjectInfo objectInfo : objectInfos) {
					if (objectInfo.getObjectKey().equals(key)) {
						// key (object) exists.
						db.rollback();
						throw new EucalyptusCloudException("object already exists " + key);
					}
				}
				// write object to bucket
				ObjectInfo objectInfo = new ObjectInfo(bucketName, key);
				objectInfo.setObjectName(objectName);
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.addGrants(account.getAccountNumber(), bucket.getOwnerId(), grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);
				dbObject.add(objectInfo);

				objectInfo.setObjectKey(key);
				objectInfo.setOwnerId(account.getAccountNumber());
				objectInfo.setSize(storageManager.getSize(bucketName, objectName));
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

	@Override
	public DeleteObjectResponseType deleteObject(DeleteObjectType request)
			throws EucalyptusCloudException {
		DeleteObjectResponseType reply = (DeleteObjectResponseType) request
				.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfos = new BucketInfo(bucketName);
			BucketInfo bucketInfo = null;
			try {
				bucketInfo = db.uniqueResultEscape(bucketInfos);
			} catch(NoSuchElementException e) {
				//Nothing to do, object cannot exist if bucket does not.
				bucketInfo = null;
			} catch(TransactionException e) {
				LOG.error("Transaction error looking up bucket: " + bucketName,e);
				throw new EucalyptusCloudException(e);
			}

			if(bucketInfo != null) {
				BucketLogData logData = bucketInfo.getLoggingEnabled() ? request.getLogData() : null;
				if (ctx.hasAdministrativePrivileges()
						|| (bucketInfo.canWrite(account.getAccountNumber()) && (bucketInfo.isGlobalWrite() || Lookups
								.checkPrivilege(PolicySpec.S3_DELETEOBJECT, PolicySpec.VENDOR_S3,
										PolicySpec.S3_RESOURCE_BUCKET, bucketInfo.getBucketName(), null)))) {

					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);

					if (bucketInfo.isVersioningEnabled()) {
						// Versioning is enabled, look for delete marker. If one
						// is
						// present, do nothing. Otherwise place delete marker.
						ObjectInfo searchDeletedObjectInfo = new ObjectInfo(bucketName, objectKey);
						searchDeletedObjectInfo.setDeleted(true);
						searchDeletedObjectInfo.setLast(true);
						try {
							dbObject.getUniqueEscape(searchDeletedObjectInfo);
							db.rollback();
							// Delete marker already exists, nothing to do here
							LOG.debug("Object " + objectKey + " has a delete marker in bucket " + bucketName
									+ " that is marked latest. Nothing to delete");
						} catch (NoSuchEntityException ex) {
							// No such key found, nothing to do here
							LOG.debug("Object " + objectKey + " not found in bucket " + bucketName
									+ ". Nothing to delete");
						} catch (EucalyptusCloudException ex) {
							ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
							searchObjectInfo.setLast(true);
							List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
							for (ObjectInfo objInfo : objectInfos) {
								objInfo.setLast(false);
							}

							// Add the delete marker
							ObjectInfo deleteMarker = new ObjectInfo(bucketName, objectKey);
							deleteMarker.setDeleted(true);
							deleteMarker.setLast(true);
							deleteMarker.setOwnerId(account.getAccountNumber());
							deleteMarker.setLastModified(new Date());
							deleteMarker.setVersionId(UUID.randomUUID().toString().replaceAll("-", ""));
							dbObject.add(deleteMarker);
						}
					} else {
						/*
						 * Versioning disabled or suspended.
						 * 
						 * Only delete 'null' versioned objects. If versioning
						 * is suspended then insert a delete marker. If
						 * versioning is suspended and no 'null' version object
						 * exists then simply insert a delete marker
						 */
						ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
						// searchObjectInfo.setVersionId(WalrusProperties.NULL_VERSION_ID);
						searchObjectInfo.setLast(true);
						searchObjectInfo.setDeleted(false);
						List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);

						if (objectInfos.size() > 0) {
							if (objectInfos.size() > 1) {
								// This shouldn't happen, so bail if it does
								db.rollback();
								throw new EucalyptusCloudException("More than one object set to 'last' found");
							}
							ObjectInfo lastObject = objectInfos.get(0);
							if (lastObject.getVersionId().equals(WalrusProperties.NULL_VERSION_ID)) {
								// Remove the 'null' versioned object
								ObjectInfo nullObject = lastObject;
								dbObject.delete(nullObject);
								String objectName = nullObject.getObjectName();
								for (GrantInfo grantInfo : nullObject.getGrants()) {
									db.delete(grantInfo);
								}
								Long size = nullObject.getSize();
								boolean success = false;
								int retryCount = 0;
								do {
									try {
										decrementBucketSize(bucketName, size);
										success = true;
									} catch (NoSuchBucketException ex) {
										db.rollback();
										throw ex;
									} catch (RollbackException ex) {
										retryCount++;
										LOG.trace("retrying update: " + bucketName);
									} catch (EucalyptusCloudException ex) {
										db.rollback();
										throw ex;
									}
								} while (!success && (retryCount < 5));
								ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, objectName, objectKey,
										WalrusProperties.NULL_VERSION_ID, size, ctx.getUser().getName(), ctx.getUser()
												.getUserId(), ctx.getAccount().getName(), ctx.getAccount()
												.getAccountNumber());
								Threads.lookup(Walrus.class, WalrusFSManager.ObjectDeleter.class).limitTo(10)
										.submit(objectDeleter);
							} else {
								if (bucketInfo.isVersioningSuspended()) {
									// Some version found, don't delete it, just
									// make it not last. This is possible when
									// versioning was suspended and no object
									// uploaded since then
									lastObject.setLast(false);
								} else {
									db.rollback();
									throw new EucalyptusCloudException(
											"Non 'null' versioned object found in a versioning disabled bucket, not sure how to proceed with delete.");
								}
							}

							if (logData != null) {
								updateLogData(bucketInfo, logData);
								reply.setLogData(logData);
							}

							if (bucketInfo.isVersioningSuspended()) {
								// Add the delete marker with null versioning ID
								ObjectInfo deleteMarker = new ObjectInfo(bucketName, objectKey);
								deleteMarker.setDeleted(true);
								deleteMarker.setLast(true);
								deleteMarker.setOwnerId(account.getAccountNumber());
								deleteMarker.setLastModified(new Date());
								deleteMarker.setVersionId(WalrusProperties.NULL_VERSION_ID);
								deleteMarker.setSize(0L);
								dbObject.add(deleteMarker);
							}
						} else {
							// No 'last' record found that isn't 'deleted'
							LOG.debug("Object " + objectKey + " not found in bucket " + bucketName
									+ ". Nothing to delete");
						}
					}
				} else {
					throw new AccessDeniedException("Bucket", bucketName, logData);
				}
			} else {
				throw new NoSuchBucketException(bucketName);
			}
			// In either case, set the response to 204 NO CONTENT
			reply.setStatus(HttpResponseStatus.NO_CONTENT);
			reply.setStatusMessage("NO CONTENT");
			db.commit();
			return reply;
		} catch(EucalyptusCloudException e) {
			LOG.error("DeleteObject operation for " + bucketName + "/" + objectKey + " failed with: " + e.getMessage());
			throw e;
		} finally {
			if(db != null && db.isActive()) {
				db.rollback();
			}
		}
	}

	private void decrementBucketSize(String bucketName, Long size) throws EucalyptusCloudException, RollbackException, NoSuchBucketException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo searchBucket = new BucketInfo(bucketName);
			BucketInfo bucket = null;
			try {
				bucket = db.getUniqueEscape(searchBucket);
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex);
				throw new NoSuchBucketException(bucketName);
			}
			Long bucketSize = bucket.getBucketSize();
			long newSize = bucketSize - size;
			bucket.setBucketSize(newSize);
			db.commit();
		} catch (RollbackException ex) {
			throw ex;
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw ex;
		}
	}

	private class ObjectDeleter implements Runnable {
		final String bucketName;
		final String objectName;
		final String objectKey;
		final String version;
		final Long size;
		final String user;
		final String userId;
		final String account;
		final String accountNumber;

		public ObjectDeleter(String bucketName, String objectName, String objectKey, String version, Long size, String user, String userId, String account,
				String accountNumber) {
			this.bucketName = bucketName;
			this.objectName = objectName;
			this.objectKey = objectKey;
			this.version = version;
			this.size = size;
			this.user = user;
			this.userId = userId;
			this.account = account;
			this.accountNumber = accountNumber;
		}

		public void run() {
			try {
				storageManager.deleteObject(bucketName, objectName);
				if (WalrusProperties.trackUsageStatistics && (size > 0))

					/* Send an event to reporting to report this S3 usage. */
					if (size > 0) {
						try {
							fireObjectUsageEvent(S3ObjectAction.OBJECTDELETE, this.bucketName, this.objectKey, this.version, this.userId, this.size);
						} catch (Exception ex) {
							LOG.debug("Failed to fire reporting event for walrus DELETE object operation", ex);
						}
					}
			} catch (Exception ex) {
				LOG.error(ex, ex);
			}
		}
	}

	@Override
	public ListBucketResponseType listBucket(ListBucketType request) throws EucalyptusCloudException {
		ListBucketResponseType reply = (ListBucketResponseType) request.getReply();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);

		try {
			String bucketName = request.getBucket();
			BucketInfo bucketInfo = new BucketInfo(bucketName);
			bucketInfo.setHidden(false);
			List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

			Context ctx = Contexts.lookup();
			Account account = ctx.getAccount();

			int maxKeys = -1;
			String maxKeysString = request.getMaxKeys();
			if (maxKeysString != null) {
				maxKeys = Integer.parseInt(maxKeysString);
				if (maxKeys < 0) {
					throw new InvalidArgumentException("max-keys", "Argument max-keys must be an integer between 0 and " + Integer.MAX_VALUE);
				}
			} else {
				maxKeys = WalrusProperties.MAX_KEYS;
			}

			if (bucketList.size() > 0) {
				BucketInfo bucket = bucketList.get(0);
				BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
				if (ctx.hasAdministrativePrivileges()
						|| (bucket.canRead(account.getAccountNumber()) && (bucket.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_LISTBUCKET,
								PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
					if (logData != null) {
						updateLogData(bucket, logData);
						reply.setLogData(logData);
					}

					if (Contexts.lookup().hasAdministrativePrivileges()) {
						try {
							if (bucketHasSnapshots(bucketName)) {
								db.rollback();
								throw new NoSuchBucketException(bucketName);
							}
						} catch (Exception e) {
							db.rollback();
							throw new EucalyptusCloudException(e);
						}
					}

					String prefix = request.getPrefix();
					String delimiter = request.getDelimiter();
					String marker = request.getMarker();

					reply.setName(bucketName);
					reply.setIsTruncated(false);
					reply.setPrefix(prefix);
					reply.setMarker(marker);
					reply.setDelimiter(delimiter);
					reply.setMaxKeys(maxKeys);

					if (maxKeys == 0) {
						// No keys requested, so just return
						reply.setContents(new ArrayList<ListEntry>());
						reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());
						db.commit();
						return reply;
					}

					final int queryStrideSize = maxKeys + 1;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);

					ObjectInfo searchObj = new ObjectInfo();
					searchObj.setBucketName(bucketName);
					searchObj.setLast(true);
					searchObj.setDeleted(false);

					Criteria objCriteria = dbObject.createCriteria(ObjectInfo.class);
					objCriteria.add(Example.create(searchObj));
					objCriteria.addOrder(Order.asc("objectKey"));
					objCriteria.setMaxResults(queryStrideSize); // add one to, hopefully, indicate truncation in one call

					if (!Strings.isNullOrEmpty(marker)) {
						// The result set should be exclusive of the marker. marker could be a common prefix from a previous response. Look for keys that
						// lexicographically follow the marker and don't contain the marker as the prefix.
						objCriteria.add(Restrictions.gt("objectKey", marker));
					} else {
						marker = "";
					}

					if (!Strings.isNullOrEmpty(prefix)) {
						objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
					} else {
						prefix = "";
					}

					// Ensure not null.
					if (Strings.isNullOrEmpty(delimiter)) {
						delimiter = "";
					}

					List<ObjectInfo> objectInfos = null;
					int resultKeyCount = 0;
					ArrayList<ListEntry> contents = new ArrayList<ListEntry>(); // contents for reply
					String nextMarker = null;
					TreeSet<String> commonPrefixes = new TreeSet<String>();
					int firstResult = -1;

					// Iterate over result sets of size maxkeys + 1
					do {
						// Start listing from the 0th element and increment the first element to be listed by the query size
						objCriteria.setFirstResult(queryStrideSize * (++firstResult));
						objectInfos = (List<ObjectInfo>) objCriteria.list();

						if (objectInfos.size() > 0) {
							for (ObjectInfo objectInfo : objectInfos) {
								String objectKey = objectInfo.getObjectKey();

								// Check if it will get aggregated as a commonprefix
								if (!Strings.isNullOrEmpty(delimiter)) {
									String[] parts = objectKey.substring(prefix.length()).split(delimiter);
									if (parts.length > 1) {
										String prefixString = prefix + parts[0] + delimiter;
										if (!StringUtils.equals(prefixString, marker) && !commonPrefixes.contains(prefixString)) {
											if (resultKeyCount == maxKeys) {
												// This is a new record, so we know we're truncating if this is true
												reply.setNextMarker(nextMarker);
												reply.setIsTruncated(true);
												resultKeyCount++;
												break;
											}

											commonPrefixes.add(prefixString);
											resultKeyCount++; // count the unique commonprefix as a single return entry

											// If max keys have been collected, set the next-marker. It might be needed for the response if the list is
											// truncated
											// If the common prefixes hit the limit set by max-keys, next-marker is the last common prefix
											if (resultKeyCount == maxKeys) {
												nextMarker = prefixString;
											}
										}
										continue;
									}
								}

								if (resultKeyCount == maxKeys) {
									// This is a new (non-commonprefix) record, so we know we're truncating
									reply.setNextMarker(nextMarker);
									reply.setIsTruncated(true);
									resultKeyCount++;
									break;
								}

								// Process the entry as a full key listing
								ListEntry listEntry = new ListEntry();
								listEntry.setKey(objectKey);
								listEntry.setEtag(objectInfo.getEtag());
								listEntry.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ALT_ISO8601_DATE_PATTERN));
								listEntry.setStorageClass(objectInfo.getStorageClass());
								listEntry.setSize(objectInfo.getSize());
								listEntry.setStorageClass(objectInfo.getStorageClass());
								try {
									Account ownerAccount = Accounts.lookupAccountById(objectInfo.getOwnerId());
									listEntry.setOwner(new CanonicalUser(ownerAccount.getCanonicalId(), ownerAccount.getName()));
								} catch (AuthException e) {
									db.rollback();
									throw new AccessDeniedException("Bucket", bucketName, logData);
								}
								contents.add(listEntry);

								resultKeyCount++;

								// If max keys have been collected, set the next-marker. It might be needed for the response if the list is truncated
								if (resultKeyCount == maxKeys) {
									nextMarker = objectKey;
								}
							}
						}

						if (resultKeyCount <= maxKeys && objectInfos.size() <= maxKeys) {
							break;
						}
					} while (resultKeyCount <= maxKeys);

					reply.setContents(contents);

					// Prefixes are already sorted, add them to the proper data structures and populate the reply
					if (!commonPrefixes.isEmpty()) {
						ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
						for (String prefixEntry : commonPrefixes) {
							commonPrefixesList.add(new CommonPrefixesEntry(prefixEntry));
						}
						reply.setCommonPrefixesList(commonPrefixesList);
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
		} finally {
			if (db.isActive()) {
				db.rollback();
			}
		}
	}

	/*
	 * Build a grant list from a list of GrantInfos. Will add 'invalid' grants if they are in the list.
	 */
	private void addGrants(ArrayList<Grant> grants, List<GrantInfo> grantInfos) {
		if (grantInfos == null) {
			return;
		}

		if (grants == null) {
			grants = new ArrayList<Grant>();
		}

		String uId = null;
		for (GrantInfo grantInfo : grantInfos) {
			uId = grantInfo.getUserId();
			try {
				if (grantInfo.getGrantGroup() != null) {
					// Add it as a group
					addPermission(grants, grantInfo);
				} else {
					// Assume it's a user/account
					addPermission(grants, Accounts.lookupAccountById(uId), grantInfo);
				}
			} catch (AuthException e) {
				LOG.debug(e, e);

				try {
					addPermission(grants, new CanonicalUser(uId, ""), grantInfo);
				} catch (AuthException ex) {
					LOG.debug(ex, ex);
					continue;
				}
			}
		}
	}

	@Override
	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
			GetObjectAccessControlPolicyType request)
			throws EucalyptusCloudException {
		GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request
				.getReply();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String ownerId = null;

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
		BucketLogData logData;

		AccessControlList accessControlList = new AccessControlList();
		if (bucketList.size() > 0) {
			// construct access control policy from grant infos
			BucketInfo bucket = bucketList.get(0);

			logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			searchObjectInfo.setVersionId(request.getVersionId());
			if (request.getVersionId() == null) {
				searchObjectInfo.setLast(true);
			}
			searchObjectInfo.setDeleted(false);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (ctx.hasAdministrativePrivileges()
						|| (objectInfo.canReadACP(account.getAccountNumber()) && (objectInfo.isGlobalReadACP() || Lookups.checkPrivilege(
								PolicySpec.S3_GETOBJECTACL, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(bucketName, objectKey), null)))) {
					if (logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(objectInfo.getSize());
						reply.setLogData(logData);
					}

					ownerId = objectInfo.getOwnerId();
					ArrayList<Grant> grants = new ArrayList<Grant>();
					List<GrantInfo> grantInfos = objectInfo.getGrants();
					objectInfo.readPermissions(grants);
					addGrants(grants, grantInfos);

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

		AccessControlPolicy accessControlPolicy = new AccessControlPolicy();
		try {
			Account ownerInfo = Accounts.lookupAccountById(ownerId);
			accessControlPolicy.setOwner(new CanonicalUser(ownerInfo.getCanonicalId(), ownerInfo.getName()));
			accessControlPolicy.setAccessControlList(accessControlList);
		} catch (AuthException e) {
			throw new AccessDeniedException("Key", objectKey, logData);
		}
		reply.setAccessControlPolicy(accessControlPolicy);
		db.commit();
		return reply;
	}

	private void fixCanonicalIds(AccessControlList accessControlList, boolean isBucket, String name) throws AccessDeniedException {
		// need to change grantees to be accountIds
		List<Grant> grants = accessControlList.getGrants();
		if (grants != null && grants.size() > 0) {
			for (Grant grant : grants) {
				Account grantAccount = null;
				Grantee grantee = grant.getGrantee();
				if (grantee != null && grantee.getCanonicalUser() != null && grantee.getCanonicalUser().getID() != null) {
					CanonicalUser canonicalUserType = grantee.getCanonicalUser();
					String canonicalUserTypeId = canonicalUserType.getID();
					try {
						grantAccount = Accounts.lookupAccountById(canonicalUserTypeId);
					} catch (AuthException e) {
						// grant must not be using accountId
					}
					if (grantAccount == null) {
						try {
							grantAccount = Accounts.lookupAccountByCanonicalId(canonicalUserTypeId);
						} catch (AuthException e) {
							// grant must not be using accountId
						}
					}
					if (grantAccount == null) {
						try {
							if (canonicalUserTypeId != null && canonicalUserTypeId.contains("@")) {
								User user = Accounts.lookupUserByEmailAddress(canonicalUserType.getID());
								if (user.isAccountAdmin()) {
									grantAccount = user.getAccount();
								}
							} else {
								LOG.error("attempted to find account with id " + canonicalUserTypeId + " as account id or canonical id, but an account"
										+ " was not found");
								if (isBucket) {
									throw new AccessDeniedException("Bucket", name);
								} else {
									throw new AccessDeniedException("Key", name);
								}
							}
						} catch (Exception ex) {
							LOG.error("attempted to find account with id " + canonicalUserType.getID()
									+ " as account id, canonical id and email address, but an account" + " was not found");
							if (isBucket) {
								throw new AccessDeniedException("Bucket", name);
							} else {
								throw new AccessDeniedException("Key", name);
							}
						}
					}
					grantee.getCanonicalUser().setID(grantAccount.getAccountNumber());
				}
			}
		}
	}

	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException {
		SetBucketAccessControlPolicyResponseType reply = (SetBucketAccessControlPolicyResponseType) request.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		AccessControlList accessControlList = request.getAccessControlList();
		String bucketName = request.getBucket();
		if (accessControlList == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		} else {
			fixCanonicalIds(accessControlList, true, bucketName);
		}
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canWriteACP(account.getAccountNumber()) && (bucket.isGlobalWriteACP() || Lookups.checkPrivilege(PolicySpec.S3_PUTBUCKETACL,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				String invalidValue = this.findInvalidGrant(accessControlList.getGrants());
				if (invalidValue != null) {
					db.rollback();
					throw new WalrusException("InvalidArgument", "Invalid canned-acl or grant list permission: " + invalidValue, "Bucket",
							bucket.getBucketName(), HttpResponseStatus.BAD_REQUEST);
				}
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				bucket.resetGlobalGrants();
				bucket.addGrants(bucket.getOwnerId(), grantInfos, accessControlList);
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

	@Override
	public SetRESTBucketAccessControlPolicyResponseType setRESTBucketAccessControlPolicy(
			SetRESTBucketAccessControlPolicyType request)
			throws EucalyptusCloudException {
		SetRESTBucketAccessControlPolicyResponseType reply = (SetRESTBucketAccessControlPolicyResponseType) request.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		AccessControlPolicy accessControlPolicy = request.getAccessControlPolicy();
		AccessControlList accessControlList = null;
		String bucketName = request.getBucket();
		if (accessControlPolicy == null) {
			throw new AccessDeniedException("Bucket", bucketName);
		} else {
			// need to change grantees to be accountIds
			accessControlList = accessControlPolicy.getAccessControlList();
			fixCanonicalIds(accessControlList, true, bucketName);
		}

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canWriteACP(account.getAccountNumber()) && (bucket.isGlobalWriteACP() || Lookups.checkPrivilege(PolicySpec.S3_PUTBUCKETACL,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				String invalidValue = this.findInvalidGrant(accessControlList.getGrants());
				if (invalidValue != null) {
					db.rollback();
					throw new WalrusException("InvalidArgument", "Invalid canned-acl or grant list permission: " + invalidValue, "Bucket",
							bucket.getBucketName(), HttpResponseStatus.BAD_REQUEST);
				}
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				bucket.resetGlobalGrants();
				bucket.addGrants(bucket.getOwnerId(), grantInfos, accessControlList);
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

	@Override
	public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
			SetObjectAccessControlPolicyType request)
			throws EucalyptusCloudException {
		SetObjectAccessControlPolicyResponseType reply = (SetObjectAccessControlPolicyResponseType) request
				.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		AccessControlList accessControlList = request
				.getAccessControlList();

		// need to change grantees to be accountIds
		fixCanonicalIds(accessControlList, false, request.getKey());

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			searchObjectInfo.setVersionId(request.getVersionId());
			if (request.getVersionId() == null) {
				searchObjectInfo.setLast(true);
			}

			searchObjectInfo.setDeleted(false);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (!ctx.hasAdministrativePrivileges()
						&& !(objectInfo.canWriteACP(account.getAccountNumber()) && (objectInfo.isGlobalWriteACP() || Lookups.checkPrivilege(
								PolicySpec.S3_PUTOBJECTACL, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(bucketName, objectKey), null)))) {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
				String invalidValue = this.findInvalidGrant(accessControlList.getGrants());
				if (invalidValue != null) {
					db.rollback();
					throw new WalrusException("InvalidArgument", "Invalid canned-acl or grant list permission: " + invalidValue, "Key", objectKey,
							HttpResponseStatus.BAD_REQUEST);
				}
				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.resetGlobalGrants();
				objectInfo.addGrants(objectInfo.getOwnerId(), bucket.getOwnerId(), grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);

				if (WalrusProperties.enableTorrents) {
					if (!objectInfo.isGlobalRead()) {
						EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
						TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
						List<TorrentInfo> torrentInfos = dbTorrent.queryEscape(torrentInfo);
						if (torrentInfos.size() > 0) {
							TorrentInfo foundTorrentInfo = torrentInfos.get(0);
							TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
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

	@Override
	public SetRESTObjectAccessControlPolicyResponseType setRESTObjectAccessControlPolicy(
			SetRESTObjectAccessControlPolicyType request)
			throws EucalyptusCloudException {
		SetRESTObjectAccessControlPolicyResponseType reply = (SetRESTObjectAccessControlPolicyResponseType) request.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		AccessControlPolicy accessControlPolicy = request.getAccessControlPolicy();
		if (accessControlPolicy == null) {
			throw new AccessDeniedException("Key", request.getKey());
		}
		AccessControlList accessControlList = accessControlPolicy.getAccessControlList();

		// need to change grantees to be accountIds
		fixCanonicalIds(accessControlList, false, request.getKey());

		String bucketName = request.getBucket();
		String objectKey = request.getKey();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			searchObjectInfo.setVersionId(request.getVersionId());
			if (request.getVersionId() == null) {
				searchObjectInfo.setLast(true);
			}

			searchObjectInfo.setDeleted(false);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (!ctx.hasAdministrativePrivileges()
						&& !(objectInfo.canWriteACP(account.getAccountNumber()) && (objectInfo.isGlobalWriteACP() || Lookups.checkPrivilege(
								PolicySpec.S3_PUTOBJECTACL, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(bucketName, objectKey), null)))) {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}

				String invalidValue = this.findInvalidGrant(accessControlList.getGrants());
				if (invalidValue != null) {
					db.rollback();
					throw new WalrusException("InvalidArgument", "Invalid canned-acl or grant list permission: " + invalidValue, "Key", objectKey,
							HttpResponseStatus.BAD_REQUEST);
				}

				List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
				objectInfo.resetGlobalGrants();
				objectInfo.addGrants(objectInfo.getOwnerId(), bucket.getOwnerId(), grantInfos, accessControlList);
				objectInfo.setGrants(grantInfos);

				if (WalrusProperties.enableTorrents) {
					if (!objectInfo.isGlobalRead()) {
						EntityWrapper<TorrentInfo> dbTorrent = db.recast(TorrentInfo.class);
						TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
						List<TorrentInfo> torrentInfos = dbTorrent.queryEscape(torrentInfo);
						if (torrentInfos.size() > 0) {
							TorrentInfo foundTorrentInfo = torrentInfos.get(0);
							TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
							if (torrentClient != null) {
								torrentClient.bye();
							}
							dbTorrent.delete(foundTorrentInfo);
						}
					}
				} else {
					LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
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

	@Override
	public GetObjectResponseType getObject(GetObjectType request) throws EucalyptusCloudException {
		GetObjectResponseType reply = (GetObjectResponseType) request.getReply();
		// Must explicitly set to true for streaming large objects.
		reply.setHasStreamingData(false);
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		Boolean deleteAfterGet = request.getDeleteAfterGet();
		if (deleteAfterGet == null) {
			deleteAfterGet = false;
		}

		Boolean getTorrent = request.getGetTorrent();
		if (getTorrent == null) {
			getTorrent = false;
		}

		Boolean getMetaData = request.getGetMetaData();
		if (getMetaData == null) {
			getMetaData = false;
		}

		Boolean getData = request.getGetData();
		if (getData == null) {
			getData = false;
		}

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			boolean versioning = false;
			if (bucket.isVersioningEnabled()) {
				versioning = true;
			}
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			searchObjectInfo.setVersionId(request.getVersionId());
			searchObjectInfo.setDeleted(false);
			if (request.getVersionId() == null) {
				searchObjectInfo.setLast(true);
			}
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);
				if (ctx.hasAdministrativePrivileges()
						|| (objectInfo.canRead(account.getAccountNumber()) && (objectInfo.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
								PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, PolicySpec.objectFullName(bucketName, objectKey), null)))) {
					String objectName = objectInfo.getObjectName();
					DefaultHttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
					if (getMetaData) {
						List<MetaDataInfo> metaDataInfos = objectInfo.getMetaData();
						for (MetaDataInfo metaDataInfo : metaDataInfos) {
							httpResponse.addHeader(WalrusProperties.AMZ_META_HEADER_PREFIX + metaDataInfo.getName(), metaDataInfo.getValue());
						}
					}
					if (getTorrent) {
						if (objectInfo.isGlobalRead()) {
							if (!WalrusProperties.enableTorrents) {
								LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
								throw new EucalyptusCloudException("Torrents disabled");
							}
							EntityWrapper<TorrentInfo> dbTorrent = EntityWrapper.get(TorrentInfo.class);
							TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
							TorrentInfo foundTorrentInfo;
							String absoluteObjectPath = storageManager.getObjectPath(bucketName, objectName);
							try {
								foundTorrentInfo = dbTorrent.getUniqueEscape(torrentInfo);
							} catch (EucalyptusCloudException ex) {
								String torrentFile = objectName + ".torrent";
								String torrentFilePath = storageManager.getObjectPath(bucketName, torrentFile);
								TorrentCreator torrentCreator = new TorrentCreator(absoluteObjectPath, objectKey, objectName, torrentFilePath,
										WalrusProperties.getTrackerUrl());
								try {
									torrentCreator.create();
								} catch (Exception e) {
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
							// send torrent
							String key = bucketName + "." + objectKey;
							String randomKey = key + "." + Hashes.getRandom(10);
							request.setRandomKey(randomKey);

							File torrent = new File(torrentFilePath);
							if (torrent.exists()) {
								Date lastModified = objectInfo.getLastModified();
								db.commit();
								long torrentLength = torrent.length();
								if (logData != null) {
									updateLogData(bucket, logData);
									logData.setObjectSize(torrentLength);
								}
								storageManager.sendObject(request, httpResponse, bucketName, torrentFile, torrentLength, null,
										DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN), "application/x-bittorrent",
										"attachment; filename=" + objectKey + ".torrent;", request.getIsCompressed(), null, logData);

								return null;
							} else {
								// No torrent exists
								db.rollback();
								String errorString = "Could not get torrent file " + torrentFilePath;
								LOG.error(errorString);
								throw new EucalyptusCloudException(errorString);
							}
						} else {
							// No global object read permission
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
								throw new InlineDataTooLargeException(bucketName + "/" + objectKey);
							}
							byte[] bytes = new byte[102400];
							int bytesRead = 0, offset = 0;
							String base64Data = "";
							try {
								FileIO fileIO = storageManager.prepareForRead(bucketName, objectName);
								while ((bytesRead = fileIO.read(offset)) > 0) {
									ByteBuffer buffer = fileIO.getBuffer();
									if (buffer != null) {
										buffer.get(bytes, 0, bytesRead);
										base64Data += new String(bytes, 0, bytesRead);
										offset += bytesRead;
									}
								}
								fileIO.finish();
							} catch (Exception e) {
								LOG.error(e, e);
								throw new EucalyptusCloudException(e);
							}
							reply.setBase64Data(Hashes.base64encode(base64Data));

							// fireUsageEvent For Get Object
						} else {
							reply.setHasStreamingData(true);
							// support for large objects
							storageManager.sendObject(request, httpResponse, bucketName, objectName, size, etag,
									DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN), contentType, contentDisposition,
									request.getIsCompressed(), versionId, logData);

							// fireUsageEvent For Get Object
							return null;
						}
					} else {
						// Request is for headers/metadata only
						storageManager.sendHeaders(request, httpResponse, size, etag,
								DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN), contentType, contentDisposition, versionId,
								logData);
						return null;

					}
					reply.setEtag(etag);
					reply.setLastModified(DateUtils.format(lastModified, DateUtils.RFC822_DATETIME_PATTERN));
					reply.setSize(size);
					reply.setContentType(contentType);
					reply.setContentDisposition(contentDisposition);
					Status status = new Status();
					status.setCode(200);
					status.setDescription("OK");
					reply.setStatus(status);
					return reply;
				} else {
					// Permissions not sufficient
					// Fix for EUCA-2782. Different exceptions are thrown based
					// on the request type so that the downstream logic can
					// differentiate
					db.rollback();
					if (getData) {
						throw new AccessDeniedException("Key", objectKey, logData);
					} else {
						throw new HeadAccessDeniedException("Key", objectKey, logData);
					}
				}
			} else {
				// Key not found
				// Fix for EUCA-2782. Different exceptions are thrown based on
				// the request type so that the downstream logic can
				// differentiate
				db.rollback();
				if (getData) {
					throw new NoSuchEntityException(objectKey);
				} else {
					throw new HeadNoSuchEntityException(objectKey);
				}
			}
		} else {
			// Bucket doesn't exist
			// Fix for EUCA-2782. Different exceptions are thrown based on the
			// request type so that the downstream logic can differentiate
			db.rollback();
			if (getData) {
				throw new NoSuchBucketException(bucketName);
			} else {
				throw new HeadNoSuchBucketException(bucketName);
			}
		}
	}

	@Override
	public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
		GetObjectExtendedResponseType reply = (GetObjectExtendedResponseType) request.getReply();
		Date ifModifiedSince = request.getIfModifiedSince();
		Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
		String ifMatch = request.getIfMatch();
		String ifNoneMatch = request.getIfNoneMatch();
		boolean returnCompleteObjectOnFailure = request.getReturnCompleteObjectOnConditionFailure();

		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		Status status = new Status();

		Boolean getData = request.getGetData();
		if (getData == null) {
			getData = false;
		}

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			boolean versioning = false;
			if (bucket.isVersioningEnabled()) {
				versioning = true;
			}
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo objectInfo = objectInfos.get(0);

				if (ctx.hasAdministrativePrivileges()
						|| (objectInfo.canRead(account.getAccountNumber()) && (objectInfo.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_GETOBJECT,
								PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, PolicySpec.objectFullName(bucketName, objectKey), null)))) {
					String etag = objectInfo.getEtag();
					String objectName = objectInfo.getObjectName();
					Long byteRangeStart = request.getByteRangeStart();
					Long byteRangeEnd = request.getByteRangeEnd();
					DefaultHttpResponse httpResponse = null;
					if (byteRangeStart != null || byteRangeEnd != null) {
						httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
					} else {
						httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
					}
					if (byteRangeStart == null) {
						byteRangeStart = 0L;
					}
					if (byteRangeEnd == null) {
						byteRangeEnd = -1L;
					}

					if (byteRangeEnd == -1 || (byteRangeEnd + 1) > objectInfo.getSize()) {
						byteRangeEnd = objectInfo.getSize() - 1;
					}

					if ((byteRangeStart > objectInfo.getSize()) || (byteRangeStart > byteRangeEnd) || (byteRangeStart < 0 || byteRangeEnd < 0)) {
						db.rollback();
						throw new InvalidRangeException("Range: " + byteRangeStart + "-" + byteRangeEnd + "object: " + bucketName + "/" + objectKey);
					}
					if (ifMatch != null) {
						if (!ifMatch.equals(etag) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new PreconditionFailedException(objectKey + " etag: " + etag);
						}

					}
					if (ifNoneMatch != null) {
						if (ifNoneMatch.equals(etag) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new NotModifiedException(objectKey + " ETag: " + etag);
						}
					}
					Date lastModified = objectInfo.getLastModified();
					if (ifModifiedSince != null) {
						if ((ifModifiedSince.getTime() >= lastModified.getTime()) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new NotModifiedException(objectKey + " LastModified: " + lastModified.toString());
						}
					}
					if (ifUnmodifiedSince != null) {
						if ((ifUnmodifiedSince.getTime() < lastModified.getTime()) && !returnCompleteObjectOnFailure) {
							db.rollback();
							throw new PreconditionFailedException(objectKey + " lastModified: " + lastModified.toString());
						}
					}
					if (request.getGetMetaData()) {
						List<MetaDataInfo> metaDataInfos = objectInfo.getMetaData();
						for (MetaDataInfo metaDataInfo : metaDataInfos) {
							httpResponse.addHeader(WalrusProperties.AMZ_META_HEADER_PREFIX + metaDataInfo.getName(), metaDataInfo.getValue());
						}
					}
					Long size = objectInfo.getSize();
					String contentType = objectInfo.getContentType();
					String contentDisposition = objectInfo.getContentDisposition();
					db.commit();
					if (logData != null) {
						updateLogData(bucket, logData);
						logData.setObjectSize(size);
					}
					String versionId = null;
					if (versioning) {
						versionId = objectInfo.getVersionId() != null ? objectInfo.getVersionId() : WalrusProperties.NULL_VERSION_ID;
					}
					if (request.getGetData()) {
						storageManager.sendObject(request, httpResponse, bucketName, objectName, byteRangeStart, byteRangeEnd + 1, size, etag,
								DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN), contentType, contentDisposition,
								request.getIsCompressed(), versionId, logData);
						// fireUsageEvent For Get Object (we need the size in
						// regards
						// to byteRangeStart : byteRangeEnd +1 do math
						return null;
					} else {
						storageManager.sendHeaders(request, httpResponse, size, etag,
								DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN), contentType, contentDisposition, versionId,
								logData);
						return null;
					}
				} else {
					db.rollback();
					// Fix for EUCA-2782. Different exceptions are thrown based
					// on the request type so that the downstream logic can
					// differentiate
					if (getData) {
						throw new AccessDeniedException("Key", objectKey, logData);
					} else {
						throw new HeadAccessDeniedException("Key", objectKey, logData);
					}
				}
			} else {
				db.rollback();
				// Fix for EUCA-2782. Different exceptions are thrown based on
				// the request type so that the downstream logic can
				// differentiate
				if (getData) {
					throw new NoSuchEntityException(objectKey);
				} else {
					throw new HeadNoSuchEntityException(objectKey);
				}
			}
		} else {
			db.rollback();
			// Fix for EUCA-2782. Different exceptions are thrown based on the
			// request type so that the downstream logic can differentiate
			if (getData) {
				throw new NoSuchBucketException(bucketName);
			} else {
				throw new HeadNoSuchBucketException(bucketName);
			}
		}
	}

	@Override
	public GetBucketLocationResponseType getBucketLocation(
			GetBucketLocationType request) throws EucalyptusCloudException {
		GetBucketLocationResponseType reply = (GetBucketLocationResponseType) request
				.getReply();
		String bucketName = request.getBucket();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			if (ctx.hasAdministrativePrivileges()
					|| (bucket.canRead(account.getAccountNumber()) && (bucket.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_GETBUCKETLOCATION,
							PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				if (logData != null) {
					updateLogData(bucket, logData);
					reply.setLogData(logData);
				}
				String location = bucket.getLocation();
				if (location == null || location.equalsIgnoreCase("US")) {
					reply.setLocationConstraint(null);
				} else {
					reply.setLocationConstraint(location);
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

	@Override
	public CopyObjectResponseType copyObject(CopyObjectType request)
			throws EucalyptusCloudException {
		CopyObjectResponseType reply = (CopyObjectResponseType) request
				.getReply();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String sourceBucket = request.getSourceBucket();
		String sourceKey = request.getSourceObject();
		String sourceVersionId = request.getSourceVersionId();
		String destinationBucket = request.getDestinationBucket();
		String destinationKey = request.getDestinationObject();
		String metadataDirective = request.getMetadataDirective();
		AccessControlList accessControlList = request.getAccessControlList();

		String copyIfMatch = request.getCopySourceIfMatch();
		String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
		Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
		Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();

		if (metadataDirective == null) {
			metadataDirective = "COPY";
		}
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(sourceBucket);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

		if (bucketList.size() > 0) {
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(sourceBucket, sourceKey);
			searchObjectInfo.setVersionId(sourceVersionId);
			if (sourceVersionId == null) {
				searchObjectInfo.setLast(true);
			}
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo sourceObjectInfo = objectInfos.get(0);
				if (ctx.hasAdministrativePrivileges()
						|| (sourceObjectInfo.canRead(account.getAccountNumber()) && (sourceObjectInfo.isGlobalRead() || Lookups.checkPrivilege(
								PolicySpec.S3_GETOBJECT, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
								PolicySpec.objectFullName(sourceBucket, sourceKey), null)))) {
					if (copyIfMatch != null) {
						if (!copyIfMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfMatch: " + copyIfMatch);
						}
					}
					if (copyIfNoneMatch != null) {
						if (copyIfNoneMatch.equals(sourceObjectInfo.getEtag())) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfNoneMatch: " + copyIfNoneMatch);
						}
					}
					if (copyIfUnmodifiedSince != null) {
						long unmodifiedTime = copyIfUnmodifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified().getTime();
						if (unmodifiedTime < objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfUnmodifiedSince: " + copyIfUnmodifiedSince.toString());
						}
					}
					if (copyIfModifiedSince != null) {
						long modifiedTime = copyIfModifiedSince.getTime();
						long objectTime = sourceObjectInfo.getLastModified().getTime();
						if (modifiedTime > objectTime) {
							db.rollback();
							throw new PreconditionFailedException(sourceKey + " CopySourceIfModifiedSince: " + copyIfModifiedSince.toString());
						}
					}
					BucketInfo destinationBucketInfo = new BucketInfo(destinationBucket);
					List<BucketInfo> destinationBuckets = db.queryEscape(destinationBucketInfo);
					if (destinationBuckets.size() > 0) {
						BucketInfo foundDestinationBucketInfo = destinationBuckets.get(0);
						if (ctx.hasAdministrativePrivileges()
								|| (foundDestinationBucketInfo.canWrite(account.getAccountNumber()) && (foundDestinationBucketInfo.isGlobalWrite() || Lookups
										.checkPrivilege(PolicySpec.S3_PUTOBJECT, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, destinationBucket, null)))) {
							// all ok
							Long destinationObjectOldSize = 0L;
							String destinationVersionId = sourceVersionId;
							ObjectInfo destinationObjectInfo = null;
							String destinationObjectName;
							ObjectInfo destSearchObjectInfo = new ObjectInfo(destinationBucket, destinationKey);
							if (foundDestinationBucketInfo.isVersioningEnabled()) {
								destinationVersionId = UUID.randomUUID().toString().replaceAll("-", "");
							} else {
								destinationVersionId = WalrusProperties.NULL_VERSION_ID;
							}
							destSearchObjectInfo.setVersionId(destinationVersionId);
							List<ObjectInfo> destinationObjectInfos = dbObject.queryEscape(destSearchObjectInfo);
							if (destinationObjectInfos.size() > 0) {
								destinationObjectInfo = destinationObjectInfos.get(0);
								// Check privilege only if its not a delete marker, HACK!!
								if (!destinationObjectInfo.getDeleted() && !destinationObjectInfo.canWrite(account.getAccountNumber())) {
									db.rollback();
									throw new AccessDeniedException("Key", destinationKey);
								}
							}
							boolean addNew = false;
							if (destinationObjectInfo == null) {
								// not found. create a new one
								if (ctx.hasAdministrativePrivileges()
										|| (Permissions.isAuthorized(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, sourceBucket, ctx.getAccount(),
												PolicySpec.S3_PUTOBJECT, ctx.getUser()) && Permissions
												.canAllocate(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, sourceBucket, PolicySpec.S3_PUTOBJECT,
														ctx.getUser(), sourceObjectInfo.getSize()))) {
									addNew = true;
									destinationObjectInfo = new ObjectInfo();
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
									destinationObjectInfo.setBucketName(destinationBucket);
									destinationObjectInfo.setObjectKey(destinationKey);
									destinationObjectInfo.addGrants(account.getAccountNumber(), foundDestinationBucketInfo.getOwnerId(), grantInfos,
											accessControlList);
									destinationObjectInfo.setGrants(grantInfos);
									destinationObjectInfo.setObjectName(UUID.randomUUID().toString());
								}
							} else {
								// If its a delete marker, make the same checks as when the object was not found, HACK!!
								if (ctx.hasAdministrativePrivileges()
										|| (destinationObjectInfo.getDeleted()
												&& Permissions.isAuthorized(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, sourceBucket,
														ctx.getAccount(), PolicySpec.S3_PUTOBJECT, ctx.getUser()) && Permissions.canAllocate(
												PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, sourceBucket, PolicySpec.S3_PUTOBJECT, ctx.getUser(),
												sourceObjectInfo.getSize()))
										|| (destinationObjectInfo.canWriteACP(account.getAccountNumber()) && (destinationObjectInfo.isGlobalWriteACP() || Lookups
												.checkPrivilege(PolicySpec.S3_PUTOBJECTACL, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
														PolicySpec.objectFullName(destinationBucket, destinationKey), null)))) {
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
									destinationObjectInfo.addGrants(account.getAccountNumber(), foundDestinationBucketInfo.getOwnerId(), grantInfos,
											accessControlList);
									destinationObjectInfo.setGrants(grantInfos);
								}
								destinationObjectOldSize = destinationObjectInfo.getSize() == null ? 0L : destinationObjectInfo.getSize();
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
							destinationObjectInfo.setVersionId(destinationVersionId);
							destinationObjectInfo.setLast(true);
							destinationObjectInfo.setDeleted(false);
							if (!metadataDirective.equals("REPLACE")) {
								destinationObjectInfo.setMetaData(sourceObjectInfo.cloneMetaData());
							} else {
								List<MetaDataEntry> metaData = request.getMetaData();
								if (metaData != null)
									destinationObjectInfo.replaceMetaData(metaData);
							}

							String sourceObjectName = sourceObjectInfo.getObjectName();
							destinationObjectName = destinationObjectInfo.getObjectName();

							try {
								storageManager.copyObject(sourceBucket, sourceObjectName, destinationBucket, destinationObjectName);
							} catch (Exception ex) {
								LOG.error(ex);
								db.rollback();
								throw new EucalyptusCloudException("Could not rename " + sourceObjectName + " to " + destinationObjectName);
							}
							if (addNew)
								dbObject.add(destinationObjectInfo);

							reply.setEtag(etag);
							reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.ALT_ISO8601_DATE_PATTERN));

							if (foundDestinationBucketInfo.isVersioningEnabled()) {
								reply.setCopySourceVersionId(sourceVersionId);
								reply.setVersionId(destinationVersionId);
							}
							db.commit();

							try {
								// Fixes EUCA-3756. Reporting event for copy
								// objects
								fireObjectCreationEvent(destinationBucket, destinationObjectName, destinationVersionId, ctx.getUser().getUserId(),
										destinationObjectInfo.getSize(), destinationObjectOldSize);
							} catch (Exception ex) {
								LOG.debug("Failed to fire reporting event for walrus COPY object operation", ex);
							}

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

	@Override
	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(
			SetBucketLoggingStatusType request) throws EucalyptusCloudException {
		SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request
				.getReply();
		String bucket = request.getBucket();
		Context ctx = Contexts.lookup();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo, targetBucketInfo;
		try {
			bucketInfo = db.getUniqueEscape(new BucketInfo(bucket));
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}

		if (ctx.hasAdministrativePrivileges()
				|| Lookups.checkPrivilege(PolicySpec.S3_PUTBUCKETLOGGING, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucket, bucketInfo.getOwnerId())) {
			if (request.getLoggingEnabled() != null) {
				String targetBucket = request.getLoggingEnabled().getTargetBucket();
				String targetPrefix = request.getLoggingEnabled().getTargetPrefix();
				List<Grant> targetGrantsList = null;
				TargetGrants targetGrants = request.getLoggingEnabled().getTargetGrants();
				if (targetGrants != null)
					targetGrantsList = targetGrants.getGrants();
				if (targetPrefix == null)
					targetPrefix = "";
				try {
					targetBucketInfo = db.getUniqueEscape(new BucketInfo(targetBucket));
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
		} else {
			LOG.error("Not authorized to set bucket logging status by " + ctx.getUserFullName());
			db.rollback();
			throw new AccessDeniedException("Bucket", bucketInfo.getBucketName());
		}
		db.commit();
		return reply;
	}

	/*
	 * Returns null if grants are valid, otherwise returns the permission/acl string that was invalid
	 */
	private String findInvalidGrant(List<Grant> grants) {
		if (grants != null && grants.size() > 0) {
			String permission = null;
			boolean grantValid = false;
			for (Grant grant : grants) {
				grantValid = false;
				permission = grant.getPermission();

				// Do toString comparisons to be sure that the enum acl value is
				// in the proper form for requests (since '-' is not a valid
				// char in enums)
				for (WalrusProperties.CannedACL cannedACL : WalrusProperties.CannedACL.values()) {
					if (permission.equals(cannedACL.toString())) {
						grantValid = true;
						break;
					}
				}
				// Do toString comparison here to be sure that the enums are
				// translated into the proper values for requests
				for (WalrusProperties.Permission perm : WalrusProperties.Permission.values()) {
					if (permission.equals(perm.toString())) {
						grantValid = true;
						break;
					}
				}

				if (!grantValid) {
					return permission;
				}
			}
		}
		return null;
	}

	@Override
	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(
			GetBucketLoggingStatusType request) throws EucalyptusCloudException {
		GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request
				.getReply();
		String bucket = request.getBucket();
		Context ctx = Contexts.lookup();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfo = db.getUniqueEscape(new BucketInfo(bucket));
			if (ctx.hasAdministrativePrivileges()
					|| Lookups.checkPrivilege(PolicySpec.S3_GETBUCKETLOGGING, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucket,
							bucketInfo.getOwnerId())) {
				if (bucketInfo.getLoggingEnabled()) {
					String targetBucket = bucketInfo.getTargetBucket();
					ArrayList<Grant> grants = new ArrayList<Grant>();
					try {
						BucketInfo targetBucketInfo = db.getUniqueEscape(new BucketInfo(targetBucket));
						List<GrantInfo> grantInfos = targetBucketInfo.getGrants();

						addGrants(grants, grantInfos);

					} catch (EucalyptusCloudException ex) {
						db.rollback();
						throw new InvalidTargetBucketForLoggingException(targetBucket);
					}
					LoggingEnabled loggingEnabled = new LoggingEnabled();
					loggingEnabled.setTargetBucket(bucketInfo.getTargetBucket());
					loggingEnabled.setTargetPrefix(bucketInfo.getTargetPrefix());

					TargetGrants targetGrants = new TargetGrants();
					targetGrants.setGrants(grants);
					loggingEnabled.setTargetGrants(targetGrants);
					reply.setLoggingEnabled(loggingEnabled);
				}
			} else {
				LOG.error("Not authorized to get bucket logging status by " + ctx.getUserFullName());
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketInfo.getBucketName());
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

	@Override
	public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request)
	throws EucalyptusCloudException {
		GetBucketVersioningStatusResponseType reply = (GetBucketVersioningStatusResponseType) request.getReply();
		String bucket = request.getBucket();
		Context ctx = Contexts.lookup();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo bucketInfo = db.getUniqueEscape(new BucketInfo(bucket));
			if (ctx.hasAdministrativePrivileges()
					|| Lookups.checkPrivilege(PolicySpec.S3_GETBUCKETVERSIONING, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucket,
							bucketInfo.getOwnerId())) {
				if (bucketInfo.getVersioning() != null) {
					String status = bucketInfo.getVersioning();
					if (WalrusProperties.VersioningStatus.Disabled.toString().equals(status)) {
						// reply.setVersioningStatus(WalrusProperties.VersioningStatus.Suspended.toString());

					} else {
						reply.setVersioningStatus(status);
					}
				}
			} else {
				LOG.error("Not authorized to get bucket version status by " + ctx.getUserFullName());
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketInfo.getBucketName());
			}
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}
		db.commit();
		return reply;
	}

	@Override
	public SetBucketVersioningStatusResponseType setBucketVersioningStatus(
			SetBucketVersioningStatusType request)
			throws EucalyptusCloudException {
		SetBucketVersioningStatusResponseType reply = (SetBucketVersioningStatusResponseType) request
				.getReply();
		String bucket = request.getBucket();
		Context ctx = Contexts.lookup();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo;
		try {
			bucketInfo = db.getUniqueEscape(new BucketInfo(bucket));
		} catch (EucalyptusCloudException ex) {
			db.rollback();
			throw new NoSuchBucketException(bucket);
		}

		if (ctx.hasAdministrativePrivileges()
				|| Lookups.checkPrivilege(PolicySpec.S3_PUTBUCKETVERSIONING, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucket,
						bucketInfo.getOwnerId())) {
			if (request.getVersioningStatus() != null) {
				String status = request.getVersioningStatus();
				if (WalrusProperties.VersioningStatus.Enabled.toString().equals(status))
					bucketInfo.setVersioning(WalrusProperties.VersioningStatus.Enabled.toString());
				else if (WalrusProperties.VersioningStatus.Suspended.toString().equals(status)
						&& WalrusProperties.VersioningStatus.Enabled.toString().equals(bucketInfo.getVersioning()))
					bucketInfo.setVersioning(WalrusProperties.VersioningStatus.Suspended.toString());
			}
		} else {
			LOG.error("Not authorized to set bucket version status by " + ctx.getUserFullName());
			db.rollback();
			throw new AccessDeniedException("Bucket", bucketInfo.getBucketName());
		}
		db.commit();
		return reply;
	}

	/*
	 * Significantly re-done version of listVersions that is based on listBuckets and the old listVersions.
	 */
	@Override
	public ListVersionsResponseType listVersions(ListVersionsType request) throws EucalyptusCloudException {
		ListVersionsResponseType reply = (ListVersionsResponseType) request.getReply();
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);

		try {
			String bucketName = request.getBucket();
			BucketInfo bucketInfo = new BucketInfo(bucketName);
			bucketInfo.setHidden(false);
			List<BucketInfo> bucketList = db.queryEscape(bucketInfo);

			Context ctx = Contexts.lookup();
			Account account = ctx.getAccount();

			int maxKeys = -1;
			String maxKeysString = request.getMaxKeys();
			if (maxKeysString != null) {
				maxKeys = Integer.parseInt(maxKeysString);
				if (maxKeys < 0) {
					throw new InvalidArgumentException("max-keys", "Argument max-keys must be an integer between 0 and " + Integer.MAX_VALUE);
				}
			} else {
				maxKeys = WalrusProperties.MAX_KEYS;
			}

			if (bucketList.size() > 0) {
				BucketInfo bucket = bucketList.get(0);
				BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;

				if (ctx.hasAdministrativePrivileges()
						|| (bucket.canRead(account.getAccountNumber()) && (bucket.isGlobalRead() || Lookups.checkPrivilege(PolicySpec.S3_LISTBUCKETVERSIONS,
								PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {

					if (logData != null) {
						updateLogData(bucket, logData);
						reply.setLogData(logData);
					}

					if (Contexts.lookup().hasAdministrativePrivileges()) {
						try {
							if (bucketHasSnapshots(bucketName)) {
								db.rollback();
								throw new NoSuchBucketException(bucketName);
							}
						} catch (Exception e) {
							db.rollback();
							throw new EucalyptusCloudException(e);
						}
					}

					String prefix = request.getPrefix();
					String keyMarker = request.getKeyMarker();
					String versionMarker = request.getVersionIdMarker();
					String delimiter = request.getDelimiter();

					reply.setName(bucketName);
					reply.setIsTruncated(false);
					reply.setPrefix(prefix);
					reply.setMaxKeys(maxKeys);
					reply.setDelimiter(delimiter);
					reply.setKeyMarker(keyMarker);
					reply.setVersionIdMarker(versionMarker);

					if (bucket.isVersioningDisabled()) {
						db.commit();
						return reply;
					}

					if (maxKeys == 0) {
						// No keys requested, so just return
						reply.setKeyEntries(new ArrayList<KeyEntry>());
						reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());
						db.commit();
						return reply;
					}

					final int queryStrideSize = maxKeys + 1;
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);

					ObjectInfo searchObj = new ObjectInfo();
					searchObj.setBucketName(bucketName);

					Criteria objCriteria = dbObject.createCriteria(ObjectInfo.class);
					objCriteria.add(Example.create(searchObj));
					objCriteria.addOrder(Order.asc("objectKey"));
					objCriteria.addOrder(Order.desc("lastModified"));
					objCriteria.setMaxResults(queryStrideSize); // add one to, hopefully, indicate truncation in one call

					// Ensure these aren't null
					keyMarker = (Strings.isNullOrEmpty(keyMarker) ? "" : keyMarker);
					prefix = (Strings.isNullOrEmpty(prefix) ? "" : prefix);
					versionMarker = (Strings.isNullOrEmpty(versionMarker) ? "" : versionMarker);

					if (!Strings.isNullOrEmpty(keyMarker)) {
						if (!Strings.isNullOrEmpty(versionMarker)) {
							Date resumeDate = null;
							try {
								ObjectInfo markerObj = new ObjectInfo();
								markerObj.setBucketName(bucketName);
								markerObj.setVersionId(versionMarker);
								markerObj.setObjectKey(keyMarker);
								ObjectInfo lastFromPrevObj = dbObject.uniqueResultEscape(markerObj);
								if (lastFromPrevObj != null && lastFromPrevObj.getLastModified() != null) {
									resumeDate = lastFromPrevObj.getLastModified();
								} else {
									dbObject.rollback();
									throw new NoSuchEntityException("VersionIDMarker " + versionMarker + " does not match an existing object version");
								}
							} catch (TransactionException e) {
								LOG.error(e);
								dbObject.rollback();
								throw new EucalyptusCloudException("Next-Key-Marker or Next-Version-Id marker invalid");
							}
							// The result set should be exclusive of the key with the key-marker version-id-marker pair. Look for keys that lexicographically
							// follow the version-id-marker for a given key-marker and also the keys that follow the key-marker.
							objCriteria.add(Restrictions.or(
									Restrictions.and(Restrictions.eq("objectKey", keyMarker), Restrictions.lt("lastModified", resumeDate)),
									Restrictions.gt("objectKey", keyMarker)));
						} else {
							// The result set should be exclusive of the key-marker. key-marker could be a common prefix from a previous response. Look for keys
							// that lexicographically follow the key-marker and don't contain the key-marker as the prefix.
							objCriteria.add(Restrictions.gt("objectKey", keyMarker));
						}
					}

					if (!Strings.isNullOrEmpty(prefix)) {
						objCriteria.add(Restrictions.like("objectKey", prefix, MatchMode.START));
					} else {
						prefix = ""; // ensure not null has already been set in the reply, so this is safe
					}

					List<ObjectInfo> objectInfos = null;
					int resultKeyCount = 0;
					ArrayList<KeyEntry> keyEntries = new ArrayList<KeyEntry>();
					String nextKeyMarker = null;
					String nextVersionIdMarker = null;
					TreeSet<String> commonPrefixes = new TreeSet<String>();
					int firstResult = -1;

					// Iterate over result sets of size maxkeys + 1
					do {
						// Start listing from the 0th element and increment the first element to be listed by the query size
						objCriteria.setFirstResult(queryStrideSize * (++firstResult));
						objectInfos = (List<ObjectInfo>) objCriteria.list();

						if (objectInfos.size() > 0) {

							for (ObjectInfo objectInfo : objectInfos) {
								String objectKey = objectInfo.getObjectKey();

								// Check if it will get aggregated as a commonprefix
								if (!Strings.isNullOrEmpty(delimiter)) {
									String[] parts = objectKey.substring(prefix.length()).split(delimiter);
									if (parts.length > 1) {
										String prefixString = prefix + parts[0] + delimiter;
										if (!StringUtils.equals(prefixString, keyMarker) && !commonPrefixes.contains(prefixString)) {
											if (resultKeyCount == maxKeys) {
												// This is a new record, so we know we're truncating if this is true
												reply.setNextKeyMarker(nextKeyMarker);
												reply.setNextVersionIdMarker(nextVersionIdMarker);
												reply.setIsTruncated(true);
												resultKeyCount++;
												break;
											}

											commonPrefixes.add(prefixString);
											resultKeyCount++; // count the unique commonprefix as a single return entry

											// If max keys have been collected, set the next-key-marker. It might be needed for the response if the list is
											// truncated
											// If the common prefixes hit the limit set by max-keys, next-key-marker is the last common prefix and there is no
											// version-id-marker
											if (resultKeyCount == maxKeys) {
												nextKeyMarker = prefixString;
												nextVersionIdMarker = null;
											}
										}
										continue;
									}
								}

								if (resultKeyCount == maxKeys) {
									// This is a new (non-commonprefix) record, so we know we're truncating
									reply.setNextKeyMarker(nextKeyMarker);
									reply.setNextVersionIdMarker(nextVersionIdMarker);
									reply.setIsTruncated(true);
									resultKeyCount++;
									break;
								}

								// This is either a version entry or a delete marker
								KeyEntry keyEntry = null;
								if (!objectInfo.getDeleted()) {
									keyEntry = new VersionEntry();
									((VersionEntry) keyEntry).setEtag(objectInfo.getEtag());
									((VersionEntry) keyEntry).setSize(objectInfo.getSize());
									((VersionEntry) keyEntry).setStorageClass(objectInfo.getStorageClass());
								} else {
									keyEntry = new DeleteMarkerEntry();
								}
								keyEntry.setKey(objectKey);
								keyEntry.setVersionId(objectInfo.getVersionId());
								keyEntry.setIsLatest(objectInfo.getLast());
								keyEntry.setLastModified(DateUtils.format(objectInfo.getLastModified().getTime(), DateUtils.ALT_ISO8601_DATE_PATTERN));
								try {
									Account ownerAccount = Accounts.lookupAccountById(objectInfo.getOwnerId());
									keyEntry.setOwner(new CanonicalUser(ownerAccount.getCanonicalId(), ownerAccount.getName()));
								} catch (AuthException e) {
									db.rollback();
									throw new AccessDeniedException("Bucket", bucketName, logData);
								}
								keyEntries.add(keyEntry);

								resultKeyCount++;

								// If max keys have been collected, set the next- markers. They might be needed for the response if the list is truncated
								if (resultKeyCount == maxKeys) {
									nextKeyMarker = objectKey;
									nextVersionIdMarker = objectInfo.getVersionId();
								}
							}
						}
						if (resultKeyCount <= maxKeys && objectInfos.size() <= maxKeys) {
							break;
						}
					} while (resultKeyCount <= maxKeys);

					reply.setKeyEntries(keyEntries);

					// Prefixes are already sorted, add them to the proper data structures and populate the reply
					if (!commonPrefixes.isEmpty()) {
						ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>();
						for (String prefixEntry : commonPrefixes) {
							commonPrefixesList.add(new CommonPrefixesEntry(prefixEntry));
						}
						reply.setCommonPrefixesList(commonPrefixesList);
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
		} finally {
			if (db.isActive()) {
				db.rollback();
			}
		}
	}

	@Override
	public DeleteVersionResponseType deleteVersion(DeleteVersionType request) throws EucalyptusCloudException {
		DeleteVersionResponseType reply = (DeleteVersionResponseType) request.getReply();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();

		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfos = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfos);

		if (bucketList.size() > 0) {
			BucketInfo bucketInfo = bucketList.get(0);
			BucketLogData logData = bucketInfo.getLoggingEnabled() ? request.getLogData() : null;
			ObjectInfo foundObject = null;
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			if (request.getVersionid() == null) {
				db.rollback();
				throw new EucalyptusCloudException("versionId is null");
			}
			searchObjectInfo.setVersionId(request.getVersionid());
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				foundObject = objectInfos.get(0);
			}

			/*
			 * The admin can always delete object versions, and if versioning is suspended then only the bucket owner can delete a specific version. If bucket
			 * versioning is enabled then do the normal permissions check to grant permissions.
			 */
			if (foundObject != null) {
				if (ctx.hasAdministrativePrivileges()
						|| (bucketInfo.getOwnerId().equals(account.getAccountNumber()) && Lookups
								.checkPrivilege(PolicySpec.S3_DELETEOBJECTVERSION, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT,
										PolicySpec.objectFullName(bucketName, objectKey), foundObject.getOwnerId()))) {

					dbObject.delete(foundObject);
					if (!foundObject.getDeleted()) {
						String objectName = foundObject.getObjectName();
						for (GrantInfo grantInfo : foundObject.getGrants()) {
							db.delete(grantInfo);
						}
						Long size = foundObject.getSize();

						boolean success = false;
						int retryCount = 0;
						do {
							try {
								decrementBucketSize(bucketName, size);
								success = true;
							} catch (NoSuchBucketException ex) {
								db.rollback();
								throw ex;
							} catch (RollbackException ex) {
								retryCount++;
								LOG.trace("retrying update: " + bucketName);
							} catch (EucalyptusCloudException ex) {
								db.rollback();
								throw ex;
							}
						} while (!success && (retryCount < 5));

						ObjectDeleter objectDeleter = new ObjectDeleter(
								bucketName, objectName,
								foundObject.getObjectKey(),
								foundObject.getVersionId(),
								size, 
								ctx.getUser().getName(),
								ctx.getUser().getUserId(),
								ctx.getAccount().getName(),
								ctx.getAccount().getAccountNumber());
						Threads.lookup(Walrus.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
					}
					
					if (logData != null) {
						updateLogData(bucketInfo, logData);
						reply.setLogData(logData);
					}
				} else {
					db.rollback();
					throw new AccessDeniedException("Key", objectKey, logData);
				}
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		
		reply.setStatus(HttpResponseStatus.NO_CONTENT);
		reply.setStatusMessage("NO CONTENT");
		db.commit();
		return reply;
	}

	public static InetAddress getBucketIp(String bucket) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo searchBucket = new BucketInfo(bucket);
			db.getUniqueEscape(searchBucket);
			return WalrusProperties.getWalrusAddress();
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.rollback();
		}
	}

	@Override
	public void fastDeleteObject(DeleteObjectType request) throws EucalyptusCloudException {
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfos = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfos);
		if (bucketList.size() > 0) {
			BucketInfo bucketInfo = bucketList.get(0);
			BucketLogData logData = bucketInfo.getLoggingEnabled() ? request.getLogData() : null;
			ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
			searchObjectInfo.setVersionId(WalrusProperties.NULL_VERSION_ID);
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
			if (objectInfos.size() > 0) {
				ObjectInfo foundObject = objectInfos.get(0);
				dbObject.delete(foundObject);
				String objectName = foundObject.getObjectName();
				for (GrantInfo grantInfo : foundObject.getGrants()) {
					db.delete(grantInfo);
				}
				Long size = foundObject.getSize();
				try {
					storageManager.deleteObject(bucketName, objectName);
				} catch (IOException ex) {
					LOG.error(ex, ex);
				}
				boolean success = false;
				int retryCount = 0;
				do {
					try {
						decrementBucketSize(bucketName, size);
						success = true;
					} catch (NoSuchBucketException ex) {
						db.rollback();
						throw ex;
					} catch (RollbackException ex) {
						retryCount++;
						LOG.trace("retrying update: " + bucketName);
					} catch (EucalyptusCloudException ex) {
						db.rollback();
						throw ex;
					}
				} while (!success && (retryCount < 5));

			} else {
				db.rollback();
				throw new NoSuchEntityException(objectKey, logData);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
	}

	@Override
	public void fastDeleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
		String bucketName = request.getBucket();
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo searchBucket = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(searchBucket);

		if (bucketList.size() > 0) {
			BucketInfo bucketFound = bucketList.get(0);
			EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
			ObjectInfo searchObject = new ObjectInfo();
			searchObject.setBucketName(bucketName);
			searchObject.setDeleted(false);
			List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObject);
			if (objectInfos.size() == 0) {
				db.delete(bucketFound);
				// Actually remove the bucket from the backing store
				try {
					storageManager.deleteBucket(bucketName);
				} catch (IOException ex) {
					// set exception code in reply
					LOG.error(ex);
				}
			} else {
				db.rollback();
				throw new BucketNotEmptyException(bucketName);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		db.commit();
	}

	/**
	 * Fire creation and possibly a related delete event.
	 * 
	 * If an object (version) is being overwritten then there will not be a corresponding delete event so we fire one prior to the create event.
	 */
	private void fireObjectCreationEvent(final String bucketName, final String objectKey, final String version, final String userId, final Long size,
			final Long oldSize) {
		try {
			if (oldSize != null && oldSize > 0) {
				fireObjectUsageEvent(S3ObjectAction.OBJECTDELETE, bucketName, objectKey, version, userId, oldSize);
			}

			/* Send an event to reporting to report this S3 usage. */
			if (size != null && size > 0) {
				fireObjectUsageEvent(S3ObjectAction.OBJECTCREATE, bucketName, objectKey, version, userId, size);
			}
		} catch (final Exception e) {
			LOG.error(e, e);
		}
	}

	private static void fireObjectUsageEvent(S3ObjectAction actionInfo, String bucketName, String objectKey, String version, String ownerUserId,
			Long sizeInBytes) {
		try {
			ListenerRegistry.getInstance().fireEvent(S3ObjectEvent.with(actionInfo, bucketName, objectKey, version, ownerUserId, sizeInBytes));
		} catch (final Exception e) {
			LOG.error(e, e);
		}
	}
	
	public InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws EucalyptusCloudException {
		InitiateMultipartUploadResponseType reply = (InitiateMultipartUploadResponseType) request.getReply();
		
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			
			if(ctx.hasAdministrativePrivileges() || (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || 
					Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				
				EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
				ObjectInfo searchObjectInfo = new ObjectInfo(bucketName, objectKey);
				List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObjectInfo);
				
				if(objectInfos.size() == 1 && objectInfos.get(0).getUploadId() == null) {
					throw new EucalyptusCloudException("A single part upload exists for the same bucket and key, intiate multipart uplaod with a different key for now");
				}
				
				if (Iterables.any(objectInfos,  new Predicate<ObjectInfo>(){

					@Override
					public boolean apply(@Nullable ObjectInfo arg0) {
						return arg0.getPartNumber() != null || arg0.getManifest();
					}
					
				})){
					throw new EucalyptusCloudException("Multipart upload is in progress for the same bucket and key, either abort or complete the upload");
				}
			
			
				// Create a manifest object entity
				String objectName;
				ObjectInfo manifest = new ObjectInfo(bucketName, objectKey);
				manifest.setOwnerId(account.getAccountNumber());
				objectName = UUID.randomUUID().toString();
				manifest.setObjectName(objectName);
				manifest.setUploadId(Crypto.generateAlphanumericId(64, ""));
				manifest.setManifest(Boolean.TRUE);
				manifest.setUploadComplete(Boolean.FALSE);
				manifest.setCleanup(Boolean.FALSE);
				manifest.setSize(0L);
				manifest.setVersionId(UUID.randomUUID().toString().replaceAll("-", ""));
				dbObject.add(manifest);
				
				try {
					dbObject.commit();
				} catch (RollbackException ex) {
					dbObject.rollback();
					LOG.error("Error comitting new object entity to database", ex);
				}
				
				reply.setUploadId(manifest.getUploadId()); 
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		
		reply.setBucket(bucketName);
		reply.setKey(objectKey);
		return reply;
	}
	
	public UploadPartResponseType uploadPart(UploadPartType request) throws EucalyptusCloudException {
		UploadPartResponseType reply = (UploadPartResponseType) request.getReply();
		
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		String key = bucketName + "." + objectKey;
		String randomKey = request.getRandomKey();
		String uploadId = request.getUploadid();
		Integer partNumber = Integer.parseInt(request.getPartnumber());
		WalrusDataMessenger messenger = WalrusRESTBinding.getWriteMessenger();
		Date lastModified = null;
		String md5 = new String();
		Long oldBucketSize = 0L; //TODO compute this correctly later
		
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
		
		if(bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			BucketLogData logData = bucket.getLoggingEnabled() ? request.getLogData() : null;
			long objSize = 0;
			
			try {
				objSize = Long.valueOf(request.getContentLength());
			} catch (NumberFormatException e) {
				LOG.error("Invalid content length " + request.getContentLength());
				// TODO(wenye): should handle this properly.
				objSize = 1L;
			}
			
			if(ctx.hasAdministrativePrivileges() || (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || 
					Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				
				if (logData != null) {
					reply.setLogData(logData);
				}
				String objectName = null;
				String versionId = null;
				Long oldObjectSize = 0L;
				ObjectInfo objectInfo = null;
				
				try {
					objectName = UUID.randomUUID().toString();
					ObjectInfo searchManifest = new ObjectInfo(bucketName, objectKey);
					searchManifest.setUploadId(uploadId);
					searchManifest.setManifest(Boolean.TRUE);
					searchManifest.setUploadComplete(Boolean.FALSE);
					searchManifest.setCleanup(Boolean.FALSE);
					
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo foundManifest = dbObject.uniqueResultEscape(searchManifest);
					
					if (foundManifest != null) {
						versionId = foundManifest.getVersionId(); 
						objectInfo = new ObjectInfo(bucketName, objectKey);
						objectInfo.setOwnerId(account.getAccountNumber());
						objectInfo.setObjectName(objectName);
						objectInfo.setUploadId(uploadId);
						objectInfo.setPartNumber(partNumber);
						objectInfo.setManifest(Boolean.FALSE);
						objectInfo.setUploadComplete(Boolean.FALSE);
						objectInfo.setCleanup(Boolean.FALSE);
						objectInfo.setVersionId(versionId);
						dbObject.add(objectInfo);
					} else {
						throw new EucalyptusCloudException("Multipart upload ID is invalid. Intitiate a multipart upload request before uploading the parts");
					}
					
					String bucketOwnerId = bucket.getOwnerId();
					dbObject.commit();
				} catch (Exception ex) {
					throw new EucalyptusCloudException(ex);
				}
					
				// writes are unconditional
				WalrusDataQueue<WalrusDataMessage> putQueue = messenger.getQueue(key, randomKey);
				
				try {
					WalrusDataMessage dataMessage;
					String tempObjectName = objectName;
					MessageDigest digest = null;
					long size = 0;
					FileIO fileIO = null;
					while ((dataMessage = putQueue.take()) != null) {
						if (putQueue.getInterrupted()) {
							if (WalrusDataMessage.isEOF(dataMessage)) {
								WalrusMonitor monitor = messenger.getMonitor(key);
								if (monitor.getLastModified() == null) {
									LOG.trace("Monitor wait: " + key
											+ " random: " + randomKey);
									synchronized (monitor) {
										monitor.wait();
									}
								}
								LOG.trace("Monitor resume: " + key + " random: " + randomKey);
								lastModified = monitor.getLastModified();
								md5 = monitor.getMd5();
								// ok we are done here
								if (fileIO != null) {
									fileIO.finish();
								}
								ObjectDeleter objectDeleter = new ObjectDeleter(bucketName, tempObjectName, null, null,-1L, 
										ctx.getUser().getName(), 
										ctx.getUser().getUserId(), 
										ctx.getAccount().getName(), 
										ctx.getAccount().getAccountNumber());
								Threads.lookup(Walrus.class, WalrusFSManager.ObjectDeleter.class).limitTo(10).submit(objectDeleter);
								LOG.info("Transfer interrupted: " + key);
								messenger.removeQueue(key, randomKey);
								break;
							}
							continue;
						}
						if (WalrusDataMessage.isStart(dataMessage)) {
							tempObjectName = UUID.randomUUID().toString();
							digest = Digest.MD5.get();
							try {
								fileIO = storageManager.prepareForWrite(bucketName, tempObjectName);
							} catch (Exception ex) {
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(ex);
							}
						} else if (WalrusDataMessage.isEOF(dataMessage)) {
							if (digest != null) {
								md5 = Hashes.bytesToHex(digest.digest());
							} else {
								WalrusMonitor monitor = messenger.getMonitor(key);
								md5 = monitor.getMd5();
								lastModified = monitor.getLastModified();
								if (md5 == null) {
									LOG.error("ETag did not match for: " + randomKey + " Computed MD5 is null");
									throw new ContentMismatchException(bucketName + "/" + objectKey);
								}
								break;
							}
							String contentMD5 = request.getContentMD5();
							if (contentMD5 != null) {
								String contentMD5AsHex = Hashes.bytesToHex(Base64.decode(contentMD5));
								if (!contentMD5AsHex.equals(md5)) {
									if (fileIO != null) {
										fileIO.finish();
									}
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									LOG.error("ETag did not match for: " + randomKey + " Expected: " + contentMD5AsHex + " Computed: " + md5);
									throw new ContentMismatchException(bucketName + "/" + objectKey);
								}
							}

							// Fix for EUCA-2275:
							// Moved up policy and bucket size checks on the temporary object. The temp object is committed (renamed) only after it clears the checks.
							// If any of the checks fail, temp object is cleaned up and the process errors out. If the PUT request is overwriting an existing object, the object is left untouched.
							// So the fix ensures proper clean up of temp files (no orphaned files) and does not overwrite existing data when policy or bucket size checks fail

							if (!ctx.hasAdministrativePrivileges() && 
									!Permissions.canAllocate(PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_OBJECT, bucketName, PolicySpec.S3_PUTOBJECT, ctx.getUser(), oldBucketSize + size)) {
								// dbObject.rollback();
								cleanupTempObject(ctx, bucketName, tempObjectName);
								messenger.removeQueue(key, randomKey);
								LOG.error("Quota exceeded for Walrus putObject");
								throw new EntityTooLargeException("Key", objectKey);
							}
							boolean success = false;
							int retryCount = 0;
							do {
								try {
									incrementBucketSize(bucketName, objectKey, oldBucketSize, size);
									success = true;
								} catch (EntityTooLargeException ex) {
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									// dbObject.rollback();
									throw ex;
								} catch (NoSuchBucketException ex) {
									// dbObject.rollback();
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									throw ex;
								} catch (RollbackException ex) {
									retryCount++;
									LOG.trace("retrying update: " + bucketName);
								} catch (EucalyptusCloudException ex) {
									// dbObject.rollback();
									cleanupTempObject(ctx, bucketName, tempObjectName);
									messenger.removeQueue(key, randomKey);
									throw ex;
								}
							} while (!success && (retryCount < 5));

							// commit object
							try {
								if (fileIO != null) {
									fileIO.finish();
								}
								storageManager.renameObject(bucketName, tempObjectName, objectName);
							} catch (IOException ex) {
								LOG.error(ex);
								messenger.removeQueue(key, randomKey);
								throw new EucalyptusCloudException(objectKey);
							}
							lastModified = new Date();
							ObjectInfo searchObject = new ObjectInfo(bucketName, objectKey);
							searchObject.setVersionId(versionId);
							searchObject.setPartNumber(partNumber);
							searchObject.setUploadId(uploadId);
							EntityWrapper<ObjectInfo> dbObject = EntityWrapper.get(ObjectInfo.class);
							ObjectInfo foundObject;
							try {
								foundObject = dbObject.getUniqueEscape(searchObject);
								if (ctx.hasAdministrativePrivileges() || foundObject.canWriteACP(account.getAccountNumber())) {
									List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
// TODO										foundObject.addGrants(account.getAccountNumber(), bucketOwnerId, grantInfos, accessControlList);
									foundObject.setGrants(grantInfos);
								}
								if (WalrusProperties.enableTorrents) {
									EntityWrapper<TorrentInfo> dbTorrent = dbObject.recast(TorrentInfo.class);
									TorrentInfo torrentInfo = new TorrentInfo(bucketName, objectKey);
									List<TorrentInfo> torrentInfos = dbTorrent.queryEscape(torrentInfo);
									if (torrentInfos.size() > 0) {
										TorrentInfo foundTorrentInfo = torrentInfos.get(0);
										TorrentClient torrentClient = Torrents.getClient(bucketName + objectKey);
										if (torrentClient != null) {
											torrentClient.bye();
										}
										dbTorrent.delete(foundTorrentInfo);
									}
								} else {
									LOG.warn("Bittorrent support has been disabled. Please check pre-requisites");
								}
							} catch (EucalyptusCloudException ex) {
								if (objectInfo != null) {
									foundObject = objectInfo;
								} else {
									dbObject.rollback();
									throw new EucalyptusCloudException("Unable to update object: " + bucketName + "/" + objectKey);
								}
							}
// TODO								foundObject.setVersionId(versionId); 
// TODO								foundObject.replaceMetaData(request.getMetaData());
							foundObject.setEtag(md5);
							foundObject.setSize(size);
							foundObject.setLastModified(lastModified);
							foundObject.setStorageClass("STANDARD");
// TODO								foundObject.setContentType(request.getContentType());
// TODO								foundObject.setContentDisposition(request.getContentDisposition());
							foundObject.setLast(true);
							foundObject.setDeleted(false);
							reply.setSize(size);
							if (logData != null) {
								logData.setObjectSize(size);
								updateLogData(bucket, logData);
							}
							if (objectInfo != null) {
								dbObject.add(foundObject);
							}
							success = false;
							try {
								dbObject.commit();
								success = true;
							} catch (RollbackException ex) {
								dbObject.rollback();
								LOG.error(ex, ex);
							}

							dbObject = EntityWrapper.get(ObjectInfo.class);
							List<ObjectInfo> objectInfos = dbObject.queryEscape(searchObject);
							for (ObjectInfo objInfo : objectInfos) {
								if (!success) {
									if (objInfo.getLast()) {
										lastModified = objInfo.getLastModified();
										md5 = objInfo.getEtag();
									}
									success = true;
								}
								if (!versionId.equals(objInfo.getVersionId())) {
									objInfo.setLast(false);
								}
							}
							dbObject.commit();

							// See if a delete marker exists that needs to be
							// removed now
							dbObject = EntityWrapper.get(ObjectInfo.class);
							ObjectInfo deleteMarker = new ObjectInfo(bucketName, objectKey);
							deleteMarker.setDeleted(true);
							deleteMarker.setUploadId(uploadId);
							deleteMarker.setPartNumber(partNumber);
							ObjectInfo foundDeleteMarker = null;
							try {
								foundDeleteMarker = dbObject.getUniqueEscape(deleteMarker);
								dbObject.delete(foundDeleteMarker);
							} catch (Exception ex) {
								if (foundDeleteMarker != null) {
									LOG.error("Deletion of delete marker failed for: " + bucketName + "/" + objectKey, ex);
								}
							}

							dbObject.commit();

							if (logData != null) {
								logData.setTurnAroundTime(Long.parseLong(new String(dataMessage.getPayload())));
							}
							// restart all interrupted puts
							WalrusMonitor monitor = messenger.getMonitor(key);
							synchronized (monitor) {
								monitor.setLastModified(lastModified);
								monitor.setMd5(md5);
								monitor.notifyAll();
							}
							// messenger.removeMonitor(key);
							messenger.clearQueues(key);
							messenger.removeQueue(key, randomKey);
							LOG.info("Transfer complete: " + key);

							try {
								fireObjectCreationEvent(bucketName, objectKey, versionId, ctx.getUser().getUserId(), size, oldObjectSize);
							} catch (Exception ex) {
								LOG.debug("Failed to fire reporting event for walrus PUT object operation", ex);
							}

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
							if (digest != null) {
								digest.update(data);
							}
						}
					}
				} catch (Exception ex) {
					LOG.error(ex, ex);
					db.rollback();
					messenger.removeQueue(key, randomKey);
					throw new EucalyptusCloudException("Transfer interrupted: " + key + "." + randomKey);
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
		
		reply.setEtag(md5); // TODO figure out etag correctly
		reply.setLastModified(DateUtils.format(lastModified.getTime(), DateUtils.RFC822_DATETIME_PATTERN));
		return reply;
	}
	
	public CompleteMultipartUploadResponseType completeMultipartUpload(CompleteMultipartUploadType request) throws EucalyptusCloudException {
		CompleteMultipartUploadResponseType reply = (CompleteMultipartUploadResponseType) request.getReply();
		
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		String bucketName = request.getBucket();
		String objectKey = request.getKey();
		List<Part> requestParts = request.getParts();
		
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		BucketInfo bucketInfo = new BucketInfo(bucketName);
		List<BucketInfo> bucketList = db.queryEscape(bucketInfo);
		
		if (bucketList.size() > 0) {
			BucketInfo bucket = bucketList.get(0);
			
			if(ctx.hasAdministrativePrivileges() || (bucket.canWrite(account.getAccountNumber()) && (bucket.isGlobalWrite() || 
					Lookups.checkPrivilege(PolicySpec.S3_PUTOBJECT, PolicySpec.VENDOR_S3, PolicySpec.S3_RESOURCE_BUCKET, bucketName, null)))) {
				
				try {
					// Find the manifest entity
					ObjectInfo searchManifest = new ObjectInfo(bucketName, objectKey);
					searchManifest.setUploadId(request.getUploadid());
					searchManifest.setManifest(Boolean.TRUE);
					
					EntityWrapper<ObjectInfo> dbObject = db.recast(ObjectInfo.class);
					ObjectInfo foundManifest = dbObject.uniqueResultEscape(searchManifest);
					
					if (foundManifest != null) {
						// Look for the parts
						ObjectInfo searchPart = new ObjectInfo(bucketName, objectKey);
						searchPart.setUploadId(request.getUploadid());
						searchPart.setManifest(Boolean.FALSE);

						List<ObjectInfo> foundParts = dbObject.queryEscape(searchPart);
						
						if(foundParts != null && foundParts.size() > 0) {
							if(requestParts != null && requestParts.size() > foundParts.size()) {
								throw new EucalyptusCloudException("One or more parts has not been uploaded yet. Either upload the part or fix the manifest");
							} else {
								// Create a hashmap 
								Map<Integer, ObjectInfo> partsMap = new HashMap<Integer, ObjectInfo>(foundParts.size());
								for(ObjectInfo foundPart : foundParts) {
									foundPart.setCleanup(Boolean.TRUE);
									foundPart.setUploadComplete(Boolean.TRUE);
									partsMap.put(foundPart.getPartNumber(), foundPart);
								}
								
								ObjectInfo lookupPart = null;
								for(Part requestPart : requestParts){
									if((lookupPart = partsMap.get(requestPart.getPartNumber())) != null) {
										lookupPart.setCleanup(Boolean.FALSE);
									} else {
										throw new EucalyptusCloudException("Part not found");
									}
								}
								foundManifest.setCleanup(Boolean.FALSE);
								foundManifest.setUploadComplete(Boolean.TRUE);
								db.commit();
							}
						} else {
							throw new EucalyptusCloudException("No parts found in the database");
						}
						
					} else {
						throw new EucalyptusCloudException("Multipart upload ID is invalid.");
					}
				} catch (Exception ex) {
					db.rollback();
					throw new EucalyptusCloudException(ex);
				}
			} else {
				db.rollback();
				throw new AccessDeniedException("Bucket", bucketName);
			}
		} else {
			db.rollback();
			throw new NoSuchBucketException(bucketName);
		}
		
		reply.setLocation(request.getBucket() + '/' + request.getKey());
		reply.setBucket(request.getBucket());
		reply.setKey(request.getKey());
		reply.setEtag(Crypto.generateAlphanumericId(16, "")); // TODO figure out etag correctly
		return reply;
	}
	
	public AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) {
		AbortMultipartUploadResponseType reply = (AbortMultipartUploadResponseType) request.getReply();
		return reply;
	}

}
