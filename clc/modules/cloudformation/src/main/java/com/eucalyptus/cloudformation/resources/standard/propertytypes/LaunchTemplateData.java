/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
public class LaunchTemplateData {

  @Property
  private ArrayList<EC2BlockDeviceMapping> blockDeviceMappings = Lists.newArrayList( );

  @Property
  private Boolean disableApiTermination;

  @Property
  private Boolean ebsOptimized;

  @Property
  private EC2Monitoring monitoring;

  @Property
  private String imageId;

  @Property
  private String instanceInitiatedShutdownBehavior;

  @Property
  private String instanceType;

  @Property
  private EC2IamInstanceProfile iamInstanceProfile;

  @Property
  private EC2Placement placement;

  @Property
  private String kernelId;

  @Property
  private String keyName;

  @Property
  private String ramdiskId;

  @Property
  private ArrayList<String> securityGroupIds = Lists.newArrayList( );

  @Property
  private ArrayList<String> securityGroups = Lists.newArrayList( );

  @Property
  private ArrayList<EC2NetworkInterfaceSpecification> networkInterfaces = Lists.newArrayList( );

  @Property
  private String userData;

  @Property
  private ArrayList<EC2TagSpecification> tagSpecifications = Lists.newArrayList();

  public ArrayList<EC2BlockDeviceMapping> getBlockDeviceMappings() {
    return blockDeviceMappings;
  }

  public void setBlockDeviceMappings(final ArrayList<EC2BlockDeviceMapping> blockDeviceMappings) {
    this.blockDeviceMappings = blockDeviceMappings;
  }

  public Boolean getDisableApiTermination() {
    return disableApiTermination;
  }

  public void setDisableApiTermination(final Boolean disableApiTermination) {
    this.disableApiTermination = disableApiTermination;
  }

  public Boolean getEbsOptimized() {
    return ebsOptimized;
  }

  public void setEbsOptimized(final Boolean ebsOptimized) {
    this.ebsOptimized = ebsOptimized;
  }

  public EC2Monitoring getMonitoring() {
    return monitoring;
  }

  public void setMonitoring(final EC2Monitoring monitoring) {
    this.monitoring = monitoring;
  }

  public String getImageId() {
    return imageId;
  }

  public void setImageId(final String imageId) {
    this.imageId = imageId;
  }

  public String getInstanceInitiatedShutdownBehavior() {
    return instanceInitiatedShutdownBehavior;
  }

  public void setInstanceInitiatedShutdownBehavior(final String instanceInitiatedShutdownBehavior) {
    this.instanceInitiatedShutdownBehavior = instanceInitiatedShutdownBehavior;
  }

  public String getInstanceType() {
    return instanceType;
  }

  public void setInstanceType(final String instanceType) {
    this.instanceType = instanceType;
  }

  public EC2IamInstanceProfile getIamInstanceProfile() {
    return iamInstanceProfile;
  }

  public void setIamInstanceProfile(final EC2IamInstanceProfile iamInstanceProfile) {
    this.iamInstanceProfile = iamInstanceProfile;
  }

  public EC2Placement getPlacement() {
    return placement;
  }

  public void setPlacement(final EC2Placement placement) {
    this.placement = placement;
  }

  public String getKernelId() {
    return kernelId;
  }

  public void setKernelId(final String kernelId) {
    this.kernelId = kernelId;
  }

  public String getKeyName() {
    return keyName;
  }

  public void setKeyName(final String keyName) {
    this.keyName = keyName;
  }

  public String getRamdiskId() {
    return ramdiskId;
  }

  public void setRamdiskId(final String ramdiskId) {
    this.ramdiskId = ramdiskId;
  }

  public ArrayList<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  public void setSecurityGroupIds(final ArrayList<String> securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
  }

  public ArrayList<String> getSecurityGroups() {
    return securityGroups;
  }

  public void setSecurityGroups(final ArrayList<String> securityGroups) {
    this.securityGroups = securityGroups;
  }

  public ArrayList<EC2NetworkInterfaceSpecification> getNetworkInterfaces() {
    return networkInterfaces;
  }

  public void setNetworkInterfaces(final ArrayList<EC2NetworkInterfaceSpecification> networkInterfaces) {
    this.networkInterfaces = networkInterfaces;
  }

  public String getUserData() {
    return userData;
  }

  public void setUserData(final String userData) {
    this.userData = userData;
  }

  public ArrayList<EC2TagSpecification> getTagSpecifications() {
    return tagSpecifications;
  }

  public void setTagSpecifications(final ArrayList<EC2TagSpecification> tagSpecifications) {
    this.tagSpecifications = tagSpecifications;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final LaunchTemplateData that = (LaunchTemplateData) o;
    return Objects.equals(getBlockDeviceMappings(), that.getBlockDeviceMappings()) &&
        Objects.equals(getDisableApiTermination(), that.getDisableApiTermination()) &&
        Objects.equals(getEbsOptimized(), that.getEbsOptimized()) &&
        Objects.equals(getMonitoring(), that.getMonitoring()) &&
        Objects.equals(getImageId(), that.getImageId()) &&
        Objects.equals(getInstanceInitiatedShutdownBehavior(), that.getInstanceInitiatedShutdownBehavior()) &&
        Objects.equals(getInstanceType(), that.getInstanceType()) &&
        Objects.equals(getIamInstanceProfile(), that.getIamInstanceProfile()) &&
        Objects.equals(getPlacement(), that.getPlacement()) &&
        Objects.equals(getKernelId(), that.getKernelId()) &&
        Objects.equals(getKeyName(), that.getKeyName()) &&
        Objects.equals(getRamdiskId(), that.getRamdiskId()) &&
        Objects.equals(getSecurityGroupIds(), that.getSecurityGroupIds()) &&
        Objects.equals(getSecurityGroups(), that.getSecurityGroups()) &&
        Objects.equals(getNetworkInterfaces(), that.getNetworkInterfaces()) &&
        Objects.equals(getUserData(), that.getUserData()) &&
        Objects.equals(getTagSpecifications(), that.getTagSpecifications());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getBlockDeviceMappings(),
        getDisableApiTermination(),
        getEbsOptimized(),
        getMonitoring(),
        getImageId(),
        getInstanceInitiatedShutdownBehavior(),
        getInstanceType(),
        getIamInstanceProfile(),
        getPlacement(),
        getKernelId(),
        getKeyName(),
        getRamdiskId(),
        getSecurityGroupIds(),
        getSecurityGroups(),
        getNetworkInterfaces(),
        getUserData(),
        getTagSpecifications());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("blockDeviceMappings", blockDeviceMappings)
        .add("disableApiTermination", disableApiTermination)
        .add("ebsOptimized", ebsOptimized)
        .add("monitoring", monitoring)
        .add("imageId", imageId)
        .add("instanceInitiatedShutdownBehavior", instanceInitiatedShutdownBehavior)
        .add("instanceType", instanceType)
        .add("iamInstanceProfile", iamInstanceProfile)
        .add("placement", placement)
        .add("kernelId", kernelId)
        .add("keyName", keyName)
        .add("ramdiskId", ramdiskId)
        .add("securityGroupIds", securityGroupIds)
        .add("securityGroups", securityGroups)
        .add("networkInterfaces", networkInterfaces)
        .add("userData", userData)
        .add("tagSpecifications", tagSpecifications)
        .toString();
  }
}
