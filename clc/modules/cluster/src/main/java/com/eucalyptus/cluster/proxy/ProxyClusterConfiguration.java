/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 ************************************************************************/package com.eucalyptus.cluster.proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableInit;
import com.eucalyptus.entities.AbstractPersistent;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_cluster_configuration" )
@ConfigurableClass( root = "cloud.cluster", description = "Configuration options controlling interactions with Cluster Controllers." )
public class ProxyClusterConfiguration extends AbstractPersistent {
  private static final long serialVersionUID = 1L;

  private static final String DEFAULT_REQUEST_WORKERS = "8";
  private static final String DEFAULT_PENDING_INTERVAL = "3";
  private static final String DEFAULT_NOTREADY_INTERVAL = "10";
  private static final String DEFAULT_DISABLED_INTERVAL = "15";
  private static final String DEFAULT_ENABLED_INTERVAL = "15";
  private static final String DEFAULT_STARTUP_SYNC_RETRIES = "10";

  @ConfigurableField(
      description = "The number of concurrent requests which will be sent to a single Cluster Controller.",
      initial = DEFAULT_REQUEST_WORKERS )
  @Column( name = "config_cluster_workers", nullable = false )
  private Integer requestWorkers;

  @ConfigurableField(
      description = "The time period between service state checks for a Cluster Controller which is PENDING.",
      initial = DEFAULT_PENDING_INTERVAL )
  @Column( name = "config_cluster_interval_pending", nullable = false )
  private Long pendingInterval;

  @ConfigurableField(
      description = "The time period between service state checks for a Cluster Controller which is NOTREADY.",
      initial = DEFAULT_NOTREADY_INTERVAL )
  @Column( name = "config_cluster_interval_notready", nullable = false )

  private Long notreadyInterval;
  @ConfigurableField(
      description = "The time period between service state checks for a Cluster Controller which is DISABLED.",
      initial = DEFAULT_DISABLED_INTERVAL )
  @Column( name = "config_cluster_interval_disabled", nullable = false )
  private Long disabledInterval;

  @ConfigurableField(
      description = "The time period between service state checks for a Cluster Controller which is ENABLED.",
      initial = DEFAULT_ENABLED_INTERVAL )
  @Column( name = "config_cluster_interval_enabled", nullable = false )
  private Long enabledInterval;

  @ConfigurableField(
      description = "The number of times a request will be retried while bootstrapping a Cluster Controller.",
      initial = DEFAULT_STARTUP_SYNC_RETRIES )
  @Column( name = "config_cluster_startup_sync_retries", nullable = false )
  private Integer startupSyncRetries;

  @ConfigurableInit
  public ProxyClusterConfiguration init( ) {
    setRequestWorkers( Integer.valueOf( DEFAULT_REQUEST_WORKERS ) );
    setPendingInterval( Long.valueOf( DEFAULT_PENDING_INTERVAL ) );
    setNotreadyInterval( Long.valueOf( DEFAULT_NOTREADY_INTERVAL ) );
    setDisabledInterval( Long.valueOf( DEFAULT_DISABLED_INTERVAL ) );
    setEnabledInterval( Long.valueOf( DEFAULT_ENABLED_INTERVAL ) );
    setStartupSyncRetries( Integer.valueOf( DEFAULT_STARTUP_SYNC_RETRIES ) );
    return this;
  }

  public Integer getRequestWorkers() {
    return this.requestWorkers;
  }

  private void setRequestWorkers( final Integer requestWorkers ) {
    this.requestWorkers = requestWorkers;
  }

  public void setStartupSyncRetries( final Integer startupSyncRetries ) {
    this.startupSyncRetries = startupSyncRetries;
  }

  public Integer getStartupSyncRetries() {
    return this.startupSyncRetries;
  }

  private void setEnabledInterval( final Long enabledInterval ) {
    this.enabledInterval = enabledInterval;
  }

  public Long getEnabledInterval() {
    return this.enabledInterval;
  }

  private void setDisabledInterval( final Long disabledInterval ) {
    this.disabledInterval = disabledInterval;
  }

  public Long getDisabledInterval() {
    return this.disabledInterval;
  }

  private void setNotreadyInterval( final Long notreadyInterval ) {
    this.notreadyInterval = notreadyInterval;
  }

  public Long getNotreadyInterval() {
    return this.notreadyInterval;
  }

  private void setPendingInterval( final Long pendingInterval ) {
    this.pendingInterval = pendingInterval;
  }

  public Long getPendingInterval() {
    return this.pendingInterval;
  }
}
