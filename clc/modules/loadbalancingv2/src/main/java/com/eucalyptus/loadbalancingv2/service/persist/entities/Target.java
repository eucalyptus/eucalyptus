/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.loadbalancingv2.service.persist.views.TargetView;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;


@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_target")
public class Target extends AbstractPersistent implements TargetView {

  private static final long serialVersionUID = 1L;

  public enum State {
    initial("Elb.InitialHealthChecking"),
    healthy,
    unhealthy("Target.FailedHealthChecks", "Health check failing"),
    unused("Target.NotInUse", "Target group not in use"),
    draining,
    unavailable,
    ;

    private final String reason;
    private final String description;

    State() {
      this(null, null);
    }

    State(String reason) {
      this(reason, null);
    }

    State(String reason, String description) {
      this.reason = reason;
      this.description = description;
    }

    public void apply(final Target target) {
      target.setTargetHealthState(this);
      target.setTargetHealthReason(this.reason);
      target.setTargetHealthDescription(this.description);
    }
  }

  @ManyToOne
  @JoinColumn(name = "metadata_target_group_fk", updatable = false, nullable = false)
  private TargetGroup targetGroup = null;

  @Column(name = "target_group_name", updatable = false, nullable = false)
  private String targetGroupName;

  @Column(name = "target_group_id", updatable = false, nullable = false)
  private String targetGroupId;

  @Column(name = "target_id", updatable = false, nullable = false)
  private String targetId;

  @Column(name = "availability_zone")
  private String availabilityZone;

  @Column(name = "ip_address", nullable = false)
  private String ipAddress;

  @Column(name = "port")
  private Integer port;

  @Column(name = "health_check_port")
  private Integer healthCheckPort;

  @Column(name = "target_health_description")
  private String targetHealthDescription;

  @Column(name = "target_health_reason")
  private String targetHealthReason;

  @Column(name = "target_health_state")
  @Enumerated(EnumType.STRING)
  private State targetHealthState;

  protected Target(){
  }

  public static Target create(
      final TargetGroup targetGroup,
      final String id,
      final String ipAddress
  ) {
    final Target target = new Target();
    target.setTargetGroup(targetGroup);
    target.setTargetGroupName(targetGroup.getDisplayName());
    target.setTargetGroupId(targetGroup.getNaturalId());
    target.setTargetId(id);
    target.setIpAddress(ipAddress);
    State.unused.apply(target);
    return target;
  }

  public TargetGroup getTargetGroup() {
    return targetGroup;
  }

  public void setTargetGroup(
      TargetGroup targetGroup) {
    this.targetGroup = targetGroup;
  }

  public String getTargetGroupName() {
    return targetGroupName;
  }

  public void setTargetGroupName(String targetGroupName) {
    this.targetGroupName = targetGroupName;
  }

  public String getTargetGroupId() {
    return targetGroupId;
  }

  public void setTargetGroupId(String targetGroupId) {
    this.targetGroupId = targetGroupId;
  }

  public String getTargetId() {
    return targetId;
  }

  public void setTargetId(String targetId) {
    this.targetId = targetId;
  }

  @Override
  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  @Override
  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  @Override
  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  @Override
  public Integer getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(Integer healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  @Override
  public String getTargetHealthDescription() {
    return targetHealthDescription;
  }

  public void setTargetHealthDescription(String targetHealthDescription) {
    this.targetHealthDescription = targetHealthDescription;
  }

  @Override
  public String getTargetHealthReason() {
    return targetHealthReason;
  }

  public void setTargetHealthReason(String targetHealthReason) {
    this.targetHealthReason = targetHealthReason;
  }

  @Override
  public State getTargetHealthState() {
    return targetHealthState;
  }

  public void setTargetHealthState(State targetHealthState) {
    this.targetHealthState = targetHealthState;
  }
}
