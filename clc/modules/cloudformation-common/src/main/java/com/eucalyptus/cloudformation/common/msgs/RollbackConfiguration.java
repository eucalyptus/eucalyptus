/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class RollbackConfiguration extends EucalyptusData {

  @FieldRange(max = 180)
  private Integer monitoringTimeInMinutes;

  @FieldRange(max = 5)
  private RollbackTriggers rollbackTriggers;

  public Integer getMonitoringTimeInMinutes() {
    return monitoringTimeInMinutes;
  }

  public void setMonitoringTimeInMinutes(final Integer monitoringTimeInMinutes) {
    this.monitoringTimeInMinutes = monitoringTimeInMinutes;
  }

  public RollbackTriggers getRollbackTriggers() {
    return rollbackTriggers;
  }

  public void setRollbackTriggers(final RollbackTriggers rollbackTriggers) {
    this.rollbackTriggers = rollbackTriggers;
  }

}
