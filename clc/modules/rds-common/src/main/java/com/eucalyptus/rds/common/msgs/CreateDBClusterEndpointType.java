/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import javax.annotation.Nonnull;


public class CreateDBClusterEndpointType extends RdsMessage {

  @Nonnull
  private String dBClusterEndpointIdentifier;

  @Nonnull
  private String dBClusterIdentifier;

  @Nonnull
  private String endpointType;

  private StringList excludedMembers;

  private StringList staticMembers;

  private TagList tags;

  public String getDBClusterEndpointIdentifier() {
    return dBClusterEndpointIdentifier;
  }

  public void setDBClusterEndpointIdentifier(final String dBClusterEndpointIdentifier) {
    this.dBClusterEndpointIdentifier = dBClusterEndpointIdentifier;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getEndpointType() {
    return endpointType;
  }

  public void setEndpointType(final String endpointType) {
    this.endpointType = endpointType;
  }

  public StringList getExcludedMembers() {
    return excludedMembers;
  }

  public void setExcludedMembers(final StringList excludedMembers) {
    this.excludedMembers = excludedMembers;
  }

  public StringList getStaticMembers() {
    return staticMembers;
  }

  public void setStaticMembers(final StringList staticMembers) {
    this.staticMembers = staticMembers;
  }

  public TagList getTags() {
    return tags;
  }

  public void setTags(final TagList tags) {
    this.tags = tags;
  }

}
