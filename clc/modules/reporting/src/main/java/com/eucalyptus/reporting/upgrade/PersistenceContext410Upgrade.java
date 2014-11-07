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
package com.eucalyptus.reporting.upgrade;

import groovy.sql.Sql;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.base.Joiner;

@Upgrades.PostUpgrade( value = Reporting.class, since = Upgrades.Version.v4_1_0 )
public class PersistenceContext410Upgrade implements Callable<Boolean>{
  private static Logger LOG = Logger.getLogger( PersistenceContext410Upgrade.class );
  @Override
  public Boolean call( ) throws Exception {
   LOG.debug("Deleting duplicate tables from eucalyptus_reporting");
   Sql sql = null;
   try {
     sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_reporting" );
     final String[] backend_tables = {
         "reporting_elastic_ip_attach_events",
         "reporting_elastic_ip_create_events",
         "reporting_elastic_ip_delete_events",
         "reporting_elastic_ip_detach_events",
         "reporting_instance_create_events",
         "reporting_instance_usage_events",
         "reporting_s3_object_create_events",
         "reporting_s3_object_delete_events",
         "reporting_volume_attach_events",
         "reporting_volume_create_events",
         "reporting_volume_delete_events",
         "reporting_volume_detach_events",
         "reporting_volume_snapshot_create_events",
         "reporting_volume_snapshot_delete_events"
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