/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.util.ArrayList;


public class DeleteLaunchTemplateVersionsResponseSuccessSet extends EucalyptusData {

  private ArrayList<DeleteLaunchTemplateVersionsResponseSuccessItem> member = new ArrayList<DeleteLaunchTemplateVersionsResponseSuccessItem>();

  public ArrayList<DeleteLaunchTemplateVersionsResponseSuccessItem> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<DeleteLaunchTemplateVersionsResponseSuccessItem> member ) {
    this.member = member;
  }
}
