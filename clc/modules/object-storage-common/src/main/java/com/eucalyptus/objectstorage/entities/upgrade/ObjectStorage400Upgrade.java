/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.objectstorage.entities.upgrade;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_0_0;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.DatabaseAuthProvider;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.BucketState;
import com.eucalyptus.objectstorage.ObjectState;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.VersioningStatus;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.walrus.entities.BucketInfo;
import com.eucalyptus.walrus.entities.GrantInfo;
import com.eucalyptus.walrus.entities.ImageCacheInfo;
import com.eucalyptus.walrus.entities.ObjectInfo;
import com.eucalyptus.walrus.entities.WalrusSnapshotInfo;
import com.eucalyptus.walrus.util.WalrusProperties;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Upgrade process for transferring information from Walrus to OSG, and modifying Walrus entities to work with OSG. The upgrade is broken down in to well
 * defined ordered stages. A failing stage will halt the upgrade process and the subsequent stages won't be processed.
 * 
 * @author Swathi Gangisetty
 */
public class ObjectStorage400Upgrade {

	private static Logger LOG = Logger.getLogger(ObjectStorage400Upgrade.class);
	private static Map<String, Account> accountIdAccountMap = Maps.newHashMap(); // Cache account ID -> account info
	private static Map<String, User> accountIdAdminMap = Maps.newHashMap(); // Cache account ID -> admin user info
	private static Map<String, User> userIdUserMap = Maps.newHashMap(); // Cache user ID -> user info
	private static Set<String> deletedAccountIds = Sets.newHashSet(); // Cache deleted account IDs
	private static Set<String> deletedUserIds = Sets.newHashSet(); // Cache deleted user IDs
	private static Set<String> deletedAdminAccountIds = Sets.newHashSet(); // Cache account IDs whose admin is deleted
	private static Set<String> noCanonicalIdAccountIds = Sets.newHashSet(); // Cache account IDs without any canonical IDs
	private static Map<String, Bucket> bucketMap = Maps.newHashMap(); // Cache bucket name -> bucket object
	private static Set<String> walrusSnapshotBuckets = Sets.newHashSet(); // Cache all snapshot buckets
	private static Set<String> walrusSnapshotObjects = Sets.newHashSet(); // Cache all snapshot objects
	private static Account eucalyptusAccount = null;
	private static User eucalyptusAdmin = null;
	private static Account blockStorageAccount = null;
	private static User blockStorageAdmin = null;

	public interface UpgradeTask {
		public void apply() throws Exception;
	}

	private static final ArrayList<? extends UpgradeTask> upgrades = Lists.newArrayList(Setup.INSTANCE, CopyBucketsToOSG.INSTANCE, CopyObjectsToOSG.INSTANCE,
			ModifyWalrusBuckets.INSTANCE, ModifyWalrusObjects.INSTANCE, FlushImageCache.INSTANCE);

	@EntityUpgrade(entities = { ObjectEntity.class }, since = v4_0_0, value = ObjectStorage.class)
	public static enum OSGUpgrade implements Predicate<Class> {
		INSTANCE;

		@Override
		public boolean apply(@Nullable Class arg0) {

			// Iterate through each upgrade task, using iterators.all to bail out on the first failure
			return Iterators.all(upgrades.iterator(), new Predicate<UpgradeTask>() {

				@Override
				public boolean apply(UpgradeTask task) {
					try {
						LOG.info("Executing objectstorage upgrade task: " + task.getClass().getSimpleName());
						task.apply();
						return true;
					} catch (Exception e) {
						LOG.error("Upgrade task failed: " + task.getClass().getSimpleName());
						// Returning false does not seem to halt the upgrade and cause a rollback, must throw an exception
						throw Exceptions.toUndeclared("Objectstorage upgrade failed due to an error in upgrade task: " + task.getClass().getSimpleName(), e);
					}
				}

			});
		}
	}

	/**
	 * Setup stage for configuring the prerequisites before performing the upgrade
	 * 
	 * <li>Initialize the Accounts library</li>
	 * 
	 * <li>Setup a blockstorage account</li>
	 * 
	 * <li>Assign canonical IDs to accounts that don't have it</li>
	 * 
	 */
	public enum Setup implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			// Initialize the accounts provider before doing anything
			DatabaseAuthProvider dbAuth = new DatabaseAuthProvider();
			Accounts.setAccountProvider(dbAuth);

			// Setup the blockstorage account
			createBlockStorageAccount();

