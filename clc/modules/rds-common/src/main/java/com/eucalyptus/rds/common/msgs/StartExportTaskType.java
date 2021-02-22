/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class StartExportTaskType extends RdsMessage {

  private StringList exportOnly;

  @Nonnull
  private String exportTaskIdentifier;

  @Nonnull
  private String iamRoleArn;

  @Nonnull
  private String kmsKeyId;

  @Nonnull
  private String s3BucketName;

  private String s3Prefix;

  @Nonnull
  private String sourceArn;

  public StringList getExportOnly() {
    return exportOnly;
  }

  public void setExportOnly(final StringList exportOnly) {
    this.exportOnly = exportOnly;
  }

  public String getExportTaskIdentifier() {
    return exportTaskIdentifier;
  }

  public void setExportTaskIdentifier(final String exportTaskIdentifier) {
    this.exportTaskIdentifier = exportTaskIdentifier;
  }

  public String getIamRoleArn() {
    return iamRoleArn;
  }

  public void setIamRoleArn(final String iamRoleArn) {
    this.iamRoleArn = iamRoleArn;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getS3BucketName() {
    return s3BucketName;
  }

  public void setS3BucketName(final String s3BucketName) {
    this.s3BucketName = s3BucketName;
  }

  public String getS3Prefix() {
    return s3Prefix;
  }

  public void setS3Prefix(final String s3Prefix) {
    this.s3Prefix = s3Prefix;
  }

  public String getSourceArn() {
    return sourceArn;
  }

  public void setSourceArn(final String sourceArn) {
    this.sourceArn = sourceArn;
  }

}
