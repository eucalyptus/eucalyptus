/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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