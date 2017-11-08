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
