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
 ************************************************************************/
package com.eucalyptus.cassandra;

import java.io.File;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Assert;

/**
 *
 */
public enum CassandraDirectory {

  CDCRAW( "cdc_raw" ),
  DATA( "data" ),
  HINTS( "hints" ),
  COMMITLOG( "commitlog" ),
  SAVEDCACHES( "saved_caches" ),
  CONF( "conf" ),
  CONF_TRIGGERS( "conf", "triggers" ),
  ;

  private final String[] path;

  CassandraDirectory( final String dir ) {
    Assert.notNull( dir, "dir" );
    this.path = new String[]{ "cassandra", dir };
  }

  CassandraDirectory( final String dir1, final String dir2 ) {
    Assert.notNull( dir1, "dir1" );
    Assert.notNull( dir2, "dir2" );
    this.path = new String[]{ "cassandra", dir1, dir2 };
  }

  public File file( ) {
    return BaseDirectory.VAR.getChildFile( this.path );
  }

  public boolean mkdirs( ) {
    return file( ).mkdirs( );
  }

  public boolean isEmpty( ) {
    final String[] listing = file( ).list( );
    return listing != null && listing.length == 0;
  }
}
