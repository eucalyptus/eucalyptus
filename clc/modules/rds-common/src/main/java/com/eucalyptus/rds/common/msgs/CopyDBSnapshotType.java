/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CopyDBSnapshotType extends RdsMessage {

  private Boolean copyTags;

  private String kmsKeyId;

  private String optionGroupName;

  private String preSignedUrl;

  @Nonnull
  private String sourceDBSnapshotIdentifier;

  private TagList tags;

  @Nonnull
  private String targetDBSnapshotIdentifier;

  public Boolean getCopyTags() {
    return copyTags;
  }

  public void setCopyTags(final Boolean copyTags) {
    this.copyTags = copyTags;
  }

  public String getKmsKeyId() {
    return kmsKeyId;
  }

  public void setKmsKeyId(final String kmsKeyId) {
    this.kmsKeyId = kmsKeyId;
  }

  public String getOptionGroupName() {
    return optionGroupName;
  }

  public void setOptionGroupName(final String optionGroupName) {
    this.optionGroupName = optionGroupName;
  }

  public String getPreSignedUrl() {
    return preSignedUrl;
  }

  public void setPreSignedUrl(final String preSignedUrl) {
    this.preSignedUrl = preSignedUrl;
  }

  public String getSourceDBSnapshotIdentifier() {
    return sourceDBSnapshotIdentifier;
  }

  public void setSourceDBSnapshotIdentifier(final String sourceDBSnapshotIdentifier) {
    this.sourceDBSnapshotIdentifier = sourceDBSnapshotIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getTargetDBSnapshotIdentifier() {
    return targetDBSnapshotIdentifier;
  }

  public void setTargetDBSnapshotIdentifier(final String targetDBSnapshotIdentifier) {
    this.targetDBSnapshotIdentifier = targetDBSnapshotIdentifier;
  }

}