			// Generate canonical IDs for accounts that don't have them
			generateCanonicaIDs();
		}
	}

	/**
	 * Transform Walrus bucket entities to OSG bucket entities and persist them. A transformation function is used for converting a Walrus bucket entity to OSG
	 * bucket entity
	 * 
	 */
	public enum CopyBucketsToOSG implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			EntityTransaction osgTran = Entities.get(Bucket.class);
			try {

				List<Bucket> osgBuckets = Entities.query(new Bucket());
				if (osgBuckets != null && osgBuckets.isEmpty()) { // Perform the upgrade only if osg entities are empty

					EntityTransaction walrusTran = Entities.get(BucketInfo.class);
					try {

						List<BucketInfo> walrusBuckets = Entities.query(new BucketInfo(), Boolean.TRUE);
						if (walrusBuckets != null && !walrusBuckets.isEmpty()) { // Check if there are any walrus objects to upgrade

							// Populate snapshot buckets and objects the snapshot buckets and objects
							populateSnapshotBucketsAndObjects();

							// Create an OSG bucket for the corresponding walrus Bucket and persist it
							for (Bucket osgBucket : Lists.transform(walrusBuckets, bucketTransformationFunction())) {
								Entities.persist(osgBucket);
							}
						} else {
							// no buckets in walrus, nothing to do here
						}
						walrusTran.commit();
					} catch (Exception e) {
						walrusTran.rollback();
						throw e;
					} finally {
						if (walrusTran.isActive()) {
							walrusTran.commit();
						}
					}
				} else {
					// nothing to do here since buckets might already be there
				}
				osgTran.commit();
			} catch (Exception e) {
				osgTran.rollback();
				throw e;
			} finally {
				if (osgTran.isActive()) {
					osgTran.commit();
				}
			}
		}
	}

	/**
	 * Transform Walrus object entities to OSG object entities and persist them. A transformation function is used for converting a Walrus object entity to OSG
	 * object entity
	 * 
	 */
	public enum CopyObjectsToOSG implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			EntityTransaction osgTran = Entities.get(ObjectEntity.class);
			try {

				List<ObjectEntity> osgObjects = Entities.query(new ObjectEntity());
				if (osgObjects != null && osgObjects.isEmpty()) { // Perform the upgrade only if osg entities are empty

					EntityTransaction walrusTran = Entities.get(ObjectInfo.class);
					try {

						List<ObjectInfo> walrusObjects = Entities.query(new ObjectInfo(), Boolean.TRUE);
						if (walrusObjects != null && !walrusObjects.isEmpty()) { // Check if there are any walrus objects to upgrade

							// Lists.transform() is a lazy operation, so all elements are iterated through only once
							for (ObjectEntity osgObject : Lists.transform(walrusObjects, objectTransformationFunction())) {
								Entities.persist(osgObject);
							}
						} else {
							// no objects in walrus, nothing to do here
						}
						walrusTran.commit();
					} catch (Exception e) {
						walrusTran.rollback();
						throw e;
					} finally {
						if (walrusTran.isActive()) {
							walrusTran.commit();
						}
					}
				} else {
					// nothing to do here since buckets might already be there
				}
				osgTran.commit();
			} catch (Exception e) {
				osgTran.rollback();
				throw e;
			} finally {
				if (osgTran.isActive()) {
					osgTran.commit();
				}
			}
		}
	}

	/**
	 * Modify Walrus buckets to work better with OSG
	 * 
	 * <li>Reset the ownership of every bucket to Eucalyptus account</li>
	 * 
	 * <li>Reset the ACLs on every bucket and set it to private (FULL_CONTROL for the bucket owner)</li>
	 * 
	 * <li>Disable versioning entirely (even if its suspended)</li>
	 */
	public enum ModifyWalrusBuckets implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			EntityTransaction tran = Entities.get(BucketInfo.class);
			try {

				List<BucketInfo> walrusBuckets = Entities.query(new BucketInfo());
				if (walrusBuckets != null && !walrusBuckets.isEmpty()) { // Check if there are any walrus objects to upgrade

					for (BucketInfo walrusBucket : walrusBuckets) {
						try {
							// Reset the ownership and assign it to Eucalyptus admin account and user
							walrusBucket.setOwnerId(getEucalyptusAccount().getAccountNumber());
							walrusBucket.setUserId(getEucalyptusAdmin().getUserId());

							// Reset the ACLs and assign the owner full control
							walrusBucket.resetGlobalGrants();
							List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
							GrantInfo.setFullControl(walrusBucket.getOwnerId(), grantInfos);
							walrusBucket.setGrants(grantInfos);

							// Disable versioning, could probably suspend it but that might not entirely stop walrus from doing versioning related tasks
							if (walrusBucket.getVersioning() != null
									&& (WalrusProperties.VersioningStatus.Enabled.toString().equals(walrusBucket.getVersioning()) || WalrusProperties.VersioningStatus.Suspended
											.toString().equals(walrusBucket.getVersioning()))) {
								walrusBucket.setVersioning(WalrusProperties.VersioningStatus.Disabled.toString());
							}
						} catch (Exception e) {
							LOG.error("Failed to modify Walrus bucket " + walrusBucket.getBucketName(), e);
							throw e;
						}
					}
				} else {
					// no buckets in walrus, nothing to do here
				}
				tran.commit();
			} catch (Exception e) {
				tran.rollback();
				throw e;
			} finally {
				if (tran.isActive()) {
					tran.commit();
				}
			}
		}
	}

	/**
	 * Modify Walrus objects to work with OSG
	 * 
	 * <li>Remove delete markers since versioning is entirely handled by OSG</li>
	 * 
	 * <li>Overwrite objectKey with the objectName, this is the same as the objectUuid in OSG and will be used by the OSG to refer to the object</li>
	 * 
	 * <li>Overwrite the version ID with the string "null" as Walrus no longer keeps track of versions</li>
	 * 
	 * <li>Mark the object as the latest since all the objects are unique to Walrus after changing the object key</li>
	 * 
	 * <li>Reset the ownership of every object to Eucalyptus account</li>
	 * 
	 * <li>Reset the ACLs on every object and set it to private (FULL_CONTROL for the object owner)</li>
	 */
	public enum ModifyWalrusObjects implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			EntityTransaction tran = Entities.get(ObjectInfo.class);
			try {

				List<ObjectInfo> walrusObjects = Entities.query(new ObjectInfo());
				if (walrusObjects != null && !walrusObjects.isEmpty()) { // Check if there are any walrus objects to upgrade

					for (ObjectInfo walrusObject : walrusObjects) {
						try {
							// Check and remove the record if its a delete marker
							if (walrusObject.getDeleted() != null && walrusObject.getDeleted()) {
								LOG.info("Removing delete marker from Walrus for object " + walrusObject.getObjectKey() + " in bucket "
										+ walrusObject.getBucketName() + " with version ID " + walrusObject.getVersionId());
								Entities.delete(walrusObject);
								continue;
							}

							// Copy object name to object key since thats the reference used by OSG
							walrusObject.setObjectKey(walrusObject.getObjectName());

							// Change the version ID to null
							walrusObject.setVersionId(WalrusProperties.NULL_VERSION_ID);

							// Mark the object as latest
							walrusObject.setLast(Boolean.TRUE);

							// Reset the ownership and assign it to Eucalyptus admin account
							walrusObject.setOwnerId(getEucalyptusAccount().getAccountNumber());

							// Reset the ACLs and assign the owner full control
							walrusObject.resetGlobalGrants();
							List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
							GrantInfo.setFullControl(walrusObject.getOwnerId(), grantInfos);
							walrusObject.setGrants(grantInfos);
						} catch (Exception e) {
							LOG.error("Failed to modify Walrus object " + walrusObject.getObjectKey(), e);
							throw e;
						}
					}
				} else {
					// no objects in walrus, nothing to do here
				}
				tran.commit();
			} catch (Exception e) {
				tran.rollback();
				throw e;
			} finally {
				if (tran.isActive()) {
					tran.commit();
				}
			}
		}
	}

	/**
	 * Add cached images as objects to walrus and OSG and mark them for deletion in OSG. When the OSG boots up, it'll start deleting the objects
	 * 
	 */
	public enum FlushImageCache implements UpgradeTask {
		INSTANCE;

		@Override
		public void apply() throws Exception {
			EntityTransaction walrusImageTran = Entities.get(ImageCacheInfo.class);
			try {

				List<ImageCacheInfo> images = Entities.query(new ImageCacheInfo());
				if (images != null && !images.isEmpty()) { // Check if there are any cached images to delete
					EntityTransaction osgObjectTran = Entities.get(ObjectEntity.class);
					EntityTransaction walrusObjectTran = Entities.get(ObjectInfo.class);

					try {
						for (ImageCacheInfo image : images) {
							Entities.persist(imageToOSGObjectTransformation().apply(image)); // Persist a new OSG object
							Entities.persist(imageToWalrusObjectTransformation().apply(image));
							Entities.delete(image); // Delete the cached image from database
						}

						osgObjectTran.commit();
						walrusObjectTran.commit();
					} catch (Exception e) {
						osgObjectTran.rollback();
						walrusObjectTran.rollback();
						throw e;
					} finally {
						if (osgObjectTran.isActive()) {
							osgObjectTran.commit();
						}
						if (walrusObjectTran.isActive()) {
							walrusObjectTran.commit();
						}
					}
				} else {
					// no images in walrus, nothing to do here
				}
				walrusImageTran.commit();
			} catch (Exception e) {
				walrusImageTran.rollback();
				// Exceptions here should not halt the upgrade process, the cached images can be flushed manually
				LOG.warn("Cannot flush cached images in Walrus due to an error. May have to be flushed manually");
			} finally {
				if (walrusImageTran.isActive()) {
					walrusImageTran.commit();
				}
			}
		}
	}

	private static Account getEucalyptusAccount() throws Exception {
		if (eucalyptusAccount == null) {
			eucalyptusAccount = Accounts.lookupAccountByName(Account.SYSTEM_ACCOUNT);
		}
		return eucalyptusAccount;
	}

	private static User getEucalyptusAdmin() throws Exception {
		if (eucalyptusAdmin == null) {
			eucalyptusAdmin = getEucalyptusAccount().lookupAdmin();
		}
		return eucalyptusAdmin;
	}

	private static void createBlockStorageAccount() throws Exception {
		blockStorageAccount = Accounts.addSystemAccountWithAdmin(Account.BLOCKSTORAGE_SYSTEM_ACCOUNT);
	}

	private static Account getBlockStorageAccount() throws Exception {
		if (blockStorageAccount == null) {
			createBlockStorageAccount();
		}
		return blockStorageAccount;
	}

	private static User getBlockStorageAdmin() throws Exception {
		if (blockStorageAdmin == null) {
			blockStorageAdmin = getBlockStorageAccount().lookupAdmin();
		}
		return blockStorageAdmin;
	}

	private static void populateSnapshotBucketsAndObjects() {
		EntityTransaction tran = Entities.get(WalrusSnapshotInfo.class);
		try {
			List<WalrusSnapshotInfo> walrusSnapshots = Entities.query(new WalrusSnapshotInfo(), Boolean.TRUE);
			for (WalrusSnapshotInfo walrusSnapshot : walrusSnapshots) {
				walrusSnapshotBuckets.add(walrusSnapshot.getSnapshotBucket());
				walrusSnapshotObjects.add(walrusSnapshot.getSnapshotId());
			}
			tran.commit();
		} catch (Exception e) {
			LOG.error("Failed to lookup snapshots stored in Walrus", e);
			tran.rollback();
			throw e;
		} finally {
			if (tran.isActive()) {
				tran.commit();
			}
		}
	}

	private static void generateCanonicaIDs() throws Exception {
		EntityTransaction tran = Entities.get(AccountEntity.class);
		try {
			List<AccountEntity> accounts = Entities.query(new AccountEntity());
			if (accounts != null && accounts.size() > 0) {
				for (AccountEntity account : accounts) {
					if (account.getCanonicalId() == null || account.getCanonicalId().equals("")) {
						account.populateCanonicalId();
						LOG.debug("Assigning canonical id " + account.getCanonicalId() + " for account " + account.getAccountNumber());
					}
				}
			}
			tran.commit();
		} catch (Exception e) {
			LOG.error("Failed to generate and assign canonical ids", e);
			tran.rollback();
			throw e;
		} finally {
			if (tran.isActive()) {
				tran.commit();
			}
		}
	}

	private static ArrayList<Grant> getBucketGrants(BucketInfo walrusBucket) throws Exception {
		ArrayList<Grant> grants = new ArrayList<Grant>();
		walrusBucket.readPermissions(grants); // Add global grants
		grants = convertGrantInfosToGrants(grants, walrusBucket.getGrants()); // Add account/group specific grant
		return grants;
	}

	private static ArrayList<Grant> getObjectGrants(ObjectInfo walrusObject) throws Exception {
		ArrayList<Grant> grants = new ArrayList<Grant>();
		walrusObject.readPermissions(grants); // Add global grants
		grants = convertGrantInfosToGrants(grants, walrusObject.getGrants()); // Add account/group specific grant
		return grants;
	}

	private static ArrayList<Grant> convertGrantInfosToGrants(ArrayList<Grant> grants, List<GrantInfo> grantInfos) throws Exception {
		if (grants == null) {
			grants = new ArrayList<Grant>();
		}
		if (grantInfos == null) {
			// nothing to do here
			return grants;
		}

		for (GrantInfo grantInfo : grantInfos) {
			if (grantInfo.getGrantGroup() != null) {
				// Add it as a group
				Group group = new Group(grantInfo.getGrantGroup());
				transferPermissions(grants, grantInfo, new Grantee(group));
			} else {
				// Assume it's a user/account
				Account account = null;
				if (accountIdAccountMap.containsKey(grantInfo.getUserId())) {
					account = accountIdAccountMap.get(grantInfo.getUserId());
				} else if (deletedAccountIds.contains(grantInfo.getUserId())) {// In case the account is deleted, skip the grant
					LOG.warn("Account ID " + grantInfo.getUserId() + " does not not exist. Skipping this grant");
					continue;
				} else if (noCanonicalIdAccountIds.contains(grantInfo.getUserId())) { // If canonical ID is missing, use the eucalyptus admin account
					LOG.warn("Account ID " + grantInfo.getUserId() + " does not not have a canonical ID. Skipping this grant");
					continue;
				} else {
					try {
						// Lookup owning account
						account = Accounts.lookupAccountById(grantInfo.getUserId());
						if (StringUtils.isBlank(grantInfo.getUserId())) { // If canonical ID is missing, use the eucalyptus admin account
							LOG.warn("Account ID " + grantInfo.getUserId() + " does not not have a canonical ID. Skipping this grant");
							noCanonicalIdAccountIds.add(grantInfo.getUserId());
							continue;
						} else {
							// Add it to the map
							accountIdAccountMap.put(grantInfo.getUserId(), account);
						}
					} catch (Exception e) { // In case the account is deleted, skip the grant
						LOG.warn("Account ID " + grantInfo.getUserId() + " does not not exist. Skipping this grant");
						deletedAccountIds.add(grantInfo.getUserId());
						continue;
					}
				}

				CanonicalUser user = new CanonicalUser(account.getCanonicalId(), account.getName());
				transferPermissions(grants, grantInfo, new Grantee(user));
			}
		}

		return grants;
	}

	private static void transferPermissions(List<Grant> grants, GrantInfo grantInfo, Grantee grantee) {
		if (grantInfo.canRead() && grantInfo.canWrite() && grantInfo.canReadACP() && grantInfo.canWriteACP()) {
			grants.add(new Grant(grantee, ObjectStorageProperties.Permission.FULL_CONTROL.toString()));
			return;
		}

		if (grantInfo.canRead()) {
			grants.add(new Grant(grantee, ObjectStorageProperties.Permission.READ.toString()));
		}

		if (grantInfo.canWrite()) {
			grants.add(new Grant(grantee, ObjectStorageProperties.Permission.WRITE.toString()));
		}

		if (grantInfo.canReadACP()) {
			grants.add(new Grant(grantee, ObjectStorageProperties.Permission.READ_ACP.toString()));
		}

		if (grantInfo.canWriteACP()) {
			grants.add(new Grant(grantee, ObjectStorageProperties.Permission.WRITE_ACP.toString()));
		}
	}

	/**
	 * This method transforms a Walrus bucket to an OSG bucket. While the appropriate fields are copied over from the Walrus entity to OSG entity when
	 * available, the process includes the following additional steps
	 * 
	 * <li>Copy the bucketName in Walrus entity to bucketName and bucketUuid of the OSG entity</li>
	 * 
	 * <li>If any account information is missing due to unavailable/deleted accounts, transfer the ownership of the bucket to the Eucalyptus account</li>
	 * 
	 * <li>If the user associated with the bucket is unavailable, transfer the IAM ownership to either the admin of the owning account if available or the
	 * Eucalyptus account admin</li>
	 * 
	 * <li>Skip the grant if the grant owner cannot be retrieved</li>
	 * 
	 * <li>Transfer the ownership of Snapshot buckets to the blockstorage system account and configure the ACL to private</li>
	 */
	public static Function<BucketInfo, Bucket> bucketTransformationFunction() {
		return new Function<BucketInfo, Bucket>() {

			@Override
			@Nullable
			public Bucket apply(@Nonnull BucketInfo walrusBucket) {
				Bucket osgBucket = null;
				try {
					Account owningAccount = null;
					User owningUser = null;

					// Get the owning account
					if (walrusSnapshotBuckets.contains(walrusBucket.getBucketName())) { // If its a snapshot bucket, set the owner to blockstorage account
						LOG.warn("Changing the ownership of snapshot bucket " + walrusBucket.getBucketName() + " to blockstorage system account");
						owningAccount = getBlockStorageAccount();
						owningUser = getBlockStorageAdmin();
					} else if (accountIdAccountMap.containsKey(walrusBucket.getOwnerId())) { // If account was previously looked up, get it from the map
						owningAccount = accountIdAccountMap.get(walrusBucket.getOwnerId());
					} else if (deletedAccountIds.contains(walrusBucket.getOwnerId())) { // If the account is deleted, use the eucalyptus admin account
						LOG.warn("Account ID " + walrusBucket.getOwnerId() + " does not not exist. Changing the ownership of bucket "
								+ walrusBucket.getBucketName() + " to eucalyptus admin account");
						owningAccount = getEucalyptusAccount();
						owningUser = getEucalyptusAdmin();
					} else if (noCanonicalIdAccountIds.contains(walrusBucket.getOwnerId())) { // If canonical ID is missing, use eucalyptus admin account
						LOG.warn("Account ID " + walrusBucket.getOwnerId() + " does not have a canonical ID. Changing the ownership of bucket "
								+ walrusBucket.getBucketName() + " to eucalyptus admin account");
						owningAccount = getEucalyptusAccount();
						owningUser = getEucalyptusAdmin();
					} else { // If none of the above conditions match, lookup for the account
						try {
							owningAccount = Accounts.lookupAccountById(walrusBucket.getOwnerId());
							if (StringUtils.isBlank(owningAccount.getCanonicalId())) { // If canonical ID is missing, use eucalyptus admin account
								LOG.warn("Account ID " + walrusBucket.getOwnerId() + " does not have a canonical ID. Changing the ownership of bucket "
										+ walrusBucket.getBucketName() + " to eucalyptus admin account");
								owningAccount = getEucalyptusAccount();
								owningUser = getEucalyptusAdmin();
								noCanonicalIdAccountIds.add(walrusBucket.getOwnerId());
							} else {
								accountIdAccountMap.put(walrusBucket.getOwnerId(), owningAccount);
							}
						} catch (AuthException e) { // In case the account is deleted, transfer the ownership to eucalyptus admin
							LOG.warn("Account ID " + walrusBucket.getOwnerId() + " does not not exist. Changing the ownership of bucket "
									+ walrusBucket.getBucketName() + " to eucalyptus admin account");
							owningAccount = getEucalyptusAccount();
							owningUser = getEucalyptusAdmin();
							deletedAccountIds.add(walrusBucket.getOwnerId());
							deletedUserIds.add(walrusBucket.getUserId());
						}
					}

					// Get the owning user if its not already set
					if (owningUser == null) {
						if (userIdUserMap.containsKey(walrusBucket.getUserId())) { // If the user was previously looked up, get it from the map
							owningUser = userIdUserMap.get(walrusBucket.getUserId());
						} else if (deletedUserIds.contains(walrusBucket.getUserId()) && accountIdAdminMap.containsKey(walrusBucket.getOwnerId())) {
							// If the user was deleted and the admin for the account was previously looked up, get it from the map
							LOG.warn("User ID " + walrusBucket.getUserId() + " does not exist. Changing the IAM ownership of bucket "
									+ walrusBucket.getBucketName() + " to the account admin");
							owningUser = accountIdAdminMap.get(walrusBucket.getOwnerId());
						} else if (deletedUserIds.contains(walrusBucket.getUserId()) && deletedAdminAccountIds.contains(walrusBucket.getOwnerId())) {
							// If the user was deleted and the account was also deleted, transfer the IAM ownership to eucalyptus admin
							LOG.warn("User ID " + walrusBucket.getUserId() + " and the account admin do not exist. Changing the IAM ownership of bucket "
									+ walrusBucket.getBucketName() + " to the eucalyptus account admin");
							owningUser = getEucalyptusAdmin();
						} else { // If none of the above conditions match, lookup for the user
							if (walrusBucket.getUserId() != null) {
								try {
									owningUser = Accounts.lookupUserById(walrusBucket.getUserId());
									userIdUserMap.put(walrusBucket.getUserId(), owningUser);
								} catch (AuthException e) { // User is deleted, lookup for the account admin
									deletedUserIds.add(walrusBucket.getUserId());
									try {
										owningUser = owningAccount.lookupAdmin();
										accountIdAdminMap.put(walrusBucket.getOwnerId(), owningUser);
										LOG.warn("User ID " + walrusBucket.getUserId() + " does not exist. Changing the IAM ownership of bucket "
												+ walrusBucket.getBucketName() + " to the account admin");
									} catch (AuthException ie) { // User and admin are both deleted, transfer the IAM ownership to the eucalyptus admin
										LOG.warn("User ID " + walrusBucket.getUserId()
												+ " and the account admin do not exist. Changing the IAM ownership of bucket " + walrusBucket.getBucketName()
												+ " to the eucalyptus account admin");
										owningUser = getEucalyptusAdmin();
										deletedAdminAccountIds.add(walrusBucket.getOwnerId());
									}
								}
							} else { // If no owner ID was found for the bucket, set user to account admin or eucalyptus admin.
								// This is to avoid insert null IDs into cached sets/maps
								if (accountIdAdminMap.containsKey(walrusBucket.getOwnerId())) {
									// If the admin to the account was looked up previously, get it from the map
									LOG.warn("No user ID listed for bucket " + walrusBucket.getBucketName()
											+ ". Changing the IAM ownership of bucket to the account admin");
									owningUser = accountIdAdminMap.get(walrusBucket.getBucketName());
								} else { // Lookup up the admin if its not available in the map
									try {
										owningUser = owningAccount.lookupAdmin();
										accountIdAdminMap.put(walrusBucket.getOwnerId(), owningUser);
										LOG.warn("No user ID listed for bucket " + walrusBucket.getBucketName()
												+ ". Changing the IAM ownership of bucket to the account admin");
									} catch (AuthException ie) {// User and admin are both deleted, transfer the IAM ownership to the eucalyptus admin
										LOG.warn("No user ID listed for bucket " + walrusBucket.getBucketName()
												+ " and account admin does not exist. Changing the IAM ownership of bucket to the eucalyptus account admin");
										owningUser = getEucalyptusAdmin();
									}
								}
							}
						}
					}

					// Create a new instance of osg bucket and popluate all the fields
					osgBucket = new Bucket();
					osgBucket.setBucketName(walrusBucket.getBucketName());
					osgBucket.withUuid(walrusBucket.getBucketName());
					osgBucket.setBucketSize(walrusBucket.getBucketSize());
					osgBucket.setLocation(walrusBucket.getLocation());
					osgBucket.setLoggingEnabled(walrusBucket.getLoggingEnabled());
					osgBucket.setState(BucketState.extant);
					osgBucket.setLastState(BucketState.creating); // Set the last state after setting the current state
					osgBucket.setTargetBucket(walrusBucket.getTargetBucket());
					osgBucket.setTargetPrefix(walrusBucket.getTargetPrefix());
					osgBucket.setVersioning(VersioningStatus.valueOf(walrusBucket.getVersioning()));

					// Set the owner and IAM user fields
					osgBucket.setOwnerCanonicalId(owningAccount.getCanonicalId());
					osgBucket.setOwnerDisplayName(owningAccount.getName());
					osgBucket.setOwnerIamUserId(owningUser.getUserId());
					osgBucket.setOwnerIamUserDisplayName(owningUser.getName());

					// Generate access control policy
					AccessControlList acl = new AccessControlList();
					if (walrusSnapshotBuckets.contains(walrusBucket.getBucketName())) { // Dont set any grants for a snapshot bucket
						acl.setGrants(new ArrayList<Grant>());
					} else {
						acl.setGrants(getBucketGrants(walrusBucket));
					}
					AccessControlPolicy acp = new AccessControlPolicy(new CanonicalUser(owningAccount.getCanonicalId(), owningAccount.getName()), acl);
					osgBucket.setAcl(acp);
				} catch (Exception e) {
					LOG.error("Failed to transform Walrus bucket " + walrusBucket.getBucketName() + " to objectstorage bucket", e);
					Exceptions.toUndeclared("Failed to transform Walrus bucket " + walrusBucket.getBucketName() + " to objectstorage bucket", e);
				}
				return osgBucket;
			}
		};
	}

	/**
	 * This method transforms a Walrus object to an OSG object. While the appropriate fields are copied over from the Walrus entity to OSG entity when
	 * available, the process includes the following additional steps
	 * 
	 * <li>For delete markers, generate the objectUuid, set the ownership to bucket owner and the leave the grants empty</li>
	 * 
	 * <li>OSG refers to the backend object using the objectUuid. Use objectName of Walrus entity as the objectUuid in OSG entity. Second part of this step is
	 * to overwrite the objectKey with the objectName in the Walrus entity. This is executed in the {@code ModifyWalrusBuckets} stage</li>
	 * 
	 * <li>If any account information is missing due to unavailable/deleted accounts, transfer the ownership of the object to the Eucalyptus account</li>
	 * 
	 * <li>Since Walrus does not keep track of the user that created the object, transfer the IAM ownership to either the admin of the owning account if
	 * available or the Eucalyptus account admin</li>
	 * 
	 * <li>Skip the grant if the grant owner cannot be retrieved</li>
	 * 
	 * <li>Transfer the ownership of Snapshot objects to the blockstorage system account and configure the ACL to private</li>
	 */
	public static Function<ObjectInfo, ObjectEntity> objectTransformationFunction() {
		return new Function<ObjectInfo, ObjectEntity>() {

			@Override
			@Nullable
			public ObjectEntity apply(@Nonnull ObjectInfo walrusObject) {
				ObjectEntity osgObject = null;
				try {
					Bucket osgBucket = null;
					if (bucketMap.containsKey(walrusObject.getBucketName())) {
						osgBucket = bucketMap.get(walrusObject.getBucketName());
					} else {
						osgBucket = Transactions.find(new Bucket(walrusObject.getBucketName()));
						bucketMap.put(walrusObject.getBucketName(), osgBucket);
					}

					osgObject = new ObjectEntity(osgBucket, walrusObject.getObjectKey(), walrusObject.getVersionId());

					if (walrusObject.getDeleted() != null && walrusObject.getDeleted()) { // delete marker
						osgObject.setObjectUuid(UUID.randomUUID().toString());
						osgObject.setStorageClass(ObjectStorageProperties.STORAGE_CLASS.STANDARD.toString());
						osgObject.setObjectModifiedTimestamp(walrusObject.getLastModified());
						osgObject.setIsDeleteMarker(Boolean.TRUE);
						osgObject.setSize(0L);
						osgObject.setIsLatest(walrusObject.getLast());
						osgObject.setState(ObjectState.extant);

						// Set the ownership to bucket owner as the bucket owning account/user
						osgObject.setOwnerCanonicalId(osgBucket.getOwnerCanonicalId());
						osgObject.setOwnerDisplayName(osgBucket.getOwnerDisplayName());
						osgObject.setOwnerIamUserId(osgBucket.getOwnerIamUserId());
						osgObject.setOwnerIamUserDisplayName(osgBucket.getOwnerIamUserDisplayName());

						// Generate empty access control policy, OSG should set it to private acl for the owner
						AccessControlList acl = new AccessControlList();
						acl.setGrants(new ArrayList<Grant>());
						AccessControlPolicy acp = new AccessControlPolicy(new CanonicalUser(osgBucket.getOwnerCanonicalId(), osgBucket.getOwnerDisplayName()),
								acl);
						osgObject.setAcl(acp);

					} else { // not a delete marker

						Account owningAccount = null;
						User adminUser = null;

						// Get the owning account
						if (walrusSnapshotObjects.contains(walrusObject.getObjectKey())) {// If its a snapshot object, set the owner to blockstorage account
							LOG.warn("Changing the ownership of snapshot object " + walrusObject.getObjectKey() + " to blockstorage system account");
							owningAccount = getBlockStorageAccount();
							adminUser = getBlockStorageAdmin();
						} else if (accountIdAccountMap.containsKey(walrusObject.getOwnerId())) { // If account was previously looked up, get it from the map
							owningAccount = accountIdAccountMap.get(walrusObject.getOwnerId());
						} else if (deletedAccountIds.contains(walrusObject.getOwnerId())) { // If the account is deleted, use the eucalyptus admin account
							// Account is deleted, transfer the entire ownership to eucalyptus account admin
							LOG.warn("Account ID " + walrusObject.getOwnerId() + " does not not exist. Changing the ownership of object "
									+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to eucalyptus admin account");
							owningAccount = getEucalyptusAccount();
							adminUser = getEucalyptusAdmin();
						} else if (noCanonicalIdAccountIds.contains(walrusObject.getOwnerId())) { // If canonical ID is missing, use eucalyptus admin account
							LOG.warn("Account ID " + walrusObject.getOwnerId() + " does not have a canonical ID. Changing the ownership of object "
									+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to eucalyptus admin account");
							owningAccount = getEucalyptusAccount();
							adminUser = getEucalyptusAdmin();
						} else { // If none of the above conditions match, lookup for the account
							try {
								owningAccount = Accounts.lookupAccountById(walrusObject.getOwnerId());
								if (StringUtils.isBlank(owningAccount.getCanonicalId())) {
									LOG.warn("Account ID " + walrusObject.getOwnerId() + " does not have a canonical ID. Changing the ownership of object "
											+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to eucalyptus admin account");
									owningAccount = getEucalyptusAccount();
									adminUser = getEucalyptusAdmin();
									noCanonicalIdAccountIds.add(walrusObject.getOwnerId());
								} else {
									accountIdAccountMap.put(walrusObject.getOwnerId(), owningAccount);
								}
							} catch (AuthException e) { // In case the account is deleted, transfer the ownership to eucalyptus admin
								LOG.warn("Account ID " + walrusObject.getOwnerId() + " does not not exist. Changing the ownership of object "
										+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to eucalyptus admin account");
								owningAccount = getEucalyptusAccount();
								adminUser = getEucalyptusAdmin();
								deletedAccountIds.add(walrusObject.getOwnerId());
							}

						}

						// Get the admin user since we dont know which user actually created it
						if (adminUser == null) {
							if (accountIdAdminMap.containsKey(walrusObject.getOwnerId())) { // If the admin user was previously looked up, get it from the map
								adminUser = accountIdAdminMap.get(walrusObject.getOwnerId());
							} else if (deletedAdminAccountIds.contains(walrusObject.getOwnerId())) { // If the account admin was deleted, transfer the IAM
																										// ownership to eucalyptus admin
								LOG.warn("Admin for account ID " + walrusObject.getOwnerId() + " does not exist. Changing the IAM ownership of object "
										+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to the eucalyptus account admin");
								adminUser = getEucalyptusAdmin();
							} else { // If none of the above conditions match, lookup for the admin
								try {
									adminUser = owningAccount.lookupAdmin();
									accountIdAdminMap.put(walrusObject.getOwnerId(), adminUser);
								} catch (AuthException e) {
									LOG.warn("Admin for account ID " + walrusObject.getOwnerId() + " does not exist. Changing the IAM ownership of object "
											+ walrusObject.getObjectKey() + " in bucket " + walrusObject.getBucketName() + " to the eucalyptus account admin");
									adminUser = getEucalyptusAdmin();
									deletedAdminAccountIds.add(walrusObject.getOwnerId());
								}
							}
						}

						osgObject.seteTag(walrusObject.getEtag());
						osgObject.setIsDeleteMarker(Boolean.FALSE);
						osgObject.setIsLatest(walrusObject.getLast());
						osgObject.setObjectModifiedTimestamp(walrusObject.getLastModified());
						osgObject.setObjectUuid(walrusObject.getObjectName()); // Copy Walrus objectName to OSG objectUuid
						osgObject.setSize(walrusObject.getSize());
						osgObject.setState(ObjectState.extant);
						osgObject.setLastState(ObjectState.creating); // Set the last state after setting the current state
						osgObject.setStorageClass(walrusObject.getStorageClass());

						// Set the owner and IAM user fields
						osgObject.setOwnerCanonicalId(owningAccount.getCanonicalId());
						osgObject.setOwnerDisplayName(owningAccount.getName());
						osgObject.setOwnerIamUserId(adminUser.getUserId());
						osgObject.setOwnerIamUserDisplayName(adminUser.getName());

						// Generate access control policy
						AccessControlList acl = new AccessControlList();
						if (walrusSnapshotObjects.contains(walrusObject.getObjectKey())) { // Dont set any grants for a snapshot object or delete markers
							acl.setGrants(new ArrayList<Grant>());
						} else {
							acl.setGrants(getObjectGrants(walrusObject));
						}
						AccessControlPolicy acp = new AccessControlPolicy(new CanonicalUser(owningAccount.getCanonicalId(), owningAccount.getName()), acl);
						osgObject.setAcl(acp);
					}
				} catch (Exception e) {
					LOG.error("Failed to transform Walrus object " + walrusObject.getObjectKey() + " to objectstorage object", e);
					Exceptions.toUndeclared("Failed to transform Walrus object " + walrusObject.getObjectKey() + " to objectstorage object", e);
				}
				return osgObject;
			}
		};
	}

	public static Function<ImageCacheInfo, ObjectEntity> imageToOSGObjectTransformation() {
		return new Function<ImageCacheInfo, ObjectEntity>() {

			@Override
			@Nullable
			public ObjectEntity apply(@Nonnull ImageCacheInfo image) {
				ObjectEntity osgObject = null;
				try {
					Bucket osgBucket = null;
					if (bucketMap.containsKey(image.getBucketName())) {
						osgBucket = bucketMap.get(image.getBucketName());
					} else {
						osgBucket = Transactions.find(new Bucket(image.getBucketName()));
						bucketMap.put(image.getBucketName(), osgBucket);
					}

					// Create OSG object with name set to manifest
					osgObject = new ObjectEntity(osgBucket, image.getManifestName(), ObjectStorageProperties.NULL_VERSION_ID);
					osgObject.setIsDeleteMarker(Boolean.FALSE);
					osgObject.setIsLatest(Boolean.TRUE);
					osgObject.setObjectUuid(image.getImageName()); // set the uuid to file name on filesystem
					osgObject.setObjectModifiedTimestamp(new Date());
					osgObject.setSize(image.getSize());
					osgObject.setStorageClass(ObjectStorageProperties.STORAGE_CLASS.STANDARD.toString());
					osgObject.setState(ObjectState.deleting);
					osgObject.setLastState(ObjectState.extant);

					// Set the owner and IAM user fields
					osgObject.setOwnerCanonicalId(getEucalyptusAccount().getCanonicalId());
					osgObject.setOwnerDisplayName(getEucalyptusAccount().getName());
					osgObject.setOwnerIamUserDisplayName(getEucalyptusAdmin().getName());
					osgObject.setOwnerIamUserId(getEucalyptusAdmin().getUserId());

					// Generate access control policy
					AccessControlList acl = new AccessControlList();
					acl.setGrants(new ArrayList<Grant>());
					AccessControlPolicy acp = new AccessControlPolicy(new CanonicalUser(osgObject.getOwnerCanonicalId(), osgObject.getOwnerDisplayName()), acl);
					osgObject.setAcl(acp);
				} catch (Exception e) {
					LOG.error("Failed to create objectstorage object for cached image " + image.getManifestName(), e);
					Exceptions.toUndeclared("Failed to create objectstorage object for cached image " + image.getManifestName(), e);
				}
				return osgObject;
			}
		};
	}

	public static Function<ImageCacheInfo, ObjectInfo> imageToWalrusObjectTransformation() {
		return new Function<ImageCacheInfo, ObjectInfo>() {

			@Override
			@Nullable
			public ObjectInfo apply(@Nonnull ImageCacheInfo image) {
				ObjectInfo walrusObject = null;
				try {
					// Set the image location on the filesystem as the key
					walrusObject = new ObjectInfo(image.getBucketName(), image.getImageName());
					walrusObject.setDeleted(Boolean.FALSE);
					walrusObject.setLast(Boolean.TRUE);
					walrusObject.setLastModified(new Date());
					walrusObject.setObjectName(image.getImageName()); // location of the file on the file system
					walrusObject.setOwnerId(getEucalyptusAccount().getAccountNumber());
					walrusObject.setSize(image.getSize());
					walrusObject.setStorageClass(ObjectStorageProperties.STORAGE_CLASS.STANDARD.toString());
					walrusObject.setVersionId(WalrusProperties.NULL_VERSION_ID);

					// Reset the ACLs and assign the owner full control
					walrusObject.resetGlobalGrants();
					List<GrantInfo> grantInfos = new ArrayList<GrantInfo>();
					GrantInfo.setFullControl(walrusObject.getOwnerId(), grantInfos);
					walrusObject.setGrants(grantInfos);
				} catch (Exception e) {
					LOG.error("Failed to create Walrus object for cached image " + image.getManifestName(), e);
					Exceptions.toUndeclared("Failed to create Walrus object for cached image " + image.getManifestName(), e);
				}
				return walrusObject;
			}
		};
	}
}
