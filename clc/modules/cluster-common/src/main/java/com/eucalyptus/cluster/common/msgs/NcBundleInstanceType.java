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

public class NcBundleInstanceType extends CloudNodeMessage {

  private String instanceId;
  private String bucketName;
  private String filePrefix;
  private String objectStorageURL;
  private String userPublicKey;
  private String cloudPublicKey;
  private String s3Policy;
  private String s3PolicySig;
  private String architecture;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getBucketName( ) {
    return bucketName;
  }

  public void setBucketName( String bucketName ) {
    this.bucketName = bucketName;
  }

  public String getFilePrefix( ) {
    return filePrefix;
  }

  public void setFilePrefix( String filePrefix ) {
    this.filePrefix = filePrefix;
  }

  public String getObjectStorageURL( ) {
    return objectStorageURL;
  }

  public void setObjectStorageURL( String objectStorageURL ) {
    this.objectStorageURL = objectStorageURL;
  }

  public String getUserPublicKey( ) {
    return userPublicKey;
  }

  public void setUserPublicKey( String userPublicKey ) {
    this.userPublicKey = userPublicKey;
  }

  public String getCloudPublicKey( ) {
    return cloudPublicKey;
  }

  public void setCloudPublicKey( String cloudPublicKey ) {
    this.cloudPublicKey = cloudPublicKey;
  }

  public String getS3Policy( ) {
    return s3Policy;
  }

  public void setS3Policy( String s3Policy ) {
    this.s3Policy = s3Policy;
  }

  public String getS3PolicySig( ) {
    return s3PolicySig;
  }

  public void setS3PolicySig( String s3PolicySig ) {
    this.s3PolicySig = s3PolicySig;
  }

  public String getArchitecture( ) {
    return architecture;
  }

  public void setArchitecture( String architecture ) {
    this.architecture = architecture;
  }
}
