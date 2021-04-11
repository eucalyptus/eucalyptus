/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class StartExportTaskResult extends EucalyptusData {

  private StringList exportOnly;

  private String exportTaskIdentifier;

  private String failureCause;

  private String iamRoleArn;

  private String kmsKeyId;

  private Integer percentProgress;

  private String s3Bucket;

  private String s3Prefix;

  private java.util.Date snapshotTime;

  private String sourceArn;

  private String status;

  private java.util.Date taskEndTime;

  private java.util.Date taskStartTime;

  private Integer totalExtractedDataInGB;

  private String warningMessage;

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

  public String getFailureCause() {
    return failureCause;
  }

  public void setFailureCause(final String failureCause) {
    this.failureCause = failureCause;
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

  public Integer getPercentProgress() {
    return percentProgress;
  }

  public void setPercentProgress(final Integer percentProgress) {
    this.percentProgress = percentProgress;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public void setS3Bucket(final String s3Bucket) {
    this.s3Bucket = s3Bucket;
  }

  public String getS3Prefix() {
    return s3Prefix;
  }

  public void setS3Prefix(final String s3Prefix) {
    this.s3Prefix = s3Prefix;
  }

  public java.util.Date getSnapshotTime() {
    return snapshotTime;
  }

  public void setSnapshotTime(final java.util.Date snapshotTime) {
    this.snapshotTime = snapshotTime;
  }

  public String getSourceArn() {
    return sourceArn;
  }

  public void setSourceArn(final String sourceArn) {
    this.sourceArn = sourceArn;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public java.util.Date getTaskEndTime() {
    return taskEndTime;
  }

  public void setTaskEndTime(final java.util.Date taskEndTime) {
    this.taskEndTime = taskEndTime;
  }

  public java.util.Date getTaskStartTime() {
    return taskStartTime;
  }

  public void setTaskStartTime(final java.util.Date taskStartTime) {
    this.taskStartTime = taskStartTime;
  }

  public Integer getTotalExtractedDataInGB() {
    return totalExtractedDataInGB;
  }

  public void setTotalExtractedDataInGB(final Integer totalExtractedDataInGB) {
    this.totalExtractedDataInGB = totalExtractedDataInGB;
  }

  public String getWarningMessage() {
    return warningMessage;
  }

  public void setWarningMessage(final String warningMessage) {
    this.warningMessage = warningMessage;
  }

}
