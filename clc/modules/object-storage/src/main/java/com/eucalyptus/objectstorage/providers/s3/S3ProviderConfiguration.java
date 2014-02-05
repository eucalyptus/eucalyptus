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

package com.eucalyptus.objectstorage.providers.s3;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;

@ConfigurableClass( root = "objectstorage.s3provider", description = "Configuration for S3-compatible backend")
public class S3ProviderConfiguration{

	@ConfigurableField( description = "External S3 endpoint.",
			displayName = "s3_endpoint" )
	public static String S3Endpoint  = "s3.amazonaws.com";

	@ConfigurableField( description = "Local Store S3 Access Key.",
			displayName = "s3_access_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN )
	public static String S3AccessKey;

	@ConfigurableField( description = "Local Store S3 Secret Key.",
			displayName = "s3_secret_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN )
	public static String S3SecretKey;
	
	@ConfigurableField( description = "Remote S3 Access Key.",
			displayName = "remote_s3_access_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN )
	public static String RemoteS3AccessKey;

	@ConfigurableField( description = "Remote S3 Secret Key.",
			displayName = "remote_s3_secret_key", 
			type = ConfigurableFieldType.KEYVALUEHIDDEN )
	public static String RemoteS3SecretKey;

	@ConfigurableField( description = "Use HTTPS for communication to service backend.",
			displayName = "use_https",
			initial="false" )
	public static boolean S3UseHttps;
	
	@ConfigurableField( description = "Use DNS virtual-hosted-style bucket names for communication to service backend.",
			displayName = "use_backend_dns",
			initial="false" )
	public static boolean S3UseBackendDns;

	public static boolean getS3UseBackendDns() {
		return S3UseBackendDns;
	}

	public static void setS3UserBackendDns(boolean useDns) {
		S3UseBackendDns = useDns;
	}	

	public static String getS3AccessKey() {
		return S3AccessKey;
	}

	public static void setS3AccessKey(String s3AccessKey) {
		S3AccessKey = s3AccessKey;
	}

	public static String getS3SecretKey() {
		return S3SecretKey;
	}

	public static void setS3SecretKey(String s3SecretKey) {
		S3SecretKey = s3SecretKey;
	}
	
	public static Boolean getS3UseHttps() {
		return S3UseHttps;
	}

	public static void setS3UseHttps(Boolean s3UseHttps) {
		S3UseHttps = s3UseHttps;
	}

	public static String getS3Endpoint() {
		return S3Endpoint;
	}

	public static void setS3Endpoint(String endPoint) {
		S3Endpoint = endPoint;
	}

	public static String getS3EndpointHost() {
		String[] s3EndpointParts = S3Endpoint.split(":");
		if (s3EndpointParts.length > 0) {
			return s3EndpointParts[0];
		} else {
			return null;
		}
	}

	public static int getS3EndpointPort() {
		String[] s3EndpointParts = S3Endpoint.split(":");
		if (s3EndpointParts.length > 1) {
			try {
				return Integer.parseInt(s3EndpointParts[1]);
			} catch (NumberFormatException e) {
				return 80;
			}
		} else {
			return 80; //default http port
		}
	}
}
