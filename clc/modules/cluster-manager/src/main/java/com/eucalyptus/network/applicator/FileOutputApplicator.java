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
package com.eucalyptus.network.applicator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.log4j.Logger;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.base.Charsets;

/**
 *
 */
public class FileOutputApplicator implements Applicator {

  private static final Logger logger = Logger.getLogger( FileOutputApplicator.class );

  @Override
  public void apply( final ApplicatorContext context, final ApplicatorChain chain ) throws ApplicatorException {
    final String networkInfo = MarshallingApplicatorHelper.getMarshalledNetworkInfo( context );

    final File newView = BaseDirectory.RUN.getChildFile( "global_network_info.xml.temp" );
    if ( newView.exists( ) && !newView.delete( ) ) {
      logger.warn( "Error deleting stale network view " + newView.getAbsolutePath( ) );
    }

    try {
      com.google.common.io.Files.write( networkInfo, newView, Charsets.UTF_8 );
      Files.move(
          newView.toPath( ),
          BaseDirectory.RUN.getChildFile( "global_network_info.xml" ).toPath( ), StandardCopyOption.REPLACE_EXISTING
      );
    } catch ( IOException e ) {
      throw new ApplicatorException( "Error writing network information to file", e );
    }

    chain.applyNext( context );
  }
}
