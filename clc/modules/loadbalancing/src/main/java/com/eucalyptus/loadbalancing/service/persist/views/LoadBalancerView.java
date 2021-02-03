/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancing.service.persist.views;

import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import com.eucalyptus.loadbalancing.LoadBalancerDeploymentVersion;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;


@Immutable
public interface LoadBalancerView {

  String getDisplayName();

  String getOwnerUserId();

  String getOwnerUserName();

  String getOwnerAccountNumber();

  Date getCreationTimestamp();

  @Nullable
  LoadBalancer.Scheme getScheme();

  @Nullable
  String getVpcId( );

  @Nullable
  LoadBalancerHealthCheckConfigView getHealthCheckConfig();

  @Nullable
  Integer getConnectionIdleTimeout();

  @Nullable
  Boolean getCrossZoneLoadbalancingEnabled();

  @Nullable
  Boolean getAccessLogEnabled();

  @Nullable
  Integer getAccessLogEmitInterval();

  @Nullable
  String getAccessLogS3BucketName();

  @Nullable
  String getAccessLogS3BucketPrefix();

  @Nullable
  String getLoadbalancerDeploymentVersion();

  default boolean hasHealthCheckConfig() {
    return getHealthCheckConfig() != null;
  }

  default boolean useSystemAccount() {
    return getLoadbalancerDeploymentVersion() != null &&
        LoadBalancerDeploymentVersion.getVersion(getLoadbalancerDeploymentVersion()).isEqualOrLaterThan(LoadBalancerDeploymentVersion.v4_2_0);
  }
}
