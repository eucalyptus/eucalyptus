/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.walrus.util;

import com.eucalyptus.system.BaseDirectory;

public interface WalrusProperties {

  String NAME = "Walrus"; // leave as walrus for upgrade ease
  long G = 1024 * 1024 * 1024;
  long M = 1024 * 1024;

  String bucketRootDirectory = BaseDirectory.VAR.toString() + "/bukkits";
  int MAX_BUCKETS_PER_ACCOUNT = 100;
  long MAX_BUCKET_SIZE = 5 * G;
  int MAX_TOTAL_SNAPSHOT_SIZE = 50;
  boolean BUCKET_NAMES_REQUIRE_DNS_COMPLIANCE = false;
  int MAX_KEYS = 1000;

  long MAX_INLINE_DATA_SIZE = 10 * M;
  String NAMESPACE_VERSION = "2006-03-01";
  String CONTENT_LEN = "Content-Length";
  String CONTENT_TYPE = "Content-Type";
  String CONTENT_MD5 = "Content-MD5";

  String AMZ_META_HEADER_PREFIX = "x-amz-meta-";
  String AMZ_ACL = "x-amz-acl";
  String AMZ_REQUEST_ID = "x-amz-request-id";

  String IGNORE_PREFIX = "x-ignore-";
  String COPY_SOURCE = "x-amz-copy-source";
  String METADATA_DIRECTIVE = "x-amz-metadata-directive";

  String X_AMZ_VERSION_ID = "x-amz-version-id";
  String NULL_VERSION_ID = "null";

  String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";

  // 15 minutes
  long EXPIRATION_LIMIT = 900000;

  enum Permission {
    READ, WRITE, READ_ACP, WRITE_ACP, FULL_CONTROL
  }

  enum VersioningStatus {
    Enabled, Disabled, Suspended
  }

  enum Headers {
    Bucket, Key, RandomKey, VolumeId, S3UploadPolicy, S3UploadPolicySignature
  }

  enum ExtendedGetHeaders {
    IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
  }

  enum ExtendedHeaderDateTypes {
    IfModifiedSince, IfUnmodifiedSince, CopySourceIfModifiedSince, CopySourceIfUnmodifiedSince;

    public static boolean contains(String value) {
      for (ExtendedHeaderDateTypes type : values()) {
        if (type.toString().equals(value)) {
          return true;
        }
      }
      return false;
    }
  }

  enum ExtendedHeaderRangeTypes {
    ByteRangeStart, ByteRangeEnd
  }

  enum GetOptionalParameters {
    IsCompressed
  }

  enum FormField {
    FormUploadPolicyData, AWSAccessKeyId, key, bucket, acl, policy, redirect, success_action_redirect, success_action_status, signature, file
  }

  enum CopyHeaders {
    CopySourceIfMatch, CopySourceIfNoneMatch, CopySourceIfUnmodifiedSince, CopySourceIfModifiedSince
  }

  enum SubResource {
    // Per the S3 Dev guide, these must be included in the canonicalized resource:
    acl, lifecycle, location, logging, notification, partNumber, policy, requestPayment, torrent, uploadId, uploads, versionId, versioning, versions, website, cors, tagging
  }

  enum HTTPVerb {
    GET, PUT, DELETE, POST, HEAD
  }

  enum ServiceParameter {
  }

  enum BucketParameter {
    acl, location, prefix, maxkeys, delimiter, marker, torrent, logging, versioning, versions, versionidmarker, keymarker, cors, lifecycle, policy, notification, tagging, requestPayment, website, uploads
  }

  enum ObjectParameter {
    acl, torrent, versionId, uploads, partNumber, uploadId
  }
}
