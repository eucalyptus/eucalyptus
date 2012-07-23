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

package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import com.eucalyptus.util.Exceptions;

public class BootstrapException extends RuntimeException {
  private static Logger LOG = Logger.getLogger( BootstrapException.class );

  private BootstrapException( String message, Throwable cause ) {
    super( Bootstrap.getCurrentStage( ) + ": " + message, cause );
  }

  public BootstrapException( String message ) {
    super( Bootstrap.getCurrentStage( ) + ": " + message );
  }

  private BootstrapException( Throwable cause ) {
    super( Bootstrap.getCurrentStage( ) + ": " + cause );
  }
  
  public static BootstrapException throwError( String message ) {
    return error( message, null );
  }
  public static BootstrapException throwError( String message, Throwable t ) {
    return error( message, t );
  }
  public static BootstrapException error( String message, Throwable t ) {
    Bootstrap.Stage stage = Bootstrap.getCurrentStage( );
    BootstrapException ex = new BootstrapException( message );
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[3];
    if( t == null ) {
      Logger.getLogger( ste.getClassName( ) ).error( "Error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), ex );
    } else {
      Logger.getLogger( ste.getClassName( ) ).error( "Error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    }
    return ex;
  }

  public static BootstrapException throwFatal( String message ) {
    return fatal( message, null );
  }
  public static BootstrapException throwFatal( String message, Throwable t ) {
    return fatal( message, t );
  }
  private static BootstrapException fatal( String message, Throwable t ) {
    Bootstrap.Stage stage = Bootstrap.getCurrentStage( );
    BootstrapException ex = new BootstrapException( message, t );
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[3];
    if( t == null ) {
      Logger.getLogger( ste.getClassName( ) ).fatal( "Fatal error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), ex );
    } else {
      Logger.getLogger( ste.getClassName( ) ).fatal( "Fatal error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    }
    Exceptions.error( message, t );
    return ex;
  }

}
