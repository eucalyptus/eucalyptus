/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/**
 *
 */
public class IamInstanceProfileAssociation extends EucalyptusData {

  private String associationId;
  private IamInstanceProfile iamInstanceProfile;
  private String instanceId;
  private String state;
  private Date timestamp;

  public String getAssociationId( ) {
    return associationId;
  }

  public void setAssociationId( final String associationId ) {
    this.associationId = associationId;
  }

  public IamInstanceProfile getIamInstanceProfile( ) {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile( final IamInstanceProfile iamInstanceProfile ) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getState( ) {
    return state;
  }

  public void setState( final String state ) {
    this.state = state;
  }

  public Date getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( final Date timestamp ) {
    this.timestamp = timestamp;
  }
}
