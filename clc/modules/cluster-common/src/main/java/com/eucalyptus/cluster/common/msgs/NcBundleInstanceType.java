/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
