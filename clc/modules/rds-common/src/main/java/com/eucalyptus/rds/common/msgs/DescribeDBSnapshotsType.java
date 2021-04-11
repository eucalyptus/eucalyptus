/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

public class DescribeDBSnapshotsType extends RdsMessage {

  private String dBInstanceIdentifier;

  private String dBSnapshotIdentifier;

  private String dbiResourceId;

  private FilterList filters;

  private Boolean includePublic;

  private Boolean includeShared;

  private String marker;

  private Integer maxRecords;

  private String snapshotType;

  public String getDBInstanceIdentifier() {
    return dBInstanceIdentifier;
  }

  public void setDBInstanceIdentifier(final String dBInstanceIdentifier) {
    this.dBInstanceIdentifier = dBInstanceIdentifier;
  }

  public String getDBSnapshotIdentifier() {
    return dBSnapshotIdentifier;
  }

  public void setDBSnapshotIdentifier(final String dBSnapshotIdentifier) {
    this.dBSnapshotIdentifier = dBSnapshotIdentifier;
  }

  public String getDbiResourceId() {
    return dbiResourceId;
  }

  public void setDbiResourceId(final String dbiResourceId) {
    this.dbiResourceId = dbiResourceId;
  }

  public FilterList getFilters() {
    return filters;
  }

  public void setFilters(final FilterList filters) {
    this.filters = filters;
  }

  public Boolean getIncludePublic() {
    return includePublic;
  }

  public void setIncludePublic(final Boolean includePublic) {
    this.includePublic = includePublic;
  }

  public Boolean getIncludeShared() {
    return includeShared;
  }

  public void setIncludeShared(final Boolean includeShared) {
    this.includeShared = includeShared;
  }

  public String getMarker() {
    return marker;
  }

  public void setMarker(final String marker) {
    this.marker = marker;
  }

  public Integer getMaxRecords() {
    return maxRecords;
  }

  public void setMaxRecords(final Integer maxRecords) {
    this.maxRecords = maxRecords;
  }

  public String getSnapshotType() {
    return snapshotType;
  }

  public void setSnapshotType(final String snapshotType) {
    this.snapshotType = snapshotType;
  }

}
