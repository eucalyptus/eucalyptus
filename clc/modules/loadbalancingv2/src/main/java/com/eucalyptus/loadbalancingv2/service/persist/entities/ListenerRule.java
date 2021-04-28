/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerRuleView;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_listener_rule")
public class ListenerRule extends AbstractOwnedPersistent implements Loadbalancingv2Metadata.ListenerRuleMetadata, ListenerRuleView {

  private static final long serialVersionUID = 1L;

  public static final int PRIORITY_DEFAULT = 1_000_000_000;

  @ManyToOne
  @JoinColumn(name = "metadata_loadbalancer_fk", updatable = false, nullable = false)
  private LoadBalancer loadbalancer = null;

  @ManyToOne
  @JoinColumn(name = "metadata_listener_fk", updatable = false, nullable = false)
  private Listener listener = null;

  @ManyToMany
  @JoinTable(name = "metadata_v2_listener_rule_target_groups",
      joinColumns =        @JoinColumn( name = "metadata_listener_rule_fk" ),
      inverseJoinColumns = @JoinColumn( name = "metadata_target_group_fk" ) )
  private Set<TargetGroup> targetGroups = Sets.newHashSet(); //TODO:STEVE: update these pre-persist

  @Column(name = "loadbalancer_type", updatable = false, nullable = false)
  @Enumerated(EnumType.STRING)
  private LoadBalancer.Type loadBalancerType;

  @Column(name = "loadbalancer_name", updatable = false, nullable = false)
  private String loadBalancerName;

  @Column(name = "loadbalancer_id", updatable = false, nullable = false)
  private String loadBalancerId;

  @Column(name = "listener_id", updatable = false, nullable = false)
  private String listenerId;

  @Column(name = "priority")
  private Integer priority;

  @Column(name = "actions")
  @Type(type="text")
  private String actions;

  @Column(name = "conditions")
  @Type(type="text")
  private String conditions;

  protected ListenerRule(){
  }

  protected ListenerRule(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static ListenerRule create(
      final Listener listener,
      final int priority
  ) {
    final ListenerRule rule = new ListenerRule(listener.getOwner(), Loadbalancingv2ResourceName.generateId());
    rule.setNaturalId(rule.getDisplayName());
    rule.setListener(listener);
    rule.setLoadbalancer(listener.getLoadbalancer());
    rule.setLoadBalancerType(listener.getLoadBalancerType());
    rule.setLoadBalancerId(listener.getLoadBalancerId());
    rule.setLoadBalancerName(listener.getLoadBalancerName());
    rule.setListenerId(listener.getNaturalId());
    rule.setPriority(priority);
    return rule;
  }

  public static Listener named(final OwnerFullName owner, final String displayName) {
    return new Listener(owner, displayName);
  }

  public LoadBalancer getLoadbalancer() {
    return loadbalancer;
  }

  public void setLoadbalancer(LoadBalancer loadbalancer) {
    this.loadbalancer = loadbalancer;
  }

  public Listener getListener() {
    return listener;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override public LoadBalancer.Type getLoadBalancerType() {
    return loadBalancerType;
  }

  public void setLoadBalancerType(LoadBalancer.Type loadBalancerType) {
    this.loadBalancerType = loadBalancerType;
  }

  @Override public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  @Override public String getLoadBalancerId() {
    return loadBalancerId;
  }

  public void setLoadBalancerId(String loadBalancerId) {
    this.loadBalancerId = loadBalancerId;
  }

  @Override public String getListenerId() {
    return listenerId;
  }

  public void setListenerId(String listenerId) {
    this.listenerId = listenerId;
  }

  public Set<TargetGroup> getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(
      Set<TargetGroup> targetGroups) {
    this.targetGroups = targetGroups;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public String getActions() {
    return actions;
  }

  public void setActions(String actions) {
    this.actions = actions;
  }

  public String getConditions() {
    return conditions;
  }

  public void setConditions(String conditions) {
    this.conditions = conditions;
  }
}
