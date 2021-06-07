/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.loadbalancingv2.service.persist.Taggable;
import com.eucalyptus.loadbalancingv2.service.persist.views.TargetGroupView;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_targetgroup")
public class TargetGroup extends UserMetadata<TargetGroup.State>
    implements Loadbalancingv2Metadata.TargetgroupMetadata, TargetGroupView, Taggable<TargetGroupTag> {

  private static final long serialVersionUID = 1L;

  public enum State {
    available
  }

  public enum Protocol {
    HTTP(30, 5, 5, 2),
    HTTPS(30, 5, 5, 2),
    TCP(null, null, 3, null),
    TLS(null, null, 3, null),
    UDP(null, null, null, null),
    TCP_UDP(null, null, null, null),
    GENEVE(10, 5, 3, 3),
    ;

    private final Integer defaultHealthCheckIntervalSeconds;
    private final Integer defaultHealthCheckTimeoutSeconds;
    private final Integer defaulHealthyThresholdCount;
    private final Integer defaulUnhealthyThresholdCount;

    Protocol(
        final Integer defaultHealthCheckIntervalSeconds,
        final Integer defaultHealthCheckTimeoutSeconds,
        final Integer defaulHealthyThresholdCount,
        final Integer defaulUnhealthyThresholdCount
    ) {
      this.defaultHealthCheckIntervalSeconds = defaultHealthCheckIntervalSeconds;
      this.defaultHealthCheckTimeoutSeconds = defaultHealthCheckTimeoutSeconds;
      this.defaulHealthyThresholdCount = defaulHealthyThresholdCount;
      this.defaulUnhealthyThresholdCount = defaulUnhealthyThresholdCount;
    }

    public Integer getDefaultHealthCheckIntervalSeconds() {
      return defaultHealthCheckIntervalSeconds;
    }

    public Integer getDefaultHealthCheckTimeoutSeconds() {
      return defaultHealthCheckTimeoutSeconds;
    }

    public Integer getDefaulHealthyThresholdCount() {
      return defaulHealthyThresholdCount;
    }

    public Integer getDefaulUnhealthyThresholdCount() {
      return defaulUnhealthyThresholdCount;
    }
  }

  public enum ProtocolVersion {
    GRPC("/AWS.ALB/healthcheck"),
    HTTP1("/"),
    HTTP2("/"),
    ;

    private final String defaultHealthCheckPath;

    ProtocolVersion(
        final String defaultHealthCheckPath
    ) {
      this.defaultHealthCheckPath = defaultHealthCheckPath;
    }

    public String getDefaultHealthCheckPath() {
      return defaultHealthCheckPath;
    }
  }

  public enum TargetType {
    instance,
    ip,
    lambda,
  }

  @Column(name = "targetgroup_target_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private TargetType targetType;

  @Column(name = "targetgroup_vpc_id")
  private String vpcId;

  @Column(name = "targetgroup_protocol")
  @Enumerated(EnumType.STRING)
  private Protocol protocol;

  @Column(name = "targetgroup_protocol_version")
  @Enumerated(EnumType.STRING)
  private ProtocolVersion protocolVersion;

  @Column(name = "targetgroup_port")
  private Integer port;

  @Column(name = "targetgroup_matcher_grpc_code")
  private String matcherGrpcCode;

  @Column(name = "targetgroup_matcher_http_code")
  private String matcherHttpCode;

  @Column(name = "targetgroup_health_check_enabled")
  private Boolean healthCheckEnabled;

  @Column(name = "targetgroup_health_check_interval_secs")
  private Integer healthCheckIntervalSeconds;

  @Column(name = "targetgroup_health_check_path")
  private String healthCheckPath;

  @Column(name = "targetgroup_health_check_port")
  private Integer healthCheckPort;

  @Column(name = "targetgroup_health_check_protocol")
  @Enumerated(EnumType.STRING)
  private Protocol healthCheckProtocol;

  @Column(name = "targetgroup_health_check_timeout_secs")
  private Integer healthCheckTimeoutSeconds;

  @Column(name = "targetgroup_healthy_threshold_count")
  private Integer healthyThresholdCount;

  @Column(name = "targetgroup_unhealthy_threshold_count")
  private Integer unhealthyThresholdCount;

  @ElementCollection
  @CollectionTable(name = "metadata_v2_targetgroup_attribute")
  @MapKeyColumn(name = "metadata_key")
  @Column(name = "metadata_value", length = 1024)
  @JoinColumn(name = "targetgroup_id")
  private Map<String, String> attributes = Maps.newHashMap();

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "targetGroup")
  @OrderBy("targetId")
  private List<Target> targets;

  @ManyToMany(mappedBy = "targetGroups")
  private List<LoadBalancer> loadBalancers = Lists.newArrayList();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "targetGroup")
  private List<TargetGroupTag> tags = Lists.newArrayList();

  protected TargetGroup() {
  }

  protected TargetGroup(
      final OwnerFullName owner,
      final String displayName
  ) {
    super(owner, displayName);
  }

  public static TargetGroup create(
      final OwnerFullName owner,
      final String displayName,
      final TargetType targetType,
      final String vpcId,
      final Protocol protocol,
      final ProtocolVersion protocolVersion,
      final Integer port
  ) {
    TargetGroup group = new TargetGroup(owner, displayName);
    group.setNaturalId(Loadbalancingv2ResourceName.generateId());
    group.setVpcId(vpcId);
    group.setProtocol(protocol);
    group.setProtocolVersion(protocolVersion);
    group.setPort(port);
    group.setHealthCheckEnabled(true);
    group.setHealthCheckIntervalSeconds(protocol.getDefaultHealthCheckIntervalSeconds());
    group.setTargetType(targetType);
    return group;
  }

  @Override public TargetGroupTag createTag(final String key, final String value) {
    return TargetGroupTag.create(this, key, value);
  }

  @Override public void updateTag(final TargetGroupTag tag, final String value) {
    tag.setValue(value);
  }

  public static TargetGroup named(final OwnerFullName userFullName, final String name) {
    final TargetGroup example = new TargetGroup(null, name);
    if (userFullName != null) {
      example.setOwnerAccountNumber(userFullName.getAccountNumber());
    }
    return example;
  }

  @Override public TargetType getTargetType() {
    return targetType;
  }

  public void setTargetType(TargetType targetType) {
    this.targetType = targetType;
  }

  @Nullable @Override public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }

  @Nullable @Override public Protocol getProtocol() {
    return protocol;
  }

  public void setProtocol(Protocol protocol) {
    this.protocol = protocol;
  }

  @Nullable @Override public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(ProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  @Nullable @Override public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  @Nullable @Override public String getMatcherGrpcCode() {
    return matcherGrpcCode;
  }

  public void setMatcherGrpcCode(String matcherGrpcCode) {
    this.matcherGrpcCode = matcherGrpcCode;
  }

  @Nullable @Override public String getMatcherHttpCode() {
    return matcherHttpCode;
  }

  public void setMatcherHttpCode(String matcherHttpCode) {
    this.matcherHttpCode = matcherHttpCode;
  }

  @Nullable @Override public Boolean getHealthCheckEnabled() {
    return healthCheckEnabled;
  }

  public void setHealthCheckEnabled(Boolean healthCheckEnabled) {
    this.healthCheckEnabled = healthCheckEnabled;
  }

  @Nullable @Override public Integer getHealthCheckIntervalSeconds() {
    return healthCheckIntervalSeconds;
  }

  public void setHealthCheckIntervalSeconds(Integer healthCheckIntervalSeconds) {
    this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
  }

  @Nullable @Override public String getHealthCheckPath() {
    return healthCheckPath;
  }

  public void setHealthCheckPath(String healthCheckPath) {
    this.healthCheckPath = healthCheckPath;
  }

  @Nullable @Override public Integer getHealthCheckPort() {
    return healthCheckPort;
  }

  public void setHealthCheckPort(Integer healthCheckPort) {
    this.healthCheckPort = healthCheckPort;
  }

  @Nullable @Override public Protocol getHealthCheckProtocol() {
    return healthCheckProtocol;
  }

  public void setHealthCheckProtocol(Protocol healthCheckProtocol) {
    this.healthCheckProtocol = healthCheckProtocol;
  }

  @Nullable @Override public Integer getHealthCheckTimeoutSeconds() {
    return healthCheckTimeoutSeconds;
  }

  public void setHealthCheckTimeoutSeconds(Integer healthCheckTimeoutSeconds) {
    this.healthCheckTimeoutSeconds = healthCheckTimeoutSeconds;
  }

  @Nullable @Override public Integer getHealthyThresholdCount() {
    return healthyThresholdCount;
  }

  public void setHealthyThresholdCount(Integer healthyThresholdCount) {
    this.healthyThresholdCount = healthyThresholdCount;
  }

  @Nullable @Override public Integer getUnhealthyThresholdCount() {
    return unhealthyThresholdCount;
  }

  public void setUnhealthyThresholdCount(Integer unhealthyThresholdCount) {
    this.unhealthyThresholdCount = unhealthyThresholdCount;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public List<Target> getTargets() {
    return targets;
  }

  public void setTargets(List<Target> targets) {
    this.targets = targets;
  }

  public Option<Target> findTarget(final String id) {
    return Stream.ofAll(getTargets()).find(target -> target.getTargetId().equals(id));
  }

  public List<LoadBalancer> getLoadBalancers() {
    return loadBalancers;
  }

  public void setLoadBalancers(List<LoadBalancer> loadBalancers) {
    this.loadBalancers = loadBalancers;
  }

  @Override
  public List<TargetGroupTag> getTags() {
    return tags;
  }

  public void setTags(final List<TargetGroupTag> tags) {
    this.tags = tags;
  }

  @Override
  public String getPartition() {
    return ComponentIds.lookup(Eucalyptus.class).name();
  }

  @Override
  public FullName getFullName() {
    return FullName.create.vendor("euca")
        .region(ComponentIds.lookup(Eucalyptus.class).name())
        .namespace(this.getOwnerAccountNumber())
        .relativeId("targetgroup", this.getDisplayName());
  }
}
