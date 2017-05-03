/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/package com.eucalyptus.cluster.proxy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
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
  @ConfigurableField( description = "The number of concurrent requests which will be sent to a single Cluster Controller.", initial = "8" )
  @Column( name = "config_cluster_workers", nullable = false )
  private Integer requestWorkers = 8;
  @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is PENDING.", initial = "3" )
  @Column( name = "config_cluster_interval_pending", nullable = false )
  private Long pendingInterval = 3l;
  @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is NOTREADY.", initial = "10" )
  @Column( name = "config_cluster_interval_notready", nullable = false )
  private Long notreadyInterval = 10l;
  @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is DISABLED.", initial = "15" )
  @Column( name = "config_cluster_interval_disabled", nullable = false )
  private Long disabledInterval = 15l;
  @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is ENABLED.", initial = "15" )
  @Column( name = "config_cluster_interval_enabled", nullable = false )
  private Long enabledInterval = 15l;
  @ConfigurableField( description = "The number of times a request will be retried while bootstrapping a Cluster Controller.", initial = "10" )
  @Column( name = "config_cluster_startup_sync_retries", nullable = false )
  private Integer startupSyncRetries = 10;

  public ProxyClusterConfiguration() {
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
