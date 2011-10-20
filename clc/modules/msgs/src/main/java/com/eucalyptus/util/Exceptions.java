package com.eucalyptus.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import com.eucalyptus.records.Logs;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

public class Exceptions {
  private static Logger                    LOG                      = Logger.getLogger( Exceptions.class );
  private static final List<String>        DEFAULT_FILTER_PREFIXES  = Lists.newArrayList( "com.eucalyptus", "edu.ucsb.eucalyptus" );
  private static final List<String>        DEFAULT_FILTER_MATCHES   = Lists.newArrayList( );
  private static final Integer             DEFAULT_FILTER_MAX_DEPTH = 10;
  private static final StackTraceElement[] steArrayType             = new StackTraceElement[1];
  
  enum ExceptionCauses implements Function<Throwable, List<Throwable>> {
    INSTANCE;
    
    @Override
    public List<Throwable> apply( Throwable input ) {
      if ( input == null || input.getClass( ).equals( Exception.class ) || input.getClass( ).equals( Exception.class )
           || input.getClass( ).equals( Exception.class ) ) {
        return Lists.newArrayList( );
      } else {
        List<Throwable> ret = Lists.newArrayList( input );
        ret.addAll( this.apply( input.getCause( ) ) );
        return ret;
      }
    }
    
  }
  
  public static List<Throwable> causes( Throwable ex ) {
    return ExceptionCauses.INSTANCE.apply( ex );
  }
  
  public static <T extends Throwable> String string( String message, T ex ) {
    return message + "\n" + string( ex );
  }
  
  public static <T extends Throwable> String createFaultDetails( T ex ) {
    return Logs.isExtrrreeeme( )
      ? string( ex )
      : ex.getMessage( );
  }
  
  public static <T extends Throwable> String string( T ex ) {
    Throwable t = ( ex == null
      ? new RuntimeException( )
      : ex );
    String allMessages = causeString( ex );
    ByteArrayOutputStream os = new ByteArrayOutputStream( );
    PrintWriter p = new PrintWriter( os );
    p.println( allMessages );
    t.printStackTrace( p );
    p.flush( );
    for ( Throwable cause = t.getCause( ); cause != null; cause = cause.getCause( ) ) {
      p.print( "Caused by: " );
      cause.printStackTrace( p );
    }
    p.close( );
    return os.toString( );
  }
  
  public static <T extends Throwable> String causeString( T ex ) {
    return Joiner.on( "\nCaused by: " ).join( Exceptions.causes( ex ) );
  }
  
  /**
   * Convert the argument {@link Throwable} into a suitable {@link RuntimeException} either by type
   * casting or wrapping in an {@link UndeclaredThrowableException}.
   * 
   * @param <T>
   * @param message
   * @param ex
   * @return
   */
  public static <T extends Throwable> RuntimeException toUndeclared( String message, T... exs ) {
    Throwable ex = null;
    if ( exs != null && exs.length > 0 ) {
      ex = exs[0];
    } else {
      ex = ( T ) new RuntimeException( message );
    }
    return new UndeclaredThrowableException( ex instanceof UndeclaredThrowableException
      ? ex.getCause( )
      : ex, message );
  }
  
  /**
   * {@inheritDoc #toUndeclared(String, Throwable)}
   * 
   * @param cause
   * @return
   */
  public static RuntimeException toUndeclared( Throwable cause ) {
    return toUndeclared( cause.getMessage( ), cause );
  }

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
  
  public static <T extends Throwable> T debug( T t ) {
    return trace( t.getMessage( ), t );
  }
  
  public static RuntimeException trace( String message ) {
    return trace( new RuntimeException( message ) );
  }
  
  public static <T extends Throwable> T trace( T t ) {
    return trace( t.getMessage( ), t );
  }
  
  public static <T extends Throwable> T trace( String message, T t ) {
    Throwable filtered = new RuntimeException( t.getMessage( ) );
    filtered.setStackTrace( Exceptions.filterStackTraceElements( t ).toArray( steArrayType ) );
    LOG.trace( message, filtered );
    return t;
  }
  
  public static <T extends Throwable> T error( String message ) {
    return ( T ) error( message, new RuntimeException() );
  }
  
  public static <T extends Throwable> T error( T t ) {
    return error( t.getMessage( ), t );
  }
  
  public static <T extends Throwable> T error( String message, T t ) {
    Throwable filtered = new RuntimeException( message );
    filtered.setStackTrace( Exceptions.filterStackTraceElements( t ).toArray( steArrayType ) );
    LOG.error( message, filtered );
    return t;
  }
  
  public static <T extends Throwable> boolean isCausedBy( Throwable ex, final Class<T> class1 ) {
    return causedBy( ex, class1 ) != null;
  }
  
  @SuppressWarnings( "unchecked" )
  public static <T extends Throwable> T causedBy( Throwable ex, final Class<T> class1 ) {
    try {
      return ( T ) Iterables.find( Exceptions.causes( ex ), new Predicate<Throwable>( ) {
        
        @Override
        public boolean apply( Throwable input ) {
          return class1.isAssignableFrom( input.getClass( ) );
        }
      } );
    } catch ( NoSuchElementException ex1 ) {
      return null;
    }
  }

  /**
   * @param message
   * @return
   */
  public static RuntimeException noSuchElement( String message ) {
    return new NoSuchElementException( message );
  }
  
}
