package com.eucalyptus.util;

import org.apache.log4j.Logger;

public class Exceptions {
  private static Logger LOG = Logger.getLogger( Exceptions.class );
  public static IllegalArgumentException illegalArgument( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t==null?3:2];
    IllegalArgumentException ex = new IllegalArgumentException( "Illegal argument given to " + ste.toString( ) + ": " + message, t );
    ex.fillInStackTrace( );
    return ex;
  }
  public static IllegalArgumentException illegalArgument( String message ) {
    return illegalArgument( message, null );
  }
  public static RuntimeException fatal( String message ) {
    return fatal( message, null );
  }
  public static RuntimeException fatal( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t==null?3:2];
    Logger.getLogger( ste.getClassName( ) ).error( "Fatal error occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    RuntimeException ex = ( t ==null? new RuntimeException( "Terminating Eucalyptus: " + message ) : new RuntimeException( "Terminating Eucalyptus: " + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.error( t!=null?t:ex, t!=null?t:ex );
    System.exit( -1 );
    return ex;
  }
  public static Error uncatchable( String message ) {
    return uncatchable( message, new Error( message ) );
  }
  public static Error uncatchable( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t==null?3:2];
    Logger.getLogger( ste.getClassName( ) ).error( "Fatal error occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    Error ex = ( t ==null? new Error ( "Uncatchable exception.  Do not ever do whatever it is you did: " + message ) : new Error( "Uncatchable exception.  Do not ever do whatever it is you did: " + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.error( t!=null?t:ex, t!=null?t:ex );
    return ex;
  }
  public static boolean eat( String message ) {
    return eat( message, new Error( message ) );
  }
  public static boolean eat( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t==null?3:2];
    Logger.getLogger( ste.getClassName( ) ).error( "Ignoring the error that occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ) );
    Error ex = ( t ==null? new Error ( "Eating the exception.  Hopefully nothing goes wrong from here on out: "  + message ) : new Error( "Eating the exception.  Hopefully nothing goes wrong from here on out: "  + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.error( t!=null?t:ex, t!=null?t:ex );
    return false;
  }
  public static void ifNullArgument( Object ... args ) throws IllegalArgumentException {
    for( Object o : args ) {
      if( o == null ) {
        IllegalArgumentException ex = illegalArgument( "The argument to " + Thread.currentThread( ).getStackTrace( )[2].getMethodName( ) + " cannot be null." );
        LOG.error( ex, ex );
        throw ex;
      }
    }
  }
}
