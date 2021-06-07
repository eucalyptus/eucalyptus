/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CopyDBClusterSnapshotType extends RdsMessage {

  private Boolean copyTags;

  private String kmsKeyId;

  private String preSignedUrl;

  @Nonnull
  private String sourceDBClusterSnapshotIdentifier;

  private TagList tags;

  @Nonnull
  private String targetDBClusterSnapshotIdentifier;

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

  public String getPreSignedUrl() {
    return preSignedUrl;
  }

  public void setPreSignedUrl(final String preSignedUrl) {
    this.preSignedUrl = preSignedUrl;
  }

  public String getSourceDBClusterSnapshotIdentifier() {
    return sourceDBClusterSnapshotIdentifier;
  }

  public void setSourceDBClusterSnapshotIdentifier(final String sourceDBClusterSnapshotIdentifier) {
    this.sourceDBClusterSnapshotIdentifier = sourceDBClusterSnapshotIdentifier;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

  public String getTargetDBClusterSnapshotIdentifier() {
    return targetDBClusterSnapshotIdentifier;
  }

  public void setTargetDBClusterSnapshotIdentifier(final String targetDBClusterSnapshotIdentifier) {
    this.targetDBClusterSnapshotIdentifier = targetDBClusterSnapshotIdentifier;
  }

}
