/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

@Entity
@PersistenceContext(name = "eucalyptus_loadbalancing")
@Table(name = "metadata_v2_tag_listener_rule")
@DiscriminatorValue("listener-rule")
public class ListenerRuleTag extends Tag<ListenerRuleTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "listener_rule_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private ListenerRule listenerRule;

  protected ListenerRuleTag() {
  }

  protected ListenerRuleTag(final ListenerRule listenerRule, final String key, final String value) {
    super(listenerRule.getOwner(), listenerRule.getArn(), key, value);
    setListenerRule(listenerRule);
  }

  public static ListenerRuleTag create(final ListenerRule listenerRule, final String key, final String value) {
    return new ListenerRuleTag(listenerRule, key, value);
  }

  public ListenerRule getListenerRule() {
    return listenerRule;
  }

  public void setListenerRule(final ListenerRule listenerRule) {
    this.listenerRule = listenerRule;
  }
}
