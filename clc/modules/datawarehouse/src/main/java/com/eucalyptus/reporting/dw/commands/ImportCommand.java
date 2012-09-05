/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.dw.commands;

import java.io.File;
import java.io.IOException;
import com.eucalyptus.reporting.export.Import;
import com.eucalyptus.reporting.export.ReportingExport;
import com.eucalyptus.util.Exceptions;

/**
 * Data warehouse import command, invoked from Python wrapper.
 */
public class ImportCommand extends CommandSupport {

  public ImportCommand(final String[] args) {
    super(argumentsBuilder()
        .withFlag( "r", "replace", "Replace existing reporting data" )
        .withArg( "e", "export", "File containing exported reporting data for import", true )
        .forArgs(args));
  }

  @Override
  protected void runCommand( final Arguments arguments ) {
    final boolean replace = arguments.hasArgument( "replace" );
    final String exportFilename = arguments.getArgument( "export", null );
    final File exportFile = new File( exportFilename );
    final ReportingExport reportingExport;

    try {
      reportingExport = new ReportingExport( exportFile );
    } catch ( IOException e ) {
      throw Exceptions.toUndeclared( e );
    }

    if ( replace ) {
      Import.deleteAll();
    }

    Import.importData( reportingExport );
  }

  public static void main( final String[] args ) {
    new ImportCommand( args ).run();
  }
}
