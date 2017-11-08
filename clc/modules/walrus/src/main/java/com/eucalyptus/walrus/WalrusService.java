/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
