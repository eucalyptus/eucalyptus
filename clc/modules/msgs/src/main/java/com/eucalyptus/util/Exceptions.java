/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap.Discovery;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Exceptions {

  private static Logger                    LOG                      = Logger.getLogger( Exceptions.class );
  private static final List<String>        DEFAULT_FILTER_MATCHES   = Lists.newArrayList( "com.eucalyptus", "edu.ucsb.eucalyptus" );
  private static final StackTraceElement[] steArrayType             = new StackTraceElement[1];

  public static WebServicesException notFound( String message, Throwable... t ) {
    if ( Logs.isExtrrreeeme( ) && t != null
         && t.length > 0 ) {
      return new ServiceDispatchException( message + "\n"
                                           + string( message, t[0] ) );
    } else {
      return new ServiceDispatchException( message );
    }
  }

  enum ToString implements Function<Object, String> {
    INSTANCE;
    @Override
    public String apply( Object o ) {
      return ( o == null
                        ? "null"
                        : o.toString( ) );
    }
  };

  private static <T> Function<T, String> toStringFunction( ) {
    return ( Function<T, String> ) ToString.INSTANCE;
  }

  public static <T> Predicate<StackTraceElement> stackTraceElementFilter( final List<String> patterns ) {
    Function<StackTraceElement, String> toString = toStringFunction( );
    return Predicates.compose( makeSteFilter( patterns ), toString );
  }

  private static Predicate<String> makeSteFilter( final List<String> patterns ) {
    Predicate<String> filter = Predicates.alwaysTrue( );
    for ( String f : patterns ) {
      filter = Predicates.or( filter, Predicates.containsPattern( f ) );
    }
    return filter;
  }

  enum FilterCauses implements Predicate<Throwable> {
    INSTANCE;
    private static final Set<Class<? extends Exception>> filtered = Sets.newHashSet( UndeclaredThrowableException.class, RuntimeException.class,
                                                                                     ExecutionException.class );

    @Override
    public boolean apply( Throwable input ) {
      return !filtered.contains( input.getClass( ) );
    }
  }

  enum ExceptionCauses implements Function<Throwable, List<Throwable>> {
    INSTANCE;
    @Override
    public List<Throwable> apply( Throwable input ) {
      if ( input == null || input.getClass( ).equals( Exception.class ) ) {
        return Lists.newArrayList( );
      } else {
        List<Throwable> ret = Lists.newArrayList( input );
        ret.addAll( this.apply( input.getCause( ) ) );
        return ret;
      }
    }
  }

  public static <T extends Throwable> T maybeInterrupted( T t ) {
    if ( t instanceof InterruptedException ) {
      Thread.currentThread( ).interrupt( );
    }
    return t;
  }

  public static List<Throwable> causes( Throwable ex ) {
    return ExceptionCauses.INSTANCE.apply( ex );
  }

  /**
   * Convert this exception and all underlying causes, along with stack traces, into a string.
   *
   * @param <T>
   * @param message
   * @param ex
   * @return
   */
  public static <T extends Throwable> String string( String message, T ex ) {
    return message + "\n"
           + string( ex );
  }

  /**
   * {@inheritDoc #string(String, Throwable)}
   */
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
   * Get the message closest to the cause.
   *
   * @param throwable The exception
   * @return The message or null if none
   */
  public static String getCauseMessage( final Throwable throwable ) {
    return FluentIterable.from( Lists.reverse( causes( throwable ) ) )
      .transform( message( ) )
      .firstMatch( Predicates.notNull() )
      .orNull( );
  }

  /**
   * * Convert (possibly unwrapping) the argument {@link Throwable} into a suitable *
   * {@link RuntimeException} either by type casting or wrapping in an *
   * {@link UndeclaredThrowableException}. * * @param <T> * @param message * @param ex * @return
   */
  public static <T extends Throwable> RuntimeException toUndeclared( String message, T... exs ) {
    Throwable ex = null;
    if ( exs != null && exs.length > 0 ) {
      ex = exs[0];
    } else {
      ex = ( T ) new RuntimeException( message );
    }
    if ( ex instanceof RuntimeException ) {
      return ( RuntimeException ) ex;
    } else if ( ex instanceof ExecutionException ) {
      if ( ex.getCause( ) != null && RuntimeException.class.isAssignableFrom( ex.getCause( ).getClass( ) ) ) {
        return ( RuntimeException ) ex.getCause( );
      } else {
        return new RuntimeException( message, ex.getCause( ) );
      }
    } else {
      return new RuntimeException( message, ex );
    }
  }

  /** * {@inheritDoc #toUndeclared(String, Throwable)} * * @param cause * @return */
  public static RuntimeException toUndeclared( Throwable cause ) {
    return toUndeclared( cause.getMessage( ), cause );
  }

  public static Exception toException( String message, Throwable cause ) {
    if ( cause instanceof Exception ) {
      return (Exception) cause;
    }
    return toUndeclared( message, cause );
  }

  public static Exception toException( Throwable cause ) {
    return toException( cause.getMessage( ), cause );
  }

  public static <T extends Throwable> RuntimeException rethrow(
      @Nonnull final RuntimeException e,
      @Nonnull final Class<T> cause
  ) throws T {
    doRethrow( e, cause );
    throw e;
  }

  public static <T1 extends Throwable, T2 extends Throwable> RuntimeException rethrow(
      @Nonnull final RuntimeException e,
      @Nonnull final Class<T1> cause1,
      @Nonnull final Class<T2> cause2
      ) throws T1, T2 {
    doRethrow( e, cause1 );
    doRethrow( e, cause2 );
    throw e;
  }

  public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> RuntimeException rethrow(
      @Nonnull final RuntimeException e,
      @Nonnull final Class<T1> cause1,
      @Nonnull final Class<T2> cause2,
      @Nonnull final Class<T3> cause3
  ) throws T1, T2, T3 {
    doRethrow( e, cause1 );
    doRethrow( e, cause2 );
    doRethrow( e, cause3 );
    throw e;
  }

  private static <T extends Throwable> void doRethrow(
      @Nonnull final RuntimeException e,
      @Nonnull final Class<T> cause
  ) throws T {
    if ( e.getCause() != null && cause.isAssignableFrom( e.getCause( ).getClass( ) ) ) {
      throw cause.cast( e.getCause( ) );
    }
  }

  public static <T extends Throwable> void findAndRethrow(
      @Nonnull final Throwable e,
      @Nonnull final Class<T> cause
  ) throws T {
    doFindAndRethrow( e, cause );
  }

  public static <T1 extends Throwable, T2 extends Throwable> void findAndRethrow(
      @Nonnull final Throwable e,
      @Nonnull final Class<T1> cause1,
      @Nonnull final Class<T2> cause2
  ) throws T1, T2 {
    doFindAndRethrow( e, cause1 );
    doFindAndRethrow( e, cause2 );
  }

  public static <T1 extends Throwable, T2 extends Throwable, T3 extends Throwable> void findAndRethrow(
      @Nonnull final Throwable e,
      @Nonnull final Class<T1> cause1,
      @Nonnull final Class<T2> cause2,
      @Nonnull final Class<T3> cause3
  ) throws T1, T2, T3 {
    doFindAndRethrow( e, cause1 );
    doFindAndRethrow( e, cause2 );
    doFindAndRethrow( e, cause3 );
  }

  private static <T extends Throwable> void doFindAndRethrow(
      @Nonnull final Throwable e,
      @Nonnull final Class<T> causeClass
  ) throws T {
    T cause = findCause( e, causeClass );
    if ( cause != null ) {
      throw cause;
    }
  }

  public static <T extends Throwable> T filterStackTrace( T ex ) {
    ex.setStackTrace( Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MATCHES )
                                .toArray( steArrayType ) );
    return ex;
  }

  public static Collection<StackTraceElement> filterStackTraceElements( Throwable ex ) {
    return Exceptions.filterStackTraceElements( ex, DEFAULT_FILTER_MATCHES );
  }

  private static Collection<StackTraceElement> filterStackTraceElements( Throwable ex, List<String> patterns ) {
    Predicate<StackTraceElement> filter = stackTraceElementFilter( patterns );
    return Collections2.filter( Arrays.asList( ex.getStackTrace( ) ), filter );
  }

  public static RuntimeException trace( String message ) {
    return trace( new RuntimeException( message ) );
  }

  public static <T extends Throwable> T trace( T t ) {
    return trace( MoreObjects.firstNonNull( t.getMessage( ), t.toString( ) ), t );
  }

  public static <T extends Throwable> T trace( String message, T t ) {
    Throwable filtered = new RuntimeException( t.getMessage( ) );
    filtered.setStackTrace( Exceptions.filterStackTraceElements( t ).toArray( steArrayType ) );
    LOG.debug( message );
    LOG.trace( message, filtered );
    return t;
  }

  public static RuntimeException error( String message ) {
    return error( new RuntimeException( message ) );
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
    return findCause( ex, class1 ) != null;
  }

  /**
   * Unwrap generic exceptions to find the underlying cause. A new instance of Exception is returned
   * which contains a subset of the exception and its causes which excludes each of RuntimeException
   * and UndeclaredThrowableException while preserving any messages.
   */
  public static Throwable unwrapCause( Throwable ex ) {
    return Iterables.find( causes( ex ), FilterCauses.INSTANCE, ex );
  }

  @SuppressWarnings( "unchecked" )
  public static <T extends Throwable> T findCause( Throwable ex, final Class<T> class1 ) {
    try {
      return ( T ) Iterables.find( Exceptions.causes( ex ), Predicates.instanceOf( class1 ) );
    } catch ( NoSuchElementException ex1 ) {
      return null;
    }
  }

  @SuppressWarnings( "unchecked" )
  /*
   * This is not supper efficient method so use it carefully
   */
  public static <T extends Throwable> T findCauseByClassName( Throwable ex, final String className ) {
    MatchByClassName match = new MatchByClassName(className);
    try {
      return ( T ) Iterables.find( Exceptions.causes( ex ), match );
    } catch ( NoSuchElementException ex1 ) {
      return null;
    }
  }

  private static class MatchByClassName implements Predicate<Throwable> {
    String className = "";
    MatchByClassName(String className){
      this.className = className;
    }
    @Override
    public boolean apply(Throwable throwable) {
      return className.equals(throwable.getClass().getName());
    }
  }

  /** * @param message * @return */
  public static RuntimeException noSuchElement( String message, Throwable... t ) {
    if ( Logs.isExtrrreeeme( ) && t != null
         && t.length > 0 ) {
      return new NoSuchElementException( message + "\n"
                                         + string( message, t[0] ) );
    } else {
      return new NoSuchElementException( message );
    }
  }

  private static final LoadingCache<Class, ErrorMessageBuilder> builders = CacheBuilder.newBuilder().build(
    new CacheLoader<Class, ErrorMessageBuilder>( ) {
      @Override
      public ErrorMessageBuilder load( Class input ) {
        return new ErrorMessageBuilder( input );
      }
    });

  public static ErrorMessageBuilder builder( Class<?> type ) {
    return builders.getUnchecked( type );
  }

  public static class ErrorMessageBuilder {
    private Class              type;
    private LoadingCache<Class, String> map;

    public ErrorMessageBuilder( Class input ) {
      this.type = input;
      this.map = classErrorMessages.get( this.type );
    }

    private boolean hasMessage( Class<? extends Throwable> ex ) {
      if ( this.map != null ) {
        return this.map.getUnchecked( ex ) != null;
      } else {
        return false;
      }
    }

    private String getMessage( Class<? extends Throwable> ex ) {
      return classErrorMessages.get( this.type ).getUnchecked( ex );
    }

    public ExceptionBuilder exception( Throwable ex ) {
      return new ExceptionBuilder( ).exception( ex.getClass( ) );
    }

    public class ExceptionBuilder {
      private String                     extraMessage;
      private Object[]                   fArgs;
      private String                     message;
      private Class<? extends Throwable> ex;
      private String                     unknownMessage;
      private String                     fstring;

      public ExceptionBuilder exception( Class<? extends Throwable> ex ) {
        this.ex = ex;
        return this;
      }

      public ExceptionBuilder message( String message, Object[] formatArgs ) {
        this.message = message;
        this.fArgs = formatArgs;
        return this;
      }

      public ExceptionBuilder append( String appendedMessage ) {
        this.extraMessage = appendedMessage;
        return this;
      }

      /**
       * @param unknownExceptionMessage
       * @return
       */
      public ExceptionBuilder unknownException( String unknownExceptionMessage ) {
        this.unknownMessage = unknownExceptionMessage;
        return this;
      }

      public String build( ) {
        if ( ErrorMessageBuilder.this.hasMessage( this.ex ) ) {
          try {
            return String.format( this.fstring, this.fArgs ) + ": " + ErrorMessageBuilder.this.getMessage( this.ex );
          } catch ( IllegalFormatException ex1 ) {
            LOG.error( "Failed to format \"" + this.fstring + "\" with args: " + Arrays.asList( this.fArgs ) + " because of: " + ex1.getMessage( ), ex1 );
            return ErrorMessageBuilder.this.getMessage( this.ex );
          }
        } else {
          return this.unknownMessage;
        }
      }

      public ExceptionBuilder context( String format, Object... formatArgs ) {
        this.fstring = format;
        this.fArgs = formatArgs;
        return this;
      }
    }

  }

  public static Function<Throwable,String> message() {
    return ThrowableToMessageTransform.INSTANCE;
  }

  private enum ThrowableToMessageTransform implements Function<Throwable,String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply( @Nullable final Throwable throwable ) {
      return throwable == null ?
          null :
          throwable.getMessage( );
    }
  }

  @Target( { ElementType.TYPE,
      ElementType.FIELD } )
  @Retention( RetentionPolicy.RUNTIME )
  public @interface ErrorMessages {
    Class<?> value( );
  }

  private static final Map<Class, LoadingCache<Class, String>> classErrorMessages = Maps.newConcurrentMap( );

  @Discovery( value = { Function.class },
              annotations = { ErrorMessages.class },
              priority = -0.1d )
  public enum ErrorMessageDiscovery implements Predicate<Class> {
    INSTANCE;

    @SuppressWarnings( { "unchecked",
        "rawtypes" } )
    @Override
    public boolean apply( Class input ) {
      if ( Function.class.isAssignableFrom( input ) && Ats.from( input ).has( ErrorMessages.class ) ) {
        try {
          ErrorMessages annote = Ats.from( input ).get( ErrorMessages.class );
          Function<Class, String> errorFunction = ( Function<Class, String> ) Classes.builder( input ).newInstance( );
          LoadingCache<Class, String> errorMap =  CacheBuilder.newBuilder().expireAfterAccess( 60, TimeUnit.SECONDS ).build(CacheLoader.from(errorFunction));
          classErrorMessages.put( annote.value( ), errorMap );
          return true;
        } catch ( UndeclaredThrowableException ex ) {
          LOG.error( ex, ex );
          return false;
        }
      } else {
        Discovery discovery = Ats.from( ErrorMessageDiscovery.class ).get( Discovery.class );
        LOG.error( "Annotated Discovery supplied class argument that does not conform to one of: value()="
                   + discovery.value( )
                   + " (assignable types) or annotations()="
                   + discovery.annotations( ) );
        return false;
      }
    }

  }

}
