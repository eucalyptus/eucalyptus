/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.upgrade;

import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.collect.ImmutableMap;
import groovy.sql.Sql;
import java.util.Map;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@Upgrades.PreUpgrade( value = LoadBalancing.class, since = Upgrades.Version.v4_3_0 )
public class Properties430Upgrade implements Callable<Boolean> {
  private static final Logger LOG = 
      Logger.getLogger( Properties430Upgrade.class );

  private final static Map<String,String> propNameToFieldName = ImmutableMap.<String,String>builder()
      .put("services.loadbalancing.worker.ntp_server", "com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.ntp_server")
      .put("services.loadbalancing.worker.init_script","com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.init_script")
      .put("services.loadbalancing.worker.expiration_days", "com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.expiration_days")
      .put("services.loadbalancing.worker.keyname","com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.keyname")
      .put("services.loadbalancing.vm_per_zone", "com.eucalyptus.loadbalancing.LoadBalancingServiceProperties.vm_per_zone")
      .put("services.loadbalancing.worker.instance_type", "com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.instance_type")
      .put("services.loadbalancing.worker.app_cookie_duration", "com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.app_cookie_duration")
      .put("services.loadbalancing.worker.image", "com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties.image")
      .build();
      
  @Override
  public Boolean call() throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_loadbalancing" );
      for(final String propName : propNameToFieldName.keySet()) {
        final String fieldName = propNameToFieldName.get(propName);
        final String query = String.format("update eucalyptus_config.config_static_property set config_static_field_name='%s' where config_static_prop_name='%s'", 
            fieldName, propName);
        sql.execute(query);
      }
      return true;
    } catch ( Exception ex ) {
      LOG.error( "Error updating loadbalancer properties field names", ex );
      return false;
    } finally {
      if ( sql != null ) {
        sql.close( );
      }
    }
  }
}
