/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import java.util.Date;

public class UploadPartCopyResponseType extends ObjectStorageResponseType {

  private String copySourceVersionId;
  private String etag;
  private Date lastModified;

  public String getCopySourceVersionId() {
    return copySourceVersionId;
  }

  public void setCopySourceVersionId(String copySourceVersionId) {
    this.copySourceVersionId = copySourceVersionId;
  }

  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }
}
