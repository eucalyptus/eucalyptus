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

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.configuration.PropertyConfigurator;
import com.eucalyptus.entities.PersistenceContexts;
import com.eucalyptus.reporting.export.ExportUtils;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Support class for commands
 */
abstract class CommandSupport {
  private static final String PROP_LOGGING_THRESHOLD = "com.eucalyptus.reporting.dw.logThreshold";
  private static final String LOGGING_THRESHOLD_DEFAULT = Level.ERROR.toString();
  private static final String LOGGING_THRESHOLD_SILENT = Level.OFF.toString();
  private static final String LOGGING_THRESHOLD_DEBUG = Level.DEBUG.toString();

  private final Arguments arguments;

  public CommandSupport( final Arguments arguments ) {
    this.arguments = arguments;
  }

  protected final void run() {
    try {
      setupLogging();
      setupPersistenceContext();
      runCommand( arguments );
    } catch ( Exception e ) {
      Logger.getLogger( this.getClass() ).error( "Error processing command", e );
      System.exit(1);
    }
  }

  protected abstract void runCommand( Arguments arguments );

  private void setupLogging() {
    final String threshold = arguments.getArgument( "log-threshold", null );
    final String log4JThreshold;
    if ( threshold == null ) {
      log4JThreshold = LOGGING_THRESHOLD_DEFAULT;
    } else if ( "debug".equalsIgnoreCase( threshold ) ) {
      log4JThreshold = LOGGING_THRESHOLD_DEBUG;
    } else if ( "silent".equalsIgnoreCase( threshold ) ) {
      log4JThreshold = LOGGING_THRESHOLD_SILENT;
    } else {
      log4JThreshold = threshold;
    }
    System.setProperty( PROP_LOGGING_THRESHOLD, log4JThreshold );
  }

  private void setupPersistenceContext() {
    final Properties properties = new Properties();
    try {
      properties.load( CommandSupport.class.getClassLoader().getResourceAsStream("com/eucalyptus/reporting/dw/datawarehouse_persistence.properties" ) );
    } catch (IOException e) {
      throw Exceptions.toUndeclared(e);
    }

    properties.setProperty( "jdbc-0.proxool.driver-url",
        String.format( "jdbc:postgresql://%s:%s/%s%s",
            arguments.getArgument( "db-host", "localhost") ,
            arguments.getArgument( "db-port", "5432"),
            arguments.getArgument( "db-name", "eucalyptus_reporting"),
            arguments.hasArgument( "db-ssl" ) ? "?ssl=true&sslfactory=com.eucalyptus.postgresql.PostgreSQLSSLSocketFactory" : "") );
    properties.setProperty( "jdbc-0.user", arguments.getArgument( "db-user", "eucalyptus" ) );
    properties.setProperty( "jdbc-0.password", arguments.getArgument("db-pass", "") );

    try {
      PropertyConfigurator.configure( properties );
    } catch (ProxoolException e) {
      throw Exceptions.toUndeclared(e);
    }

    Ejb3Configuration config = new Ejb3Configuration();
    config.setProperties( properties );
    for ( Class<?> eventClass : ExportUtils.getPersistentClasses() ) {
      config.addAnnotatedClass( eventClass );
    }
    PersistenceContexts.registerPersistenceContext( "eucalyptus_reporting", config );
  }

  protected static ArgumentsBuilder argumentsBuilder() {
    return new ArgumentsBuilder();
  }

  static final class Arguments {
    private final CommandLine commandLine;

    private Arguments( final CommandLine commandLine ) {
      this.commandLine = commandLine;
    }

    String getArgument( final String name,
                        final String defaultValue ) {
      return Iterables.get( getArguments( name ), 0, defaultValue );
    }

    List<String> getArguments( final String name ) {
      return Lists.newArrayList(Objects.firstNonNull(commandLine.getOptionValues(name), new String[0]));
    }

    boolean hasArgument( final String name ) {
      return commandLine.hasOption( name );
    }
  }

  static final class ArgumentsBuilder {
    private final Options options = new Options();

    private ArgumentsBuilder() {
      // DB arguments
      withArg( "dbh",  "db-host", "Database hostname", false );
      withArg( "dbpo", "db-port", "Database port", false );
      withArg( "dbn",  "db-name", "Database name", false );
      withArg( "dbu",  "db-user", "Database username", false );
      withArg( "dbp",  "db-pass", "Database password", true );
      withFlag( "dbs", "db-ssl", "Database connection uses SSL" );

      // Logging arguments
      withArg( "lt", "log-threshold", "Logging threshold", false );
    }

    ArgumentsBuilder withFlag( final String argument,
                               final String longName,
                               final String description ) {
      final Option option = new Option( argument, description );
      option.setLongOpt(longName);
      options.addOption(option);
      return this;
    }

    ArgumentsBuilder withArg( final String argument,
                              final String longName,
                              final String description,
                              final boolean required ) {
      final Option option = new Option( argument, description );
      option.setLongOpt(longName);
      option.setRequired(required);
      option.setArgs(1);
      option.setArgName(argument);
      options.addOption( option );
      return this;
    }

    Arguments forArgs( final String[] args ) throws IllegalArgumentException {
      try {
        return new Arguments( new GnuParser().parse( options, args ) );
      } catch (ParseException e) {
        throw new IllegalArgumentException( e );
      }
    }
  }
}
