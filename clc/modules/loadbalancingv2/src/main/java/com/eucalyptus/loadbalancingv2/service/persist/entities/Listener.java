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
import com.eucalyptus.loadbalancingv2.service.persist.Taggable;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerView;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_listener")
public class Listener extends AbstractOwnedPersistent
    implements Loadbalancingv2Metadata.ListenerMetadata, ListenerView, Taggable<ListenerTag> {

  private static final long serialVersionUID = 1L;

  public enum Protocol {
    HTTP,
    HTTPS(true),
    TCP,
    TLS(true),
    UDP,
    TCP_UDP,
    GENEVE,
    ;

    private final boolean requiresCertificate;

    Protocol() {
      this(false);
    }

    Protocol(final boolean requiresCertificate) {
      this.requiresCertificate = requiresCertificate;
    }

    public boolean requiresCertificate() {
      return requiresCertificate;
    }
  }

  @ManyToOne
  @JoinColumn(name = "metadata_loadbalancer_fk", updatable = false, nullable = false)
  private LoadBalancer loadbalancer = null;

  @Column(name = "loadbalancer_type", updatable = false, nullable = false)
  @Enumerated(EnumType.STRING)
  private LoadBalancer.Type loadBalancerType;

  @Column(name = "loadbalancer_name", updatable = false, nullable = false)
  private String loadBalancerName;

  @Column(name = "loadbalancer_id", updatable = false, nullable = false)
  private String loadBalancerId;

  @Column(name = "port")
  private Integer port;

  @Column(name = "protocol")
  @Enumerated(EnumType.STRING)
  private Protocol protocol;

  @Column(name = "default_server_certificate_arn", length = 1024)
  private String defaultServerCertificateArn;

  @Column(name = "ssl_policy")
  private String sslPolicy;

  @Column(name = "default_actions")
  @Type(type="text")
  private String defaultActions;

  @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "listener")
  @OrderBy("priority")
  private List<ListenerRule> listenerRules = Lists.newArrayList();

  @ManyToMany
  @NotFound( action = NotFoundAction.IGNORE )
  @JoinTable( name = "metadata_v2_listener_to_targetgroup",
      joinColumns =        @JoinColumn( name = "listener_id" ),
      inverseJoinColumns = @JoinColumn( name = "targetgroup_id" ) )
  private Set<TargetGroup> targetGroups = Sets.newHashSet();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true, mappedBy = "listener")
  private List<ListenerTag> tags = Lists.newArrayList();

  //TODO:STEVE: AlpnPolicy.member.N
  //TODO:STEVE: Certificates.member.N

  protected Listener(){
  }

  protected Listener(final OwnerFullName owner, final String displayName) {
    super(owner, displayName);
  }

  public static Listener create(
      final LoadBalancer loadBalancer,
      final Integer port,
      final Protocol protocol
  ) {
    final Listener listener = new Listener(loadBalancer.getOwner(), Loadbalancingv2ResourceName.generateId());
    listener.setNaturalId(listener.getDisplayName());
    listener.setLoadbalancer(loadBalancer);
    listener.setLoadBalancerType(loadBalancer.getType());
    listener.setLoadBalancerId(loadBalancer.getNaturalId());
    listener.setLoadBalancerName(loadBalancer.getDisplayName());
    listener.setPort(port);
    listener.setProtocol(protocol);
    return listener;
  }

  @Override public ListenerTag createTag(final String key, final String value) {
    return ListenerTag.create(this, key, value);
  }

  @Override public void updateTag(final ListenerTag tag, final String value) {
    tag.setValue(value);
  }

  public static Listener named(final OwnerFullName owner, final String displayName) {
    return new Listener(owner, displayName);
  }

  public LoadBalancer getLoadbalancer() {
    return loadbalancer;
  }

  public void setLoadbalancer(
      LoadBalancer loadbalancer) {
    this.loadbalancer = loadbalancer;
  }

  public LoadBalancer.Type getLoadBalancerType() {
    return loadBalancerType;
  }

  public void setLoadBalancerType(
      LoadBalancer.Type loadBalancerType) {
    this.loadBalancerType = loadBalancerType;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  public String getLoadBalancerId() {
    return loadBalancerId;
  }

  public void setLoadBalancerId(String loadBalancerId) {
    this.loadBalancerId = loadBalancerId;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Protocol getProtocol() {
    return protocol;
  }

  public void setProtocol(
      Protocol protocol) {
    this.protocol = protocol;
  }

  public String getDefaultServerCertificateArn() {
    return defaultServerCertificateArn;
  }

  public void setDefaultServerCertificateArn(String defaultServerCertificateArn) {
    this.defaultServerCertificateArn = defaultServerCertificateArn;
  }

  public String getSslPolicy() {
    return sslPolicy;
  }

  public void setSslPolicy(String sslPolicy) {
    this.sslPolicy = sslPolicy;
  }

  public String getDefaultActions() {
    return defaultActions;
  }

  public void setDefaultActions(String defaultActions) {
    this.defaultActions = defaultActions;
  }

  public List<ListenerRule> getListenerRules() {
    return listenerRules;
  }

  public void setListenerRules(List<ListenerRule> listenerRules) {
    this.listenerRules = listenerRules;
  }

  public Option<ListenerRule> findListenerRule(final String naturalId) {
    return Stream.ofAll(getListenerRules()).find(rule -> naturalId.equals(rule.getNaturalId()));
  }

  public Set<TargetGroup> getTargetGroups() {
    return targetGroups;
  }

  public void setTargetGroups(Set<TargetGroup> targetGroups) {
    this.targetGroups = targetGroups;
  }

  @Override public List<ListenerTag> getTags() {
    return tags;
  }

  @Override public void setTags(final List<ListenerTag> tags) {
    this.tags = tags;
  }
}
