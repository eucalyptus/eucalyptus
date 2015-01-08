/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.backend.upgrade;

import java.util.concurrent.Callable;

import groovy.sql.Sql;

import org.apache.log4j.Logger;

import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.base.Joiner;

@Upgrades.PostUpgrade( value = CloudWatch.class, since = Upgrades.Version.v4_1_0 )
public class PersistenceContext410Upgrade implements Callable<Boolean>{
  private static Logger LOG = Logger.getLogger( PersistenceContext410Upgrade.class );
  @Override
  public Boolean call( ) throws Exception {
   LOG.debug("Deleting duplicate tables from eucalyptus_cloudwatch");
   Sql sql = null;
   try {
     sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloudwatch" );
     final String[] backend_tables = {
         "alarm_history",
         "metric_data",
         "custom_metric_data_0",
         "custom_metric_data_1",
         "custom_metric_data_2",
         "custom_metric_data_3",
         "custom_metric_data_4",
         "custom_metric_data_5",
         "custom_metric_data_6",
         "custom_metric_data_7",
         "custom_metric_data_8",
         "custom_metric_data_9",
         "custom_metric_data_a",
         "custom_metric_data_b",
         "custom_metric_data_c",
         "custom_metric_data_d",
         "custom_metric_data_e",
         "custom_metric_data_f",
         "system_metric_data_0",
         "system_metric_data_1",
         "system_metric_data_2",
         "system_metric_data_3",
         "system_metric_data_4",
         "system_metric_data_5",
         "system_metric_data_6",
         "system_metric_data_7",
         "system_metric_data_8",
         "system_metric_data_9",
         "system_metric_data_a",
         "system_metric_data_b",
         "system_metric_data_c",
         "system_metric_data_d",
         "system_metric_data_e",
         "system_metric_data_f"
     };
     final String sqlExec = String.format("DROP TABLE IF EXISTS %s CASCADE",  Joiner.on(",").join(backend_tables));
     sql.execute( sqlExec);
     return true;
   } catch ( Exception ex ) {
     LOG.error( ex, ex );
     return false;
   } finally {
     if ( sql != null ) {
       sql.close( );
     }
   }
  }
}
