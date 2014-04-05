package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.crypto.Crypto;

/**
 * Created by ethomas on 2/14/14.
 */
public abstract class ResourceAction {
  public abstract ResourceProperties getResourceProperties();
  public abstract void setResourceProperties(ResourceProperties resourceProperties);
  public abstract ResourceInfo getResourceInfo();
  public abstract void setResourceInfo(ResourceInfo resourceInfo);
  public abstract void create(int stepNum) throws Exception;
  public abstract void update(int stepNum) throws Exception;
  public abstract void rollbackUpdate() throws Exception;
  public abstract void delete() throws Exception;
  public abstract void rollbackCreate() throws Exception;
  protected StackEntity stackEntity;

  public StackEntity getStackEntity() {
    return stackEntity;
  }

  public void setStackEntity(StackEntity stackEntity) {
    this.stackEntity = stackEntity;
  }

  public int getNumCreateSteps() {
    return 1;
  }

  public int getNumUpdateSteps() {
    return 1;
  }

  public final String getDefaultPhysicalResourceId(int maxLength) {
    // in case we need to truncate this we need to truncate prefix and middle
    String prefix = (getStackEntity() != null && getStackEntity().getStackName() != null) ?
      getStackEntity().getStackName() : "UNKNOWN";
    String middle = (getResourceInfo() != null && getResourceInfo().getLogicalResourceId() != null) ?
      getResourceInfo().getLogicalResourceId() : "UNKNOWN";
    String suffix = Crypto.generateAlphanumericId(13, "");
    String finalString = prefix + "-" + middle + "-" + suffix;
    if (finalString.length() > maxLength) {
      int prefixMiddleAndDashesLength = maxLength - suffix.length();
      int prefixAndDashLength = prefixMiddleAndDashesLength / 2;
      int middleAndDashLength = prefixMiddleAndDashesLength - prefixAndDashLength;
      int prefixLength = prefixAndDashLength - 1;
      int middleLength = middleAndDashLength - 1;
      if (prefix.length() < prefixLength) {
        int difference = prefixLength - prefix.length();
        prefixLength -= difference;
        prefixAndDashLength -= difference;
        middleLength += difference;
        middleAndDashLength += difference;
      } else if (middle.length() < middleLength) {
        int difference = middleLength - middle.length();
        middleLength -= difference;
        middleAndDashLength -= difference;
        prefixLength += difference;
        prefixAndDashLength += difference;
      }
      if (prefix.charAt(prefixLength - 1) == '-' && middleLength < middle.length()) {
        prefixLength -= 1;
        prefixAndDashLength -= 1;
        middleLength += 1;
        middleAndDashLength += 1;
      } else if (middle.charAt(prefixLength - 1) == '-' && prefixLength < prefix.length()) {
        middleLength -= 1;
        middleAndDashLength -= 1;
        prefixLength += 1;
        prefixAndDashLength += 1;
      }
      return prefix.substring(0, prefixLength) + "-" + middle.substring(0, middleLength) + "-" + suffix;
    }
    return finalString;
  }

  public final String getDefaultPhysicalResourceId() {
    return getDefaultPhysicalResourceId(Integer.MAX_VALUE);
  }

}
