package com.eucalyptus.util;

import java.util.List;
import org.apache.log4j.Logger;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class Exceptions {
  private static Logger                    LOG                      = Logger.getLogger( Exceptions.class );
  private static final List<String>        DEFAULT_FILTER_PREFIXES  = Lists.newArrayList( "com.eucalyptus", "edu.ucsb.eucalyptus" );
  private static final List<String>        DEFAULT_FILTER_MATCHES   = Lists.newArrayList( );
  private static final Integer             DEFAULT_FILTER_MAX_DEPTH = 5;
  private static final StackTraceElement[] steArrayType             = new StackTraceElement[1];
  
  public static <T extends Throwable> T filterStackTrace( T ex, int maxDepth, List<String> fqClassPrefixes, List<String> matchPatterns ) {
    ex.setStackTrace( Exceptions.filterStackTraceElements( ex, maxDepth, fqClassPrefixes, matchPatterns ).toArray( steArrayType ) );
    return ex;
  }
  
  public static <T extends Throwable> T filterStackTrace( T ex, int maxDepth ) {
    ex.setStackTrace( Exceptions.filterStackTraceElements( ex, maxDepth, DEFAULT_FILTER_PREFIXES, DEFAULT_FILTER_MATCHES ).toArray( steArrayType ) );
    return ex;
  }
  
  public static <T extends Throwable> T filterStackTrace( T ex ) {
    ex.setStackTrace( Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MAX_DEPTH, DEFAULT_FILTER_PREFIXES, DEFAULT_FILTER_MATCHES ).toArray( steArrayType ) );
    return ex;
  }
  
  public static List<StackTraceElement> filterStackTraceElements( Throwable ex ) {
    return Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MAX_DEPTH, DEFAULT_FILTER_PREFIXES, DEFAULT_FILTER_MATCHES );
  }
  
  public static List<StackTraceElement> filterStackTraceElements( Throwable ex, int maxDepth, List<String> fqClassPrefixes, List<String> matchPatterns ) {
    StringBuilder sb = new StringBuilder( );
    List<StackTraceElement> filteredStes = Lists.newArrayList( );
    for ( final StackTraceElement ste : ex.getStackTrace( ) ) {
      boolean printSte = Iterables.any( fqClassPrefixes, new Predicate<String>( ) {
        @Override
        public boolean apply( String arg0 ) {
          return ste.getClassName( ).startsWith( arg0 );
        }
      } );
      printSte |= Iterables.any( fqClassPrefixes, new Predicate<String>( ) {
        @Override
        public boolean apply( String arg0 ) {
          return ste.toString( ).matches( ".*" + arg0 + ".*" );
        }
      } );
      filteredStes.add( ste );
      if ( filteredStes.size( ) >= maxDepth ) {
        break;
      }
    }
    return filteredStes;
  }
  
  public static IllegalArgumentException illegalArgument( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t == null
      ? 3
      : 2];
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
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t == null
      ? 3
      : 2];
    Logger.getLogger( ste.getClassName( ) ).error( "Fatal error occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    RuntimeException ex = ( t == null
      ? new RuntimeException( "Terminating Eucalyptus: " + message )
      : new RuntimeException( "Terminating Eucalyptus: " + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.fatal( t != null
      ? t
      : ex, t != null
      ? t
      : ex );
    System.exit( -1 );
    return ex;
  }
  
  public static Error uncatchable( String message ) {
    return uncatchable( message, new Error( message ) );
  }
  
  public static Error uncatchable( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t == null
      ? 3
      : 2];
    Logger.getLogger( ste.getClassName( ) ).error( "Fatal error occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    Error ex = ( t == null
      ? new Error( "Uncatchable exception.  Do not ever do whatever it is you did: " + message )
      : new Error( "Uncatchable exception.  Do not ever do whatever it is you did: " + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.error( t != null
      ? t
      : ex, t != null
      ? t
      : ex );
    return ex;
  }
  
  public static boolean eat( String message ) {
    return eat( message, new Error( message ) );
  }
  
  public static boolean eat( String message, Throwable t ) {
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[t == null
      ? 3
      : 2];
    Logger.getLogger( ste.getClassName( ) ).error( "Ignoring the error that occured: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":"
                                                       + ste.getLineNumber( ), t );
    Error ex = ( t == null
      ? new Error( "Eating the exception.  Hopefully nothing goes wrong from here on out: " + message )
      : new Error( "Eating the exception.  Hopefully nothing goes wrong from here on out: " + t.getMessage( ), t ) );
    ex.fillInStackTrace( );
    LOG.error( t != null
      ? t
      : ex );
    return false;
  }
  
  public static void ifNullArgument( Object... args ) throws IllegalArgumentException {
    for ( Object o : args ) {
      if ( o == null ) {
        IllegalArgumentException ex = illegalArgument( "The argument to " + Thread.currentThread( ).getStackTrace( )[2].getMethodName( ) + " cannot be null." );
        LOG.error( ex, ex );
        throw ex;
      }
    }
  }
  
  public static <T extends Throwable> T trace( T ex ) {
    LOG.trace( ex, ex );
    return ex;
  }
  
  public static Error trace( String string, Throwable t ) {
    Error e;
    trace( e = new Error( string, t ) );
    return e;
  }
}
