/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/

package com.eucalyptus.objectstorage;

import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketLifecycleType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadObjectType;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
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
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLifecycleType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Primary interface for the OSG component. The set of S3 operations supported by Eucalyptus
 *
 */
public interface ObjectStorageService {

	public abstract UpdateObjectStorageConfigurationResponseType updateObjectStorageConfiguration(
			UpdateObjectStorageConfigurationType request)
			throws EucalyptusCloudException;

	public abstract GetObjectStorageConfigurationResponseType getObjectStorageConfiguration(
			GetObjectStorageConfigurationType request)
			throws EucalyptusCloudException;

	public abstract HeadBucketResponseType headBucket(HeadBucketType request)
			throws S3Exception;

	public abstract CreateBucketResponseType createBucket(
			CreateBucketType request) throws S3Exception;

	public abstract DeleteBucketResponseType deleteBucket(
			DeleteBucketType request) throws S3Exception;

	public abstract ListAllMyBucketsResponseType listAllMyBuckets(
			ListAllMyBucketsType request) throws S3Exception;

	public abstract PostObjectResponseType postObject(PostObjectType request)
			throws S3Exception;

	public abstract DeleteObjectResponseType deleteObject(
			DeleteObjectType request) throws S3Exception;

	public abstract ListBucketResponseType listBucket(ListBucketType request)
			throws S3Exception;

	public abstract GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
			throws S3Exception;

	public abstract SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
			SetBucketAccessControlPolicyType request)
			throws S3Exception;


	public abstract GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
			GetObjectAccessControlPolicyType request)
			throws S3Exception;

	public abstract SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
			SetObjectAccessControlPolicyType request)
			throws S3Exception;

	public abstract PutObjectResponseType putObject(PutObjectType request) throws S3Exception;
	
	public abstract GetObjectResponseType getObject(GetObjectType request)
			throws S3Exception;

	public abstract GetObjectExtendedResponseType getObjectExtended(
			GetObjectExtendedType request) throws S3Exception;

	public abstract HeadObjectResponseType headObject(HeadObjectType request)
			throws S3Exception;

	public abstract GetBucketLocationResponseType getBucketLocation(
			GetBucketLocationType request) throws S3Exception;

	public abstract CopyObjectResponseType copyObject(CopyObjectType request)
			throws S3Exception;

	public abstract GetBucketLoggingStatusResponseType getBucketLoggingStatus(
			GetBucketLoggingStatusType request) throws S3Exception;

	public abstract SetBucketLoggingStatusResponseType setBucketLoggingStatus(
			SetBucketLoggingStatusType request) throws S3Exception;

	public abstract GetBucketVersioningStatusResponseType getBucketVersioningStatus(
			GetBucketVersioningStatusType request)
			throws S3Exception;

	public abstract SetBucketVersioningStatusResponseType setBucketVersioningStatus(
			SetBucketVersioningStatusType request)
			throws S3Exception;

	public abstract ListVersionsResponseType listVersions(
			ListVersionsType request) throws S3Exception;

	public abstract DeleteVersionResponseType deleteVersion(
			DeleteVersionType request) throws S3Exception;
	
	
	public abstract InitiateMultipartUploadResponseType initiateMultipartUpload(
			InitiateMultipartUploadType request) throws S3Exception;
	
	public abstract UploadPartResponseType uploadPart(
			UploadPartType request) throws S3Exception;
	
	public abstract CompleteMultipartUploadResponseType completeMultipartUpload(
			CompleteMultipartUploadType request) throws S3Exception;
	
	public abstract AbortMultipartUploadResponseType abortMultipartUpload(
			AbortMultipartUploadType request) throws S3Exception;

    public abstract GetBucketLifecycleResponseType getBucketLifecycle(
            GetBucketLifecycleType request) throws S3Exception;

    public abstract SetBucketLifecycleResponseType setBucketLifecycle(
            SetBucketLifecycleType request) throws S3Exception;

    public abstract DeleteBucketLifecycleResponseType deleteBucketLifecycle(
            DeleteBucketLifecycleType request) throws S3Exception;

}