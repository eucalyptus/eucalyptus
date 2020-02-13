/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DetectStackResourceDriftResult extends EucalyptusData {

  @Nonnull
  private StackResourceDrift stackResourceDrift;

  public StackResourceDrift getStackResourceDrift() {
    return stackResourceDrift;
  }

  public void setStackResourceDrift(final StackResourceDrift stackResourceDrift) {
    this.stackResourceDrift = stackResourceDrift;
  }

}
