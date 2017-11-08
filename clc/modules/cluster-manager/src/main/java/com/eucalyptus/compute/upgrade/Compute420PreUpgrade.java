/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.upgrade;

import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.upgrade.Upgrades;
import groovy.sql.Sql;

/**
 *
 */
@Upgrades.PreUpgrade( value = Compute.class, since = Upgrades.Version.v4_2_0 )
public class Compute420PreUpgrade implements Callable<Boolean> {
  private static final Logger logger = Logger.getLogger( Compute420PreUpgrade.class );

  @Override
  public Boolean call( ) throws Exception {
    Sql sql = null;
    try {
      sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
      sql.execute( "drop table if exists cloud_address_configuration" );
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