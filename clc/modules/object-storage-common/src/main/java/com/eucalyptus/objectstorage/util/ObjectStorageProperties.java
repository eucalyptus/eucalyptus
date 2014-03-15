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

package com.eucalyptus.objectstorage.util;

import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.system.BaseDirectory;
import org.apache.log4j.Logger;

public class ObjectStorageProperties {
    private static Logger LOG = Logger.getLogger(ObjectStorageProperties.class);

    public static final long G = 1024 * 1024 * 1024;
    public static final long M = 1024 * 1024;
    public static final long K = 1024;

    public static int IO_CHUNK_SIZE = 4096;
    public static boolean enableTorrents = false;
    public static final String NAMESPACE_VERSION = "2006-03-01";
    public static final String CONTENT_LEN = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String MULTIFORM_DATA_TYPE = "multipart/form-data";
    public static int MAX_KEYS = 1000;

    public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";

    public static final String AMZ_ACL = "x-amz-acl";
    public static final String AMZ_REQUEST_ID = "x-amz-request-id";
    public static final long OBJECT_CREATION_EXPIRATION_INTERVAL_SEC = 30;

    //TODO: zhill - these should be replaced by references to the actual Accounts lookup. May need to add these as valid groups/accounts
    public static enum S3_GROUP {
        ALL_USERS_GROUP {
            public String toString() {
                return "http://acs.amazonaws.com/groups/global/AllUsers";
            }
        },
        AUTHENTICATED_USERS_GROUP {
            public String toString() {
                return "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
            }
        },
        LOGGING_GROUP {
            public String toString() {
                return "http://acs.amazonaws.com/groups/s3/LogDelivery";
            }
        },

        //TODO: this is wrong. za-team is a user, not a group. Should use canonicalId: 6aa5a366c34c1cbe25dc49211496e913e0351eb0e8c37aa3477e40942ec6b97c
        AWS_EXEC_READ {
            public String toString() {
                return "http://acs.amazonaws.com/groups/s3/zateam";
            }
        } //Used for the system for vm images
    }

    public static final String IGNORE_PREFIX = "x-ignore-";
    public static final String COPY_SOURCE = "x-amz-copy-source";
    public static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";

    public static final String X_AMZ_VERSION_ID = "x-amz-version-id";
    public static final String NULL_VERSION_ID = "null";

    public static final String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";

    public static final String TRACKER_BINARY = "bttrack";
    public static final String TORRENT_CREATOR_BINARY = "btmakemetafile";
    public static final String TORRENT_CLIENT_BINARY = "btdownloadheadless";
    public static String TRACKER_DIR = BaseDirectory.VAR.toString() + "/bt";
    public static String TRACKER_URL = "http://localhost:6969/announce";
    public static String TRACKER_PORT = "6969";

    public static long MAX_INLINE_DATA_SIZE = 10 * M;

    public static long MPU_PART_MIN_SIZE = 5 * 1024 * 1024; //5MB

    //15 minutes
    public final static long EXPIRATION_LIMIT = 900000;

    public enum CannedACL {
        private_only {
            public String toString() {
                return "private";
            }
        },
        public_read {
            public String toString() {
                return "public-read";
            }
        },
        public_read_write {
            public String toString() {
                return "public-read-write";
            }
        },
        authenticated_read {
            public String toString() {
                return "authenticated-read";
            }
        },
        bucket_owner_read {
            public String toString() {
                return "bucket-owner-read";
            }
        },
        bucket_owner_full_control {
            public String toString() {
                return "bucket-owner-full-control";
            }
        },
        log_delivery_write {
            public String toString() {
                return "log-delivery-write";
            }
        },
        aws_exec_read {
            public String toString() {
                return "aws-exec-read";
            }
        }
    }

    public enum STORAGE_CLASS {
        STANDARD,
        REDUCED_REDUNDANCY;
    }

    public enum Permission {
        READ, WRITE, READ_ACP, WRITE_ACP, FULL_CONTROL
    }

    public enum VersioningStatus {
        Enabled, Disabled, Suspended
    }

    public enum Headers {
        Bucket, Key, RandomKey, VolumeId, S3UploadPolicy, S3UploadPolicySignature
    }

    public enum ExtendedGetHeaders {
        IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
    }

    public enum ExtendedHeaderDateTypes {
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

    public enum ExtendedHeaderRangeTypes {
        ByteRangeStart, ByteRangeEnd
    }

    public enum GetOptionalParameters {
        IsCompressed
    }

    public enum FormField {
        FormUploadPolicyData, AWSAccessKeyId, key, bucket, acl, policy, redirect, success_action_redirect, success_action_status, signature, file
    }

    public enum IgnoredFields {
        AWSAccessKeyId, signature, file, policy, submit
    }

    public enum PolicyHeaders {
        expiration, conditions
    }

    public enum CopyHeaders {
        CopySourceIfMatch, CopySourceIfNoneMatch, CopySourceIfUnmodifiedSince, CopySourceIfModifiedSince
    }

    public enum SubResource {
        //Per the S3 Dev guide, these must be included in the canonicalized resource:
        acl, lifecycle, location, logging, notification, partNumber, policy, requestPayment, torrent, uploadId, uploads, versionId, versioning, versions, website, cors, tagging
    }

    public enum HTTPVerb {
        GET, PUT, DELETE, POST, HEAD, OPTIONS;
    }

    public enum ServiceParameter {
    }

    public enum BucketParameter {
        acl, location, prefix, maxkeys, delimiter, marker, torrent, logging, versioning, versions, versionidmarker, keymarker, cors, lifecycle, policy, notification, tagging, requestPayment, website, uploads;
    }

    public enum ObjectParameter {
        acl, torrent, versionId, uploads, partNumber, uploadId;
    }

    public enum RequiredQueryParams {
        Date
    }

    public static String getTrackerUrl() {
        try {
            TRACKER_URL = "http://" + Topology.lookup(ObjectStorage.class).getUri().getHost() + ":" + TRACKER_PORT + "/announce";
        } catch (Exception e) {
            LOG.error(e);
        }
        return TRACKER_URL;
    }
}
