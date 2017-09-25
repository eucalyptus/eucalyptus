/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.cluster.common.msgs;

public class ClusterBundleInstanceType extends CloudClusterMessage {

  private String architecture;
  private String awsAccessKeyId;
  private String bucket;
  private String instanceId;
  private String prefix;
  private String uploadPolicy;
  private String uploadPolicySignature;
  private String url;
  private String userKey;

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }

  public String getAwsAccessKeyId( ) {
    return awsAccessKeyId;
  }

  public void setAwsAccessKeyId( String awsAccessKeyId ) {
    this.awsAccessKeyId = awsAccessKeyId;
  }

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getUploadPolicy( ) {
    return uploadPolicy;
  }

  public void setUploadPolicy( String uploadPolicy ) {
    this.uploadPolicy = uploadPolicy;
  }

  public String getUploadPolicySignature( ) {
    return uploadPolicySignature;
  }

  public void setUploadPolicySignature( String uploadPolicySignature ) {
    this.uploadPolicySignature = uploadPolicySignature;
  }

  public String getUrl( ) {
    return url;
  }

  public void setUrl( String url ) {
    this.url = url;
  }

  public String getUserKey( ) {
    return userKey;
  }

  public void setUserKey( String userKey ) {
    this.userKey = userKey;
  }
}
