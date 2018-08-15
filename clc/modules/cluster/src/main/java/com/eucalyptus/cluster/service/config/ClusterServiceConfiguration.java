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
 ************************************************************************/
package com.eucalyptus.cluster.service.config;

import java.io.Serializable;
import java.util.concurrent.Callable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import com.eucalyptus.cluster.service.ClusterServiceId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;
import org.apache.log4j.Logger;

/**
 *
 */
@Entity
@PersistenceContext( name="eucalyptus_config" )
@ComponentPart( ClusterServiceId.class )
public class ClusterServiceConfiguration extends ComponentConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERVICE_PATH= "/services/Cluster";

  public ClusterServiceConfiguration( ) { }

  public ClusterServiceConfiguration( String partition, String name, String hostName, Integer port ) {
    super( partition, name, hostName, port, SERVICE_PATH );
  }

  @Upgrades.PreUpgrade( value = Empyrean.class, since = Upgrades.Version.v4_4_0 )
  public static class ClusterConfiguration440PreUpgrade implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( ClusterConfiguration440PreUpgrade.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_config" );
        sql.execute( "alter table config_component_base " +
                "drop column if exists cluster_addrs_per_net, " +
                "drop column if exists cluster_max_network_tag, " +
                "drop column if exists cluster_min_addr, " +
                "drop column if exists cluster_min_network_tag, " +
                "drop column if exists cluster_min_vlan, " +
                "drop column if exists cluster_use_network_tags"
        );

        return true;
      } catch ( Exception ex ) {
        logger.error( "Error updating cloud schema", ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }

  @Upgrades.PreUpgrade( value = Empyrean.class, since = Upgrades.Version.v5_0_0 )
  public static class ClusterConfiguration500PreUpgrade implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( ClusterConfiguration500PreUpgrade.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_config" );
        sql.execute( "alter table config_component_base " +
                "drop column if exists cluster_alt_source_hostname, " +
                "drop column if exists cluster_network_mode, " +
                "drop column if exists cluster_vnet_subnet, " +
                "drop column if exists cluster_vnet_netmask, " +
                "drop column if exists cluster_vnet_type, "
        );
        return true;
      } catch ( Exception ex ) {
        logger.error( "Error updating cloud schema", ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
