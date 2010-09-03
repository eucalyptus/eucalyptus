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
    Exceptions.fatal( message, t );
    System.exit( -1 );
    return ex;
  }

}
