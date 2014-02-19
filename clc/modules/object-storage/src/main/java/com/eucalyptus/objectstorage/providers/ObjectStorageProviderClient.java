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

package com.eucalyptus.objectstorage.providers;

import java.io.InputStream;

import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
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
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
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
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.HeadObjectType;
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
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.ListPartsResponseType;
import com.eucalyptus.objectstorage.msgs.ListPartsType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsResponseType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsType;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Class that any ObjectStorageProvider client implementation must extend.
 * This is the interface used by the ObjectStorageGateway to invoke operations
 * on the provider backend.
 * @author Zach Hill <zhill@eucalyptus.com>
 * @author Neil Soman <neil@eucalyptus.com>
 *
 */
public interface ObjectStorageProviderClient {
	/*
	 * Service lifecycle operations
	 */
	public void checkPreconditions() throws EucalyptusCloudException;
	public void initialize() throws EucalyptusCloudException;
	public void check() throws EucalyptusCloudException;
	public void start() throws EucalyptusCloudException;
	public void stop() throws EucalyptusCloudException;
	public void enable() throws EucalyptusCloudException;
	public void disable() throws EucalyptusCloudException;

	/* 
	 * -------------------------
	 * Service Operations
	 * -------------------------
	 */

	/**
	 * List all buckets accessible by the user.
	 * @param request
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public ListAllMyBucketsResponseType listAllMyBuckets(
			ListAllMyBucketsType request) throws EucalyptusCloudException;

	/*
	 * -------------------------
	 * Bucket Operations
	 * -------------------------
	 */
	/**
	 * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise
	 * returns 404 if not found or 403 if no accesss.
	 * @param request
	 * @return
	 * @throws S3Exception
	 */
	public HeadBucketResponseType headBucket(HeadBucketType request)
			throws S3Exception;

	public CreateBucketResponseType createBucket(
			CreateBucketType request) throws S3Exception;

	public DeleteBucketResponseType deleteBucket(
			DeleteBucketType request) throws S3Exception;

	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
					throws S3Exception;

	public ListBucketResponseType listBucket(ListBucketType request)
			throws S3Exception;

	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
			SetBucketAccessControlPolicyType request)
			throws S3Exception;
	
	public GetBucketLocationResponseType getBucketLocation(
			GetBucketLocationType request) throws S3Exception;

	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(
			SetBucketLoggingStatusType request) throws S3Exception;

	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(
			GetBucketLoggingStatusType request) throws S3Exception;

	public GetBucketVersioningStatusResponseType getBucketVersioningStatus(
			GetBucketVersioningStatusType request)
					throws S3Exception;

	public SetBucketVersioningStatusResponseType setBucketVersioningStatus(
			SetBucketVersioningStatusType request)
					throws S3Exception;

	public ListVersionsResponseType listVersions(
			ListVersionsType request) throws S3Exception;

	/*
	 * -------------------------
	 * Object Operations
	 * ------------------------- 
	 */
	public PutObjectResponseType putObject(PutObjectType request, InputStream inputData)
			throws S3Exception;

	public PostObjectResponseType postObject(PostObjectType request)
			throws S3Exception;

	public DeleteObjectResponseType deleteObject(
			DeleteObjectType request) throws S3Exception;

	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
			GetObjectAccessControlPolicyType request)
					throws S3Exception;

	public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
			SetObjectAccessControlPolicyType request)
					throws S3Exception;

	public GetObjectResponseType getObject(GetObjectType request)
			throws S3Exception;

	public GetObjectExtendedResponseType getObjectExtended(
			GetObjectExtendedType request) throws S3Exception;

	public HeadObjectResponseType headObject(HeadObjectType request)
			throws S3Exception;

	public CopyObjectResponseType copyObject(CopyObjectType request)
			throws S3Exception;

	public DeleteVersionResponseType deleteVersion(
			DeleteVersionType request) throws S3Exception;

	public abstract InitiateMultipartUploadResponseType initiateMultipartUpload(
			InitiateMultipartUploadType request) throws S3Exception;

	public abstract UploadPartResponseType uploadPart(
			UploadPartType request, InputStream dataContent) throws S3Exception;

	public abstract CompleteMultipartUploadResponseType completeMultipartUpload(
			CompleteMultipartUploadType request) throws S3Exception;

	public abstract AbortMultipartUploadResponseType abortMultipartUpload(
			AbortMultipartUploadType request) throws S3Exception;

	public abstract ListPartsResponseType listParts(
			ListPartsType request) throws S3Exception;
	
	public abstract ListMultipartUploadsResponseType listMultipartUploads(
			ListMultipartUploadsType request) throws S3Exception;
	

}