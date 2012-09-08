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
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Data warehouse report generation command, invoked from Python wrapper.
 */
public class ReportCommand extends CommandSupport {

  public ReportCommand(final String[] args) {
    super(argumentsBuilder()
        .withArg( "f", "file", "File for generated report", true )
        .forArgs(args));
  }

  @Override
  protected void runCommand( final Arguments arguments ) {
    final String reportFilename = arguments.getArgument( "file", null );

    //TODO process arguments, generate report and write it out.
    try {
      Files.write( "", new File(reportFilename), Charsets.UTF_8);
    } catch (IOException e) {
      throw Exceptions.toUndeclared( e );
    }
  }

  public static void main( final String[] args ) {
    new ReportCommand( args ).run();
  }
}
