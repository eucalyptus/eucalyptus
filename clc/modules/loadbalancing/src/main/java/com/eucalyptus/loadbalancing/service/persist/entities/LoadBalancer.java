/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.loadbalancing.service.persist.entities;

import static com.eucalyptus.loadbalancing.common.LoadBalancingMetadata.LoadBalancerMetadata;
import static com.eucalyptus.util.Strings.isPrefixOf;
import static com.eucalyptus.util.Strings.trimPrefix;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.hibernate.annotations.Parent;

import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerHealthCheckConfigView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerView;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.CaseFormat;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park
 */
@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_loadbalancer")
public class LoadBalancer extends UserMetadata<LoadBalancer.STATE>
    implements LoadBalancerMetadata, LoadBalancerView {

  public enum Scheme {
    Internal,
    InternetFacing;

    @Override
    public String toString() {
      return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, name());
    }

    public static Optional<Scheme> fromString(final String scheme) {
      return Enums.getIfPresent(Scheme.class,
          CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, Strings.nullToEmpty(scheme)));
    }
  }

  private static final long serialVersionUID = 1L;

  public enum STATE {
    pending, available, failed
  }

  private LoadBalancer() {
    super(null, null);
  }

  private LoadBalancer(final OwnerFullName userFullName, final String lbName) {
    super(userFullName, lbName);
  }

  public static LoadBalancer newInstance(final OwnerFullName userFullName, final String lbName) {
    final LoadBalancer instance = new LoadBalancer(userFullName, lbName);
    if (userFullName != null) {
      instance.setOwnerAccountNumber(userFullName.getAccountNumber());
    }
    return instance;
  }

  public static LoadBalancer named() {
    return new LoadBalancer();
  }

  public static LoadBalancer named(final OwnerFullName userFullName, final String lbName) {
    final LoadBalancer instance = new LoadBalancer(null, lbName);
    if (userFullName != null) {
      instance.setOwnerAccountNumber(userFullName.getAccountNumber());
    }
    return instance;
  }

  public static LoadBalancer ownedByAccount(final String accountNumber) {
    final LoadBalancer instance = new LoadBalancer();
    instance.setOwnerAccountNumber(accountNumber);
    return instance;
  }

  public static LoadBalancer namedByAccountId(final String accountId, final String lbName) {
    final LoadBalancer instance = new LoadBalancer(null, lbName);
    instance.setOwnerAccountNumber(accountId);
    return instance;
  }

  @Column(name = "loadbalancer_vpc_id", nullable = true, updatable = false)
  private String vpcId; // only available for LoadBalancers attached to an Amazon VPC

  @Column(name = "loadbalancer_scheme", nullable = true)
  @Enumerated(EnumType.STRING)
  private Scheme scheme; // only available for LoadBalancers attached to an Amazon VPC

  @Column(name = "loadbalancer_connection_idle_timeout")
  private Integer connectionIdleTimeout;

  @Column(name = "loadbalancer_cross_zone_loadbalancing")
  private Boolean crossZoneLoadbalancing;

  @Column(name = "loadbalancer_accesslog_enabled", nullable = true)
  private Boolean accessLogEnabled;

  @Column(name = "loadbalancer_accesslog_emit_interval", nullable = true)
  private Integer accessLogEmitInterval;

  @Column(name = "loadbalancer_accesslog_s3bucket_name", nullable = true)
  private String accessLogS3BucketName;

  @Column(name = "loadbalancer_accesslog_s3bucket_prefix", nullable = true)
  private String accessLogS3BucketPrefix;

  @Column(name = "loadbalancer_deployment_version", nullable = true)
  private String loadbalancerDeploymentVersion;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
  private Collection<LoadBalancerBackendInstance> backendInstances = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "loadbalancer")
  private Collection<LoadBalancerListener> listeners = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
  private Collection<LoadBalancerZone> zones = null;

  @OneToOne(fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "loadbalancer")
  private LoadBalancerSecurityGroup group = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
  private Collection<LoadBalancerAutoScalingGroup> autoscale_groups = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
  private Collection<LoadBalancerPolicyDescription> policies = null;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "loadbalancer")
  private Collection<LoadBalancerBackendServerDescription> backendServers = null;

  @ElementCollection
  @CollectionTable(name = "metadata_loadbalancer_security_groups")
  @OrderColumn(name = "metadata_security_group_ordinal")
  private List<LoadBalancerSecurityGroupRef> securityGroupRefs = Lists.newArrayList();

  @ElementCollection
  @CollectionTable(name = "metadata_loadbalancer_tag")
  @MapKeyColumn(name = "metadata_key")
  @Column(name = "metadata_value")
  @JoinColumn(name = "metadata_loadbalancer_id")
  private Map<String, String> tags;

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(final String vpcId) {
    this.vpcId = vpcId;
  }

  public void setScheme(Scheme scheme) {
    this.scheme = scheme;
  }

  public Scheme getScheme() {
    return this.scheme;
  }

  public Integer getConnectionIdleTimeout() {
    return connectionIdleTimeout;
  }

  public void setConnectionIdleTimeout(final Integer connectionIdleTimeout) {
    this.connectionIdleTimeout = connectionIdleTimeout;
  }

  public Boolean getCrossZoneLoadbalancingEnabled() {
    return this.crossZoneLoadbalancing;
  }

  public void setCrossZoneLoadbalancingEnabled(final boolean enabled) {
    this.crossZoneLoadbalancing = enabled;
  }

  public Boolean getAccessLogEnabled() {
    return this.accessLogEnabled;
  }

  public void setAccessLogEnabled(final boolean enabled) {
    this.accessLogEnabled = enabled;
  }

  public Integer getAccessLogEmitInterval() {
    return this.accessLogEmitInterval;
  }

  public void setAccessLogEmitInterval(final Integer interval) {
    this.accessLogEmitInterval = interval;
  }

  public String getAccessLogS3BucketName() {
    return this.accessLogS3BucketName;
  }

  public void setAccessLogS3BucketName(final String bucketName) {
    this.accessLogS3BucketName = bucketName;
  }

  public String getAccessLogS3BucketPrefix() {
    return this.accessLogS3BucketPrefix;
  }

  public void setLoadbalancerDeploymentVersion(final String version) {
    this.loadbalancerDeploymentVersion = version;
  }

  public String getLoadbalancerDeploymentVersion() {
    return this.loadbalancerDeploymentVersion;
  }

  public void setAccessLogS3BucketPrefix(final String bucketPrefix) {
    this.accessLogS3BucketPrefix = bucketPrefix;
  }

  public List<LoadBalancerSecurityGroupRef> getSecurityGroupRefs() {
    return securityGroupRefs;
  }

  public void setSecurityGroupRefs(final List<LoadBalancerSecurityGroupRef> securityGroupRefs) {
    this.securityGroupRefs = securityGroupRefs;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(final Map<String, String> tags) {
    this.tags = tags;
  }

  public Collection<LoadBalancerBackendInstance> getBackendInstances() {
    return this.backendInstances;
  }

  public Collection<LoadBalancerListener> getListeners() {
    return this.listeners;
  }

  public Collection<LoadBalancerZone> getZones() {
    return zones;
  }

  public LoadBalancerSecurityGroup getSecurityGroup() {
    return this.group;
  }

  public Collection<LoadBalancerAutoScalingGroup> getAutoScalingGroups() {
    return autoscale_groups;
  }

  public Collection<LoadBalancerAutoScalingGroup> getAutoScaleGroups() {
    return this.autoscale_groups;
  }

  public Collection<LoadBalancerPolicyDescription> getPolicyDescriptions() {
    return this.policies;
  }

  public Collection<LoadBalancerBackendServerDescription> getBackendServers() {
    return this.backendServers;
  }

  public void setHealthCheck(int healthyThreshold, int interval, String target, int timeout,
      int unhealthyThreshold)
      throws IllegalArgumentException {
    final String[] targetParts = target.split(":", 2);
    final String canonicalizedTarget = targetParts.length == 2 ?
        targetParts[0].toUpperCase() + ":" + targetParts[1] :
        target;
    // check the validity of the health check param
    if (healthyThreshold < 0) {
      throw new IllegalArgumentException("healthyThreshold must be > 0");
    }
    if (interval < 0) {
      throw new IllegalArgumentException("interval must be > 0");
    }
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be > 0");
    }
    if (unhealthyThreshold < 0) {
      throw new IllegalArgumentException("unhealthyThreshold must be > 0");
    }

    if (!Iterables.any(Lists.newArrayList("HTTP", "HTTPS", "TCP", "SSL"),
        isPrefixOf(canonicalizedTarget))) {
      throw new IllegalArgumentException("target must starts with one of HTTP, HTTPS, TCP, SSL");
    }

    if (canonicalizedTarget.startsWith("HTTP") || canonicalizedTarget.startsWith("HTTPS")) {
      int idxPort = canonicalizedTarget.indexOf(":");
      int idxPath = canonicalizedTarget.indexOf("/");
      if (idxPort < 0 || idxPath < 0 || (idxPath - idxPort) <= 1) {
        throw new IllegalArgumentException("Port and Path must be specified for HTTP");
      }
      String port = canonicalizedTarget.substring(idxPort + 1, idxPath);
      try {
        int portNum = Integer.parseInt(port);
        if (!(portNum > 0 && portNum < 65536)) {
          throw new Exception("invalid port number");
        }
      } catch (Exception ex) {
        throw new IllegalArgumentException("Invalid target specified", ex);
      }
    } else if (canonicalizedTarget.startsWith("TCP") || canonicalizedTarget.startsWith("SSL")) {
      String portStr = trimPrefix(":", canonicalizedTarget.substring(3));
      try {
        int portNum = Integer.parseInt(portStr);
        if (!(portNum > 0 && portNum < 65536)) {
          throw new Exception("invalid port number");
        }
      } catch (Exception ex) {
        throw new IllegalArgumentException("Invalid target specified", ex);
      }
    }

    this.healthConfig =
        new LoadBalancerHealthCheckConfig(healthyThreshold, interval, canonicalizedTarget, timeout,
            unhealthyThreshold);
    this.healthConfig.setLoadBalancer(this);
  }

  public LoadBalancerHealthCheckConfig getHealthCheckConfig() {
    return this.healthConfig;
  }

  public int getHealthyThreshold() {
    if (this.healthConfig != null) {
      return this.healthConfig.HealthyThreshold;
    } else {
      throw new IllegalStateException("health check is not configured");
    }
  }

  public int getHealthCheckInterval() {
    if (this.healthConfig != null) {
      return this.healthConfig.Interval;
    } else {
      throw new IllegalStateException("health check is not configured");
    }
  }

  public String getHealthCheckTarget() {
    if (this.healthConfig != null) {
      return this.healthConfig.Target;
    } else {
      throw new IllegalStateException("health check is not configured");
    }
  }

  public int getHealthCheckTimeout() {
    if (this.healthConfig != null) {
      return this.healthConfig.Timeout;
    } else {
      throw new IllegalStateException("health check is not configured");
    }
  }

  public int getHealthCheckUnhealthyThreshold() {
    if (this.healthConfig != null) {
      return this.healthConfig.UnhealthyThreshold;
    } else {
      throw new IllegalStateException("health check is not configured");
    }
  }

  @Override
  public String toString() {
    return String.format("loadbalancer %s", this.displayName);
  }

  @Embedded
  private LoadBalancerHealthCheckConfig healthConfig = null;

  @Embeddable
  public static class LoadBalancerHealthCheckConfig implements LoadBalancerHealthCheckConfigView {
    @Parent
    private LoadBalancer loadBalancer = null;

    @Column(name = "loadbalancer_healthy_threshold", nullable = true)
    private Integer HealthyThreshold = null;
        //Specifies the number of consecutive health probe successes required before moving the instance to the Healthy state.

    @Column(name = "loadbalancer_healthcheck_interval", nullable = true)
    private Integer Interval = null;
        //Specifies the approximate interval, in seconds, between health checks of an individual instance.

    @Column(name = "loadbalancer_healthcheck_target", nullable = true)
    private String Target = null;
        //Specifies the instance being checked. The protocol is either TCP, HTTP, HTTPS, or SSL. The range of valid ports is one (1) through 65535.

    @Column(name = "loadbalancer_healthcheck_timeout", nullable = true)
    private Integer Timeout = null;
        //Specifies the amount of time, in seconds, during which no response means a failed health probe.

    @Column(name = "loadbalancer_unhealthy_threshold", nullable = true)
    private Integer UnhealthyThreshold = null;
        //Specifies the number of consecutive health probe failures required before moving the instance to the

    private LoadBalancerHealthCheckConfig() {
    }

    private LoadBalancerHealthCheckConfig(int healthyThreshold, int interval, String target,
        int timeout, int unhealthyThreshold) {
      this.HealthyThreshold = new Integer(healthyThreshold);
      this.Interval = new Integer(interval);
      this.Target = target;
      this.Timeout = new Integer(timeout);
      this.UnhealthyThreshold = new Integer(unhealthyThreshold);
    }

    void setLoadBalancer(LoadBalancer balancer) {
      this.loadBalancer = balancer;
    }

    LoadBalancer getLoadBalancer() {
      return this.loadBalancer;
    }

    public Integer getHealthyThreshold() {
      return HealthyThreshold;
    }

    public void setHealthyThreshold(final Integer healthyThreshold) {
      HealthyThreshold = healthyThreshold;
    }

    public Integer getInterval() {
      return Interval;
    }

    public void setInterval(final Integer interval) {
      Interval = interval;
    }

    public String getTarget() {
      return Target;
    }

    public void setTarget(final String target) {
      Target = target;
    }

    public Integer getTimeout() {
      return Timeout;
    }

    public void setTimeout(final Integer timeout) {
      Timeout = timeout;
    }

    public Integer getUnhealthyThreshold() {
      return UnhealthyThreshold;
    }

    public void setUnhealthyThreshold(final Integer unhealthyThreshold) {
      UnhealthyThreshold = unhealthyThreshold;
    }
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
        .relativeId("loadbalancer", this.getDisplayName());
  }
}
