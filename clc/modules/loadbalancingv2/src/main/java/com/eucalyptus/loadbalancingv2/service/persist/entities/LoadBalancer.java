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
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.service.persist.Taggable;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerSubnetView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity(name = "LoadBalancerV2")
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_loadbalancer")
public class LoadBalancer extends UserMetadata<LoadBalancer.State>
    implements Loadbalancingv2Metadata.LoadbalancerMetadata, LoadBalancerView, Taggable<LoadBalancerTag> {

  private static final long serialVersionUID = 1L;

  public enum State {
    provisioning,
    active,
    active_impaired,
    failed,
    deleted,
  }

  public enum Scheme {
    internet_facing(LoadBalancerDomainName.EXTERNAL),
    internal(LoadBalancerDomainName.INTERNAL),
    ;

    private final LoadBalancerDomainName schemev1;

    Scheme(final LoadBalancerDomainName schemev1) {
      this.schemev1 = schemev1;
    }

    public LoadBalancerDomainName schemev1() {
      return schemev1;
    }

    public String toString() {
      return name().replace('_', '-');
    }
  }

  public enum Type {
    application("app"),
    network("net"),
    gateway("gwy"),
    ;

    private final String code;

    Type(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  public enum IpAddressType {
    dualstack,
    ipv4,
  }

  @Column( name = "update_required" )
  private Boolean updateRequired;

  @Column(name = "loadbalancer_type", nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private LoadBalancer.Type type;

  @Column(name = "loadbalancer_scheme", updatable = false)
  @Enumerated(EnumType.STRING)
  private LoadBalancer.Scheme scheme;

  @Column(name = "loadbalancer_ip_address_type")
  @Enumerated(EnumType.STRING)
  private LoadBalancer.IpAddressType ipAddressType;

  @Column(name = "loadbalancer_vpc_id")
  private String vpcId;

  @Column(name = "loadbalancer_hosted_zone_id")
  private String canonicalHostedZoneId;

  @ElementCollection
  @CollectionTable( name = "metadata_v2_loadbalancer_security_groups", joinColumns = @JoinColumn( name = "metadata_loadbalancer_id" ) )
  @Column( name = "metadata_security_group_id" )
  @OrderColumn( name = "metadata_security_group_index")
  private List<String> securityGroupIds = Lists.newArrayList();

  @ElementCollection
  @CollectionTable( name = "metadata_v2_loadbalancer_subnets", joinColumns = @JoinColumn( name = "metadata_loadbalancer_id" ) )
  @OrderColumn( name = "metadata_subnet_index")
  private List<LoadBalancerSubnet> subnets = Lists.newArrayList();

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "loadbalancer")
  @OrderBy( "port" )
  private List<Listener> listeners = Lists.newArrayList();

  @ManyToMany
  @NotFound( action = NotFoundAction.IGNORE )
  @JoinTable( name = "metadata_v2_loadbalancer_to_targetgroup",
      joinColumns =        @JoinColumn( name = "loadbalancer_id" ),
      inverseJoinColumns = @JoinColumn( name = "targetgroup_id" ) )
  private Set<TargetGroup> targetGroups = Sets.newHashSet();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "loadBalancer")
  private List<LoadBalancerTag> tags = Lists.newArrayList();

  protected LoadBalancer() {
  }

  protected LoadBalancer(
      final OwnerFullName owner,
      final String displayName
  ) {
    super(owner, displayName);
  }

  public static LoadBalancer create(
      final OwnerFullName owner,
      final String displayName,
      final LoadBalancer.Type type,
      final LoadBalancer.Scheme scheme
  ) {
    LoadBalancer loadBalancer = new LoadBalancer(owner, displayName);
    loadBalancer.setNaturalId(Loadbalancingv2ResourceName.generateId());
    loadBalancer.setState(State.provisioning);
    loadBalancer.setUpdateRequired(false);
    loadBalancer.setType(type);
    loadBalancer.setScheme(scheme);
    return loadBalancer;
  }

  @Override public LoadBalancerTag createTag(final String key, final String value) {
    return LoadBalancerTag.create(this, key, value);
  }

  @Override public void updateTag(final LoadBalancerTag tag, final String value) {
    tag.setValue(value);
  }

  public static LoadBalancer named(final OwnerFullName userFullName, final String lbName) {
    final LoadBalancer example = new LoadBalancer(null, lbName);
    if (userFullName != null) {
      example.setOwnerAccountNumber(userFullName.getAccountNumber());
    }
    return example;
  }

  public static LoadBalancer exampleWithId(final String id) {
    final LoadBalancer example = new LoadBalancer();
    example.setNaturalId(id);
    return example;
  }

  public static LoadBalancer exampleWithState(final LoadBalancer.State state) {
    return exampleWithState(state, null);
  }

  public static LoadBalancer exampleWithState(final LoadBalancer.State state, final Boolean updateRequired) {
    final LoadBalancer example = new LoadBalancer();
    if (state != null) {
      example.setState(state);
      example.setLastState(null);
      example.setStateChangeStack(null);
    }
    example.setUpdateRequired(updateRequired);
    return example;
  }

  public Boolean getUpdateRequired() {
    return updateRequired;
  }

  public void setUpdateRequired(Boolean updateRequired) {
    this.updateRequired = updateRequired;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Scheme getScheme() {
    return scheme;
  }

  public void setScheme(Scheme scheme) {
    this.scheme = scheme;
  }

  public IpAddressType getIpAddressType() {
    return ipAddressType;
  }

  public void setIpAddressType(
      IpAddressType ipAddressType) {
    this.ipAddressType = ipAddressType;
  }

  public String getVpcId() {
    return vpcId;
  }

  public void setVpcId(String vpcId) {
    this.vpcId = vpcId;
  }

  public String getCanonicalHostedZoneId() {
    return canonicalHostedZoneId;
  }

  public void setCanonicalHostedZoneId(String canonicalHostedZoneId) {
    this.canonicalHostedZoneId = canonicalHostedZoneId;
  }

  public List<String> getSecurityGroupIds() {
    return securityGroupIds;
  }

  public void setSecurityGroupIds(List<String> securityGroupIds) {
    this.securityGroupIds = securityGroupIds;
  }

  public List<LoadBalancerSubnetView> getSubnetViews() {
    return ImmutableList.copyOf(getSubnets());
  }

  public List<LoadBalancerSubnet> getSubnets() {
    return subnets;
  }

  public void setSubnets(List<LoadBalancerSubnet> subnets) {
    this.subnets = subnets;
  }

  public List<Listener> getListeners() {
    return listeners;
  }

  public void setListeners(List<Listener> listeners) {
    this.listeners = listeners;
  }

  public Option<Listener> findListener(final String naturalId) {
    return Stream.ofAll(getListeners()).find(listener -> naturalId.equals(listener.getNaturalId()));
  }

  public Set<TargetGroup> getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(Set<TargetGroup> targetGroups) {
    this.targetGroups = targetGroups;
  }

  public List<LoadBalancerTag> getTags() {
    return tags;
  }

  public void setTags(
      List<LoadBalancerTag> tags) {
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
