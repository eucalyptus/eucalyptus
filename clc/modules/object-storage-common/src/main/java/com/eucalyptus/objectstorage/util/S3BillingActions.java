/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.objectstorage.util;

import com.amazonaws.auth.policy.actions.S3Actions;

import java.util.EnumMap;

public class S3BillingActions {

  private static EnumMap<S3Actions, String> S3BillingActionsMap = 
      new EnumMap<S3Actions, String>(S3Actions.class);

  static {
    for (S3Actions action : S3BillingActionsMap.keySet()) {
      S3BillingActionsMap.put(action, action.getActionName());
    }
    // Change those names that are different in AWS billing reports from the S3Actions enums.
    S3BillingActionsMap.put(S3Actions.ListBuckets, "ListAllMyBuckets");
    S3BillingActionsMap.put(S3Actions.ListObjects, "ListBucket");
    // Some other names are different too, but so far we don't include those in 
    // usage reports, and we might never support usage reports of may of them. 
    // When new operations are added, check the usage reports from AWS to see what
    // names they call them, for compatibility.
  }
  
  private S3BillingActions() {
  }

  public static String getBillingActionName(S3Actions action) {
    return S3BillingActionsMap.get(action); 
  }
}
