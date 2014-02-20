package com.eucalyptus.walrus;

import java.net.InetAddress;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.entities.BucketInfo;
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
import com.eucalyptus.walrus.msgs.GetLifecycleResponseType;
import com.eucalyptus.walrus.msgs.GetLifecycleType;

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
import com.eucalyptus.walrus.msgs.PutLifecycleResponseType;
import com.eucalyptus.walrus.msgs.PutLifecycleType;

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
import com.eucalyptus.walrus.util.WalrusProperties;
import com.eucalyptus.walrus.msgs.UploadPartResponseType;
import com.eucalyptus.walrus.msgs.UploadPartType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadType;

public abstract class WalrusManager {

	public static void configure() {}

	public abstract void initialize() throws EucalyptusCloudException;

	public abstract void check() throws EucalyptusCloudException;

    public abstract void start() throws EucalyptusCloudException;

    public abstract void stop() throws EucalyptusCloudException;

    public abstract ListAllMyBucketsResponseType listAllMyBuckets(
			ListAllMyBucketsType request) throws EucalyptusCloudException;

	/**
	 * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise
	 * returns 404 if not found or 403 if no accesss.
	 * @param request
	 * @return
	 * @throws EucalyptusCloudException
	 */
	public abstract HeadBucketResponseType headBucket(HeadBucketType request)
			throws EucalyptusCloudException;

	public abstract CreateBucketResponseType createBucket(
			CreateBucketType request) throws EucalyptusCloudException;

	public abstract DeleteBucketResponseType deleteBucket(
			DeleteBucketType request) throws EucalyptusCloudException;

	public abstract GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract PutObjectResponseType putObject(PutObjectType request)
			throws EucalyptusCloudException;

	public abstract PostObjectResponseType postObject(PostObjectType request)
			throws EucalyptusCloudException;

	public abstract PutObjectInlineResponseType putObjectInline(
			PutObjectInlineType request) throws EucalyptusCloudException;

	public abstract AddObjectResponseType addObject(AddObjectType request)
			throws EucalyptusCloudException;

	public abstract DeleteObjectResponseType deleteObject(
			DeleteObjectType request) throws EucalyptusCloudException;

	public abstract ListBucketResponseType listBucket(ListBucketType request)
			throws EucalyptusCloudException;

	public abstract GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(
			GetObjectAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
			SetBucketAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract SetRESTBucketAccessControlPolicyResponseType setRESTBucketAccessControlPolicy(
			SetRESTBucketAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
			SetObjectAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract SetRESTObjectAccessControlPolicyResponseType setRESTObjectAccessControlPolicy(
			SetRESTObjectAccessControlPolicyType request)
			throws EucalyptusCloudException;

	public abstract GetObjectResponseType getObject(GetObjectType request)
			throws EucalyptusCloudException;

	public abstract GetObjectExtendedResponseType getObjectExtended(
			GetObjectExtendedType request) throws EucalyptusCloudException;

	public abstract GetBucketLocationResponseType getBucketLocation(
			GetBucketLocationType request) throws EucalyptusCloudException;

	public abstract CopyObjectResponseType copyObject(CopyObjectType request)
			throws EucalyptusCloudException;

	public abstract SetBucketLoggingStatusResponseType setBucketLoggingStatus(
			SetBucketLoggingStatusType request) throws EucalyptusCloudException;

	public abstract GetBucketLoggingStatusResponseType getBucketLoggingStatus(
			GetBucketLoggingStatusType request) throws EucalyptusCloudException;

	public abstract GetBucketVersioningStatusResponseType getBucketVersioningStatus(
			GetBucketVersioningStatusType request)
			throws EucalyptusCloudException;

	public abstract SetBucketVersioningStatusResponseType setBucketVersioningStatus(
			SetBucketVersioningStatusType request)
			throws EucalyptusCloudException;

	/*
	 * Significantly re-done version of listVersions that is based on listBuckets and the old listVersions.
	 */
	public abstract ListVersionsResponseType listVersions(
			ListVersionsType request) throws EucalyptusCloudException;

	public abstract DeleteVersionResponseType deleteVersion(
			DeleteVersionType request) throws EucalyptusCloudException;

	public abstract void fastDeleteObject(DeleteObjectType request)
			throws EucalyptusCloudException;

	public abstract void fastDeleteBucket(DeleteBucketType request)
			throws EucalyptusCloudException;

    public abstract GetLifecycleResponseType getLifecycle(GetLifecycleType request) throws EucalyptusCloudException;

    public abstract PutLifecycleResponseType putLifecycle(PutLifecycleType request) throws EucalyptusCloudException;

	public abstract InitiateMultipartUploadResponseType initiateMultipartUpload(
			InitiateMultipartUploadType request) throws EucalyptusCloudException;

	public abstract CompleteMultipartUploadResponseType completeMultipartUpload(
			CompleteMultipartUploadType request) throws EucalyptusCloudException;

	public abstract AbortMultipartUploadResponseType abortMultipartUpload(
			AbortMultipartUploadType request) throws EucalyptusCloudException;

	public abstract UploadPartResponseType uploadPart(
			UploadPartType request) throws EucalyptusCloudException;

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
}