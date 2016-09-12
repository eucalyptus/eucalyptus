/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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

/**
 *
 */
public interface WalrusService {

  HeadBucketResponseType HeadBucket( HeadBucketType request ) throws EucalyptusCloudException;

  CreateBucketResponseType CreateBucket( CreateBucketType request ) throws EucalyptusCloudException;

  DeleteBucketResponseType DeleteBucket( DeleteBucketType request ) throws EucalyptusCloudException;

  ListAllMyBucketsResponseType ListAllMyBuckets( ListAllMyBucketsType request ) throws EucalyptusCloudException;

  PutObjectResponseType PutObject( PutObjectType request ) throws EucalyptusCloudException;

  PutObjectInlineResponseType PutObjectInline( PutObjectInlineType request ) throws EucalyptusCloudException;

  DeleteObjectResponseType DeleteObject( DeleteObjectType request ) throws EucalyptusCloudException;

  ListBucketResponseType ListBucket( ListBucketType request ) throws EucalyptusCloudException;

  GetObjectAccessControlPolicyResponseType GetObjectAccessControlPolicy( GetObjectAccessControlPolicyType request )
      throws EucalyptusCloudException;

  GetObjectResponseType GetObject( GetObjectType request ) throws EucalyptusCloudException;

  GetObjectExtendedResponseType GetObjectExtended( GetObjectExtendedType request ) throws EucalyptusCloudException;

  CopyObjectResponseType CopyObject( CopyObjectType request ) throws EucalyptusCloudException;

  InitiateMultipartUploadResponseType InitiateMultipartUpload( InitiateMultipartUploadType request )
      throws EucalyptusCloudException;

  UploadPartResponseType UploadPart( UploadPartType request ) throws EucalyptusCloudException;

  CompleteMultipartUploadResponseType CompleteMultipartUpload( CompleteMultipartUploadType request )
      throws EucalyptusCloudException;

  AbortMultipartUploadResponseType AbortMultipartUpload( AbortMultipartUploadType request )
      throws EucalyptusCloudException;
}
