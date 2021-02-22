/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeValidDBInstanceModificationsResult extends EucalyptusData {

  private ValidDBInstanceModificationsMessage validDBInstanceModificationsMessage;

  public ValidDBInstanceModificationsMessage getValidDBInstanceModificationsMessage() {
    return validDBInstanceModificationsMessage;
  }

  public void setValidDBInstanceModificationsMessage(final ValidDBInstanceModificationsMessage validDBInstanceModificationsMessage) {
    this.validDBInstanceModificationsMessage = validDBInstanceModificationsMessage;
  }

}
