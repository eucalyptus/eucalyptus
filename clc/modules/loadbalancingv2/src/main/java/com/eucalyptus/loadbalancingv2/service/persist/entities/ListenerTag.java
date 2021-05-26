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
@Table(name = "metadata_v2_tag_listener")
@DiscriminatorValue("listener")
public class ListenerTag extends Tag<ListenerTag> {

  private static final long serialVersionUID = 1L;

  @JoinColumn(name = "listener_idfref", updatable = false, nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Listener listener;

  protected ListenerTag() {
  }

  protected ListenerTag(final Listener listener, final String key, final String value) {
    super(listener.getOwner(), listener.getArn(), key, value);
    setListener(listener);
  }

  public static ListenerTag create(final Listener listener, final String key, final String value) {
    return new ListenerTag(listener, key, value);
  }

  public Listener getListener() {
    return listener;
  }

  public void setListener(final Listener listener) {
    this.listener = listener;
  }
}
