/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package com.eucalyptus.util;

//import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;

import org.apache.log4j.Logger;

import com.eucalyptus.config.Configuration;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.system.BaseDirectory;

import java.net.*;
import java.util.List;
import java.util.Collections;

public class WalrusProperties {
	private static Logger LOG = Logger.getLogger( WalrusProperties.class );

	public static final String SERVICE_NAME = "Walrus";
	public static String NAME = "Walrus";
	public static final String DB_NAME             = "eucalyptus_walrus";
	public static final String VIRTUAL_SUBDOMAIN = "I_R_Bukkit";
	public static final long G = 1024*1024*1024;
	public static final long M = 1024*1024;
	public static final long K = 1024;
	public static String WALRUS_SUBDOMAIN = "walrus";

	public static final String bucketRootDirectory = BaseDirectory.VAR.toString() + "/bukkits";
	public static int MAX_BUCKETS_PER_USER = 5;
	public static long MAX_BUCKET_SIZE = 5 * G;
	public static long IMAGE_CACHE_SIZE = 30 * G;
	public static String WALRUS_URL;
	public static int MAX_TOTAL_SNAPSHOT_SIZE = 50;
	public static int MAX_KEYS = 1000;

	public static int IO_CHUNK_SIZE = 102400;
	public static boolean shouldEnforceUsageLimits = true;
	public static boolean trackUsageStatistics = true;
	public static boolean enableTorrents = false;
	public static boolean enableVirtualHosting = true;
	public static long CACHE_PROGRESS_TIMEOUT = 600000L; //ten minutes
	public static long IMAGE_CACHE_RETRY_TIMEOUT = 1000L;
	public static int IMAGE_CACHE_RETRY_LIMIT = 3;
	public static long MAX_INLINE_DATA_SIZE = 10 * M;
	public static final String walrusServicePath = "/services/Walrus";
	public static int WALRUS_PORT = Integer.parseInt( System.getProperty("euca.ws.port") );
	public static final String NAMESPACE_VERSION = "2006-03-01";
	public static final String CONTENT_LEN = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String MULTIFORM_DATA_TYPE = "multipart/form-data";

	public static final String URL_PROPERTY = "euca.walrus.url";
	public static final String WALRUS_HOST_PROPERTY = "euca.walrus.host";
	public static final String USAGE_LIMITS_PROPERTY = "euca.walrus.usageLimits";
	public static final String WALRUS_OPERATION = "WalrusOperation";
	public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";
	public static final String STREAMING_HTTP_GET = "STREAMING_HTTP_GET";
	public static final String STREAMING_HTTP_PUT = "STREAMING_HTTP_PUT";
	public static final String AMZ_ACL = "x-amz-acl";

	public static final String ALL_USERS_GROUP = "http://acs.amazonaws.com/groups/global/AllUsers";
	public static final String AUTHENTICATED_USERS_GROUP = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
	public static final String LOGGING_GROUP = "http://acs.amazonaws.com/groups/s3/LogDelivery";

	public static final String IGNORE_PREFIX = "x-ignore-";
	public static final String COPY_SOURCE = "x-amz-copy-source";
	public static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";
	public static final String ADMIN = "admin";

	public static final String X_AMZ_VERSION_ID = "x-amz-version-id";
	public static final String NULL_VERSION_ID = "null";

	public static final String TRACKER_BINARY = "bttrack";
	public static final String TORRENT_CREATOR_BINARY = "btmakemetafile";
	public static final String TORRENT_CLIENT_BINARY = "btdownloadheadless";
	public static String TRACKER_DIR = BaseDirectory.VAR.toString() + "/bt";
	public static String TRACKER_URL = "http://localhost:6969/announce";
	public static String TRACKER_PORT = "6969";

	static { GroovyUtil.loadConfig("walrusprops.groovy"); }

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
			for(ExtendedHeaderDateTypes type: values()) {
				if(type.toString().equals(value)) {
					return true;
				}
			}
			return false;
		}
	}

	public enum ExtendedHeaderRangeTypes {
		ByteRangeStart, ByteRangeEnd
	}

	public enum WalrusInternalOperations {
		GetDecryptedImage, ValidateImage
	}

	public enum GetOptionalParameters {
		IsCompressed
	}

	public enum StorageOperations {
		StoreSnapshot, DeleteWalrusSnapshot, GetWalrusSnapshot, GetWalrusSnapshotSize
	}

	public enum InfoOperations {
		GetSnapshotInfo
	}

	public enum StorageParameters {
		SnapshotVgName, SnapshotLvName
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
		acl, logging, torrent, location
	}

	public enum HTTPVerb {
		GET, PUT, DELETE, POST, HEAD;
	}

	public enum OperationParameter {
		acl, location, prefix, maxkeys, delimiter, marker, torrent, logging, versioning, versions, versionId;
	}

	public enum RequiredQueryParams {
		Date
	}

	public enum RequiredSOAPTags {
		AWSAccessKeyId, Timestamp, Signature
	}

	public static String getTrackerUrl() {
		try {
			String walrusUrl = SystemConfiguration.getWalrusUrl();
			TRACKER_URL = "http://" + new URI(walrusUrl).getHost() + ":" + TRACKER_PORT + "/announce";
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
		} catch (URISyntaxException e) {
			LOG.error(e);
		}
		return TRACKER_URL;
	}
}
