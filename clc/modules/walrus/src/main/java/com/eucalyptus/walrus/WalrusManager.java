/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.walrus;

import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.AbortMultipartUploadType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.CompleteMultipartUploadType;
import com.eucalyptus.walrus.msgs.CopyObjectResponseType;
import com.eucalyptus.walrus.msgs.CopyObjectType;
import com.eucalyptus.walrus.msgs.CreateBucketResponseType;
import com.eucalyptus.walrus.msgs.CreateBucketType;
import com.eucalyptus.walrus.msgs.DeleteBucketResponseType;
import com.eucalyptus.walrus.msgs.DeleteBucketType;
import com.eucalyptus.walrus.msgs.DeleteObjectResponseType;
import com.eucalyptus.walrus.msgs.DeleteObjectType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.walrus.msgs.GetObjectExtendedType;
import com.eucalyptus.walrus.msgs.GetObjectResponseType;
import com.eucalyptus.walrus.msgs.GetObjectType;
import com.eucalyptus.walrus.msgs.HeadBucketResponseType;
import com.eucalyptus.walrus.msgs.HeadBucketType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.walrus.msgs.InitiateMultipartUploadType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.walrus.msgs.ListAllMyBucketsType;
import com.eucalyptus.walrus.msgs.ListBucketResponseType;
import com.eucalyptus.walrus.msgs.ListBucketType;
import com.eucalyptus.walrus.msgs.PutObjectInlineResponseType;
import com.eucalyptus.walrus.msgs.PutObjectInlineType;
import com.eucalyptus.walrus.msgs.PutObjectResponseType;
import com.eucalyptus.walrus.msgs.PutObjectType;
import com.eucalyptus.walrus.msgs.UploadPartResponseType;
import com.eucalyptus.walrus.msgs.UploadPartType;

public abstract class WalrusManager {

  public static void configure() {}

  public abstract void initialize() throws EucalyptusCloudException;

  public abstract void check() throws EucalyptusCloudException;

  public abstract void start() throws EucalyptusCloudException;

  public abstract void stop() throws EucalyptusCloudException;

  public abstract ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException;

  /**
   * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise returns 404 if not found or 403 if no
   * accesss.
   * 
   * @param request
   * @return
   * @throws EucalyptusCloudException
   */
  public abstract HeadBucketResponseType headBucket(HeadBucketType request) throws EucalyptusCloudException;

  public abstract CreateBucketResponseType createBucket(CreateBucketType request) throws EucalyptusCloudException;

  public abstract DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws EucalyptusCloudException;

  public abstract GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request)
      throws EucalyptusCloudException;

  public abstract PutObjectResponseType putObject(PutObjectType request) throws EucalyptusCloudException;

  public abstract PutObjectInlineResponseType putObjectInline(PutObjectInlineType request) throws EucalyptusCloudException;

  public abstract DeleteObjectResponseType deleteObject(DeleteObjectType request) throws EucalyptusCloudException;

  public abstract ListBucketResponseType listBucket(ListBucketType request) throws EucalyptusCloudException;

  public abstract GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request)
      throws EucalyptusCloudException;

  public abstract GetObjectResponseType getObject(GetObjectType request) throws EucalyptusCloudException;

  public abstract GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException;

  public abstract CopyObjectResponseType copyObject(CopyObjectType request) throws EucalyptusCloudException;

  public abstract InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws EucalyptusCloudException;

  public abstract CompleteMultipartUploadResponseType completeMultipartUpload(CompleteMultipartUploadType request) throws EucalyptusCloudException;

  public abstract AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) throws EucalyptusCloudException;

  public abstract UploadPartResponseType uploadPart(UploadPartType request) throws EucalyptusCloudException;
}
