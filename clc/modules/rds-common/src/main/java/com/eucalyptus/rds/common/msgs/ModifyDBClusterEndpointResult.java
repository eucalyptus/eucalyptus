/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class ModifyDBClusterEndpointResult extends EucalyptusData {

  private String customEndpointType;

  private String dBClusterEndpointArn;

  private String dBClusterEndpointIdentifier;

  private String dBClusterEndpointResourceIdentifier;

  private String dBClusterIdentifier;

  private String endpoint;

  private String endpointType;

  private StringList excludedMembers;

  private StringList staticMembers;

  private String status;

  public String getCustomEndpointType() {
    return customEndpointType;
  }

  public void setCustomEndpointType(final String customEndpointType) {
    this.customEndpointType = customEndpointType;
  }

  public String getDBClusterEndpointArn() {
    return dBClusterEndpointArn;
  }

  public void setDBClusterEndpointArn(final String dBClusterEndpointArn) {
    this.dBClusterEndpointArn = dBClusterEndpointArn;
  }

  public String getDBClusterEndpointIdentifier() {
    return dBClusterEndpointIdentifier;
  }

  public void setDBClusterEndpointIdentifier(final String dBClusterEndpointIdentifier) {
    this.dBClusterEndpointIdentifier = dBClusterEndpointIdentifier;
  }

  public String getDBClusterEndpointResourceIdentifier() {
    return dBClusterEndpointResourceIdentifier;
  }

  public void setDBClusterEndpointResourceIdentifier(final String dBClusterEndpointResourceIdentifier) {
    this.dBClusterEndpointResourceIdentifier = dBClusterEndpointResourceIdentifier;
  }

  public String getDBClusterIdentifier() {
    return dBClusterIdentifier;
  }

  public void setDBClusterIdentifier(final String dBClusterIdentifier) {
    this.dBClusterIdentifier = dBClusterIdentifier;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

}
