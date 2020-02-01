/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ResourceIdentifierProperties extends EucalyptusData {

  private ArrayList<ResourceIdentifierPropertiesEntry> entry = new ArrayList<>();

  public ArrayList<ResourceIdentifierPropertiesEntry> getEntry() {
    return entry;
  }

  public void setEntry(final ArrayList<ResourceIdentifierPropertiesEntry> entry) {
    this.entry = entry;
  }

}
