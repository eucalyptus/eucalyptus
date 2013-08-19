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

package com.eucalyptus.objectstorage;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.objectstorage.Walrus;
import com.eucalyptus.objectstorage.bittorrent.Tracker;
import com.eucalyptus.objectstorage.entities.WalrusInfo;
import com.eucalyptus.objectstorage.exceptions.AccessDeniedException;
import com.eucalyptus.objectstorage.msgs.AddObjectResponseType;
import com.eucalyptus.objectstorage.msgs.AddObjectType;
import com.eucalyptus.objectstorage.msgs.CacheImageResponseType;
import com.eucalyptus.objectstorage.msgs.CacheImageType;
import com.eucalyptus.objectstorage.msgs.CheckImageResponseType;
import com.eucalyptus.objectstorage.msgs.CheckImageType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.DeleteWalrusSnapshotResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteWalrusSnapshotType;
import com.eucalyptus.objectstorage.msgs.FlushCachedImageResponseType;
import com.eucalyptus.objectstorage.msgs.FlushCachedImageType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.GetDecryptedImageResponseType;
import com.eucalyptus.objectstorage.msgs.GetDecryptedImageType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.GetWalrusConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.GetWalrusConfigurationType;
import com.eucalyptus.objectstorage.msgs.GetWalrusSnapshotResponseType;
import com.eucalyptus.objectstorage.msgs.GetWalrusSnapshotSizeResponseType;
import com.eucalyptus.objectstorage.msgs.GetWalrusSnapshotSizeType;
import com.eucalyptus.objectstorage.msgs.GetWalrusSnapshotType;
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType;
import com.eucalyptus.objectstorage.msgs.HeadBucketType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectInlineResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectInlineType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetRESTBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetRESTBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetRESTObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetRESTObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.StoreSnapshotResponseType;
import com.eucalyptus.objectstorage.msgs.StoreSnapshotType;
import com.eucalyptus.objectstorage.msgs.UpdateWalrusConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.UpdateWalrusConfigurationType;
import com.eucalyptus.objectstorage.msgs.ValidateImageResponseType;
import com.eucalyptus.objectstorage.msgs.ValidateImageType;
import com.eucalyptus.objectstorage.msgs.WalrusDataMessenger;
import com.eucalyptus.objectstorage.util.WalrusProperties;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.Checker;
import com.eucalyptus.troubleshooting.checker.DiskResourceCheck.LocationInfo;
import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class WalrusControl {

	private static Logger LOG = Logger.getLogger( WalrusControl.class );

	private static WalrusDataMessenger imageMessenger = new WalrusDataMessenger();
	private static StorageManager storageManager;
	private static WalrusManager walrusManager;
	private static WalrusBlockStorageManager walrusBlockStorageManager;
	private static WalrusImageManager walrusImageManager;

	public static void checkPreconditions() throws EucalyptusCloudException, ExecutionException {
		// TODO Auto-generated method stub
		String returnValue;
		returnValue = SystemUtil.run(new String[]{WalrusProperties.EUCA_ROOT_WRAPPER, "drbdadm", "status"});
		if(returnValue.length() == 0) {
		  Faults.advisory(Components.lookup(Walrus.class).getLocalServiceConfiguration(), 
		                                      new EucalyptusCloudException("drbdadm not found: Is drbd installed?"));
		}
	}	

	public static void configure() {
		WalrusInfo walrusInfo = WalrusInfo.getWalrusInfo();
        try {
			storageManager = BackendStorageManagerFactory.getStorageManager();
		} catch (Exception ex) {
			LOG.error (ex);
		}
		walrusImageManager = new WalrusImageManager(storageManager, imageMessenger);
		walrusManager = new WalrusManager(storageManager, walrusImageManager);
		WalrusManager.configure();
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
		//Disable torrents
		//Tracker.initialize();
		if(System.getProperty("euca.virtualhosting.disable") != null) {
			WalrusProperties.enableVirtualHosting = false;
		}
		try {
			if (storageManager != null) {
				storageManager.start();
			}
		} catch(EucalyptusCloudException ex) {
			LOG.error("Error starting storage backend: " + ex);			
		}
		
		// Implementation for EUCA-3583. Check for available space in Walrus bukkits directory and throw a fault when less than 10% of total space is available
		try {
			ScheduledFuture<?> future = DiskResourceCheck.start(new Checker(
					new LocationInfo(new File(WalrusInfo.getWalrusInfo().getStorageDir()), 10.0), Walrus.class, (long) 300000));
		} catch (Exception ex) {
			LOG.error("Error starting disk space check for Walrus storage directory.", ex);
		}
	}

	public WalrusControl() {}

	public static void enable() throws EucalyptusCloudException {
		storageManager.enable();
	}

	public static void disable() throws EucalyptusCloudException {
		storageManager.disable();
	}

	public static void check() throws EucalyptusCloudException {
		storageManager.check();
	}

	public static void stop() throws EucalyptusCloudException {
		storageManager.stop();
		Tracker.die();
		storageManager = null;
		walrusBlockStorageManager =null;
		walrusManager = null;
		walrusImageManager = null;
		WalrusProperties.shouldEnforceUsageLimits = true;
		WalrusProperties.enableVirtualHosting = true;
	}

	public UpdateWalrusConfigurationResponseType UpdateWalrusConfiguration(UpdateWalrusConfigurationType request) throws EucalyptusCloudException {
		UpdateWalrusConfigurationResponseType reply = (UpdateWalrusConfigurationResponseType) request.getReply();
		if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		if(request.getProperties() != null) {
			for(ComponentProperty prop : request.getProperties()) {
				LOG.info("Walrus property: " + prop.getDisplayName() + " Qname: " + prop.getQualifiedName() + " Value: " + prop.getValue());
				try {
					ConfigurableProperty entry = PropertyDirectory.getPropertyEntry(prop.getQualifiedName());
					//type parser will correctly covert the value
					entry.setValue(prop.getValue());
				} catch (IllegalAccessException e) {
					LOG.error(e, e);
				}                               
			}
		}
		String name = request.getName();
		walrusManager.check();
		return reply;
	}

	public GetWalrusConfigurationResponseType GetWalrusConfiguration(GetWalrusConfigurationType request) throws EucalyptusCloudException {
		GetWalrusConfigurationResponseType reply = (GetWalrusConfigurationResponseType) request.getReply();
		ConfigurableClass configurableClass = WalrusInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String prefix = configurableClass.root();
			reply.setProperties((ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(prefix));
		}
		return reply;
	}

	public HeadBucketResponseType HeadBucket(HeadBucketType request) throws EucalyptusCloudException {
		return walrusManager.headBucket(request);
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

	public ValidateImageResponseType ValidateImage(ValidateImageType request) throws EucalyptusCloudException {
		return walrusImageManager.validateImage(request);
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

	public GetWalrusSnapshotSizeResponseType GetWalrusSnapshotSize(GetWalrusSnapshotSizeType request) throws EucalyptusCloudException {
		return walrusBlockStorageManager.getWalrusSnapshotSize(request);
	}

}
