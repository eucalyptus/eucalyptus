/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
@Upgrades.PreUpgrade( value = LoadBalancing.class, since = Upgrades.Version.v4_4_0 )
public class Properties440Upgrade implements Callable<Boolean> {
  private static final Logger LOG = 
      Logger.getLogger( Properties440Upgrade.class );

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
