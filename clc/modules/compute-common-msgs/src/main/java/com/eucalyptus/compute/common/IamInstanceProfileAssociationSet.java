/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

/**
 *
 */
public class IamInstanceProfileAssociationSet extends EucalyptusData {
  private ArrayList<IamInstanceProfileAssociation> member = new ArrayList<IamInstanceProfileAssociation>();

  public ArrayList<IamInstanceProfileAssociation> getMember( ) {
    return member;
  }

  public void setMember( final ArrayList<IamInstanceProfileAssociation> member ) {
    this.member = member;
  }
}
