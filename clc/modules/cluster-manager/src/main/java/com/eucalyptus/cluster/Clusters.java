/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.cluster;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.Column;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.records.Logs;
import com.google.common.collect.Lists;

public class Clusters extends AbstractNamedRegistry<Cluster> {
  private static Clusters singleton = getInstance( );
  private static Logger   LOG       = Logger.getLogger( Clusters.class );
  
  public static Clusters getInstance( ) {
    synchronized ( Clusters.class ) {
      if ( singleton == null ) singleton = new Clusters( );
    }
    return singleton;
  }
  
  public List<String> getClusterAddresses( ) {
    final SortedSet<String> hostOrdered = new TreeSet<String>( );
    for ( final Cluster c : this.listValues( ) ) {
      hostOrdered.add( c.getConfiguration( ).getHostName( ) );
    }
    return Lists.newArrayList( hostOrdered );
  }
  
  public static Cluster lookup( final ServiceConfiguration clusterConfig ) {
    try {
      return Clusters.getInstance( ).lookup( clusterConfig.getName( ) );
    } catch ( final NoSuchElementException ex ) {
      return Clusters.getInstance( ).lookupDisabled( clusterConfig.getName( ) );
    }
  }
  
  public static Configuration getConfiguration( ) {
    Configuration ret = null;
    try {
      ret = Transactions.find( new Configuration( ) );
    } catch ( final Exception ex1 ) {
      try {
        ret = Transactions.save( new Configuration( ) );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        ret = new Configuration( );
      }
    }
    return ret;
  }
  
  @Entity
  @javax.persistence.Entity
  @PersistenceContext( name = "eucalyptus_cloud" )
  @Table( name = "cloud_cluster_configuration" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  @ConfigurableClass( root = "cloud.cluster", description = "Configuration options controlling interactions with Cluster Controllers." )
  static class Configuration extends AbstractPersistent {
    private static final long serialVersionUID = 1L;
    @ConfigurableField( description = "The number of concurrent requests which will be sent to a single Cluster Controller." )
    @Column( name = "config_cluster_workers", nullable = false )
    private Integer requestWorkers     = 16;
    @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is PENDING." )
    @Column( name = "config_cluster_interval_pending", nullable = false )
    private Long    pendingInterval    = 3l;
    @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is NOTREADY." )
    @Column( name = "config_cluster_interval_notready", nullable = false )
    private Long    notreadyInterval   = 10l;
    @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is DISABLED." )
    @Column( name = "config_cluster_interval_disabled", nullable = false )
    private Long    disabledInterval   = 15l;
    @ConfigurableField( description = "The time period between service state checks for a Cluster Controller which is ENABLED." )
    @Column( name = "config_cluster_interval_enabled", nullable = false )
    private Long    enabledInterval    = 15l;
    @ConfigurableField( description = "The number of times a request will be retried while bootstrapping a Cluster Controller." )
    @Column( name = "config_cluster_startup_sync_retries", nullable = false )
    private Integer startupSyncRetries = 10;
    
    public Configuration( ) {
    }
    
    public Integer getRequestWorkers( ) {
      return this.requestWorkers;
    }
    
    private void setRequestWorkers( final Integer requestWorkers ) {
      this.requestWorkers = requestWorkers;
    }
    
    public void setStartupSyncRetries( final Integer startupSyncRetries ) {
      this.startupSyncRetries = startupSyncRetries;
    }
    
    public Integer getStartupSyncRetries( ) {
      return this.startupSyncRetries;
    }
    
    private void setEnabledInterval( final Long enabledInterval ) {
      this.enabledInterval = enabledInterval;
    }
    
    public Long getEnabledInterval( ) {
      return this.enabledInterval;
    }
    
    private void setDisabledInterval( final Long disabledInterval ) {
      this.disabledInterval = disabledInterval;
    }
    
    public Long getDisabledInterval( ) {
      return this.disabledInterval;
    }
    
    private void setNotreadyInterval( final Long notreadyInterval ) {
      this.notreadyInterval = notreadyInterval;
    }
    
    public Long getNotreadyInterval( ) {
      return this.notreadyInterval;
    }
    
    private void setPendingInterval( final Long pendingInterval ) {
      this.pendingInterval = pendingInterval;
    }
    
    public Long getPendingInterval( ) {
      return this.pendingInterval;
    }
  }
}
