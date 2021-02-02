/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing;


public enum LoadBalancerDeploymentVersion {
  v4_1_0,
  v4_2_0, // the version is checked from 4.2.0
  v4_3_0,
  v4_4_0;

  public static LoadBalancerDeploymentVersion Latest = v4_4_0;

  public String toVersionString() {
    return this.name().substring(1).replace("_", ".");
  }

  public static LoadBalancerDeploymentVersion getVersion(final String version) {
    if (version == null || version.length() <= 0)
      return LoadBalancerDeploymentVersion.v4_1_0;

    return LoadBalancerDeploymentVersion.valueOf("v" + version.replace(".", "_"));
  }

  public boolean isLaterThan(final LoadBalancerDeploymentVersion other) {
    if (other == null)
      return false;

    String[] thisVersionDigits = this.name().substring(1).split("_");
    String[] otherVersionDigits = other.name().substring(1).split("_");

    for (int i = 0; i < thisVersionDigits.length; i++) {
      int thisDigit = Integer.parseInt(thisVersionDigits[i]);
      int otherDigit = 0;
      if (i < otherVersionDigits.length)
        otherDigit = Integer.parseInt(otherVersionDigits[i]);

      if (thisDigit > otherDigit)
        return true;
      else if (thisDigit < otherDigit)
        return false;
    }
    return false;
  }

  public boolean isEqualOrLaterThan(final LoadBalancerDeploymentVersion other) {
    return this.equals(other) || this.isLaterThan(other);
  }
}
