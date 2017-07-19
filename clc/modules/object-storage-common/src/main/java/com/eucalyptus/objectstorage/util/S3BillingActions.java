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

  private static class S3BillingTypes {
    String operationName;
    UsageCountTypes usageCountType;
    UsageBytesTypes usageBytesType;

    public S3BillingTypes(S3Actions operation, UsageCountTypes usageCountType, UsageBytesTypes usageBytesType) {
      this.operationName = operation.getActionName();
      this.usageCountType = usageCountType;
      this.usageBytesType = usageBytesType;
    }
    public S3BillingTypes(String operationName, UsageCountTypes usageCountType, UsageBytesTypes usageBytesType) {
      this.operationName = operationName;
      this.usageCountType = usageCountType;
      this.usageBytesType = usageBytesType;
    }
    public String getOperationName() {
      return operationName;
    }
    public UsageCountTypes getUsageCountType() {
      return usageCountType;
    }
    public UsageBytesTypes getUsageBytesType() {
      return usageBytesType;
    }
  }
  
  private static EnumMap<S3Actions, S3BillingTypes> S3BillingActionsMap = 
      new EnumMap<S3Actions, S3BillingTypes>(S3Actions.class);

  public enum UsageCountTypes {
    Tier1("Requests-Tier1"),
    Tier2("Requests-Tier2");
    private final String usageCountType;
    private UsageCountTypes(String usageCountType) {
      this.usageCountType = usageCountType;
    }
    @Override
    public String toString() {
      return usageCountType;
    }
  }
  
  public enum UsageBytesTypes {
    // In = sent by client into S3, Out = returned by S3 to client
    In("DataTransfer-In-Bytes"),
    Out("DataTransfer-Out-Bytes");
    private final String usageBytesType;
    private UsageBytesTypes(String usageBytesType) {
      this.usageBytesType = usageBytesType;
    }
    @Override
    public String toString() {
      return usageBytesType;
    }
  }
  
  static {
    // Assign string operation names in AWS billing reports for those that are
    // different from the S3Actions enum names.
    S3BillingActionsMap.put(S3Actions.GetObject, new S3BillingTypes(S3Actions.GetObject, UsageCountTypes.Tier2, UsageBytesTypes.Out));
    S3BillingActionsMap.put(S3Actions.ListBuckets, new S3BillingTypes("ListAllMyBuckets", UsageCountTypes.Tier1, UsageBytesTypes.Out));
    S3BillingActionsMap.put(S3Actions.ListObjects, new S3BillingTypes("ListBucket", UsageCountTypes.Tier1, UsageBytesTypes.Out));
    S3BillingActionsMap.put(S3Actions.PutObject, new S3BillingTypes(S3Actions.PutObject, UsageCountTypes.Tier1, UsageBytesTypes.In));
    // When new operations are added, check the usage reports from AWS to see
    // what names they call them, for compatibility.
  }
  
  private S3BillingActions() {
  }

  public static String getBillingOperationName(S3Actions action) {
    return S3BillingActionsMap.get(action).getOperationName(); 
  }

  public static String getBillingUsageCountName(S3Actions action) {
    return S3BillingActionsMap.get(action).getUsageCountType().toString(); 
  }

  public static String getBillingUsageBytesName(S3Actions action) {
    return S3BillingActionsMap.get(action).getUsageBytesType().toString(); 
  }
}
